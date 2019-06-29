(ns tankbattle.core
  (:require [clojure.set :as s]))

;; Validation of ids, commands, moves, directions etc will be done once on the
;; server boundary (as opposed to validating the values in each and every
;; function)


(def orientations #{:north :east :south :west})

;;;;;;;;;;;;;;;
;; POSITIONS ;;
;;;;;;;;;;;;;;;


(defn generate-random-position
  "generates one random position on the board within given bounds"
  ([cols rows]
   (generate-random-position 0 cols 0 rows))
  ([mincol maxcol minrow maxrow]
   [(rand-nth (range mincol (inc maxcol)))
    (rand-nth (range minrow (inc maxrow)))]))

(defn north-of [[x y]] [x (dec y)])
(defn south-of [[x y]] [x (inc y)])
(defn east-of  [[x y]] [(inc x) y])
(defn west-of  [[x y]] [(dec x) y])

(defn map-positions
  "All gameobjects (maps) have a :position key. Given a collection of
  gameobjects, returns a map from position [col row] to a collection of
  gameobjects on that position"
  [gameobjects]
  (reduce
   (fn [position-map {:keys [position] :as gameobject}]
     (update-in position-map [position] (fnil conj []) gameobject))
   {}
   gameobjects))

(defn dead-object-positions [object-position-map]
  (into #{} (keys (filter (fn [[pos [obj]]] (< (obj :energy) 1)) object-position-map))))

(defn remove-objects [object-position-map positions-of-objects-to-remove]
  (reduce
   (fn [acc [pos [& objects]]]
     (if (not (contains? positions-of-objects-to-remove pos))
       (into acc objects)
       acc))
   []
   object-position-map))


;;;;;;;;;;;;
;; BORDER ;;
;;;;;;;;;;;;


(defn north-wall-positions [cols rows]
  (into #{} (for [x (range cols)] [x 0])))

(defn east-wall-positions [cols rows]
  (into #{} (for [y (range rows)] [(dec cols) y])))

(defn south-wall-positions [cols rows]
  (into #{} (for [x (range cols)] [x (dec rows)])))

(defn west-wall-positions [cols rows]
  (into #{} (for [y (range rows)] [0 y])))

(defn wall-positions [cols rows]
  (s/union
   (north-wall-positions cols rows)
   (east-wall-positions  cols rows)
   (south-wall-positions cols rows)
   (west-wall-positions  cols rows)))

(defn create-walls [cols rows]
  (let [wps (wall-positions cols rows)]
    (mapv (fn [wall-position] {:position wall-position}) wps)))


;;;;;;;;;;;
;; TANKS ;;
;;;;;;;;;;;


(def commands #{:move-north :move-east :move-south :move-west :fire})

(defn create-tank [id position color name]
  {:id          id
   :name        name
   :position    position
   :orientation (first (shuffle orientations))
   :energy      10
   :color       color
   :last-shot   1234567890
   :reloaded    1234572890
   :last-move   1234567891
   :restarted   1234569891})

(defn subscribe-tank
  [world tank-name]
  ;; only subscribe when there is still room left for tanks
  (if (< (count (world :tanks)) 4)
    (let [available-ids       (world :av-ids)
          id                  (first available-ids)
          remaining-ids       (into #{} (rest available-ids))
          available-pos       (world :av-pos)
          position            (first available-pos)
          remaining-pos       (into #{} (rest available-pos))
          available-colors    (world :av-cls)
          color               (first available-colors)
          remaining-colors    (into #{} (rest available-colors))
          new-tank            (create-tank id position color tank-name)]
      (-> world
          (assoc     :last-update (System/currentTimeMillis))
          (assoc     :av-ids      remaining-ids)
          (assoc     :av-pos      remaining-pos)
          (assoc     :av-cls      remaining-colors)
          (update-in [:tanks]     conj new-tank)))
    world))

(defn drive [tank]
  (merge tank {:moving true}))

(defn stop [tank]
  (merge tank {:moving false}))

(defn turn [tank orientation]
  (merge tank {:orientation orientation}))

(defn fire [tank]
  (if-not (tank :moving)
    (merge tank {:firing true})
    tank))

(defn hold-fire [tank]
  (merge tank {:firing false}))

(defn shot-bullet [tank]
  (if (> (:bullets tank) 0)
    (update tank :bullets dec)
    tank))

(defn update-tank [tank cmd]
  (cond
    (= cmd :drive)      (drive     tank)
    (= cmd :stop)       (stop      tank)
    (= cmd :turn-north) (turn      tank :north)
    (= cmd :turn-east)  (turn      tank :east)
    (= cmd :turn-south) (turn      tank :south)
    (= cmd :turn-west)  (turn      tank :west)
    (= cmd :fire)       (fire      tank)
    (= cmd :hold-fire)  (hold-fire tank)
    :else               tank))

(defn execute-cmds [tank cmds]
  (reduce update-tank tank cmds))

(defn apply-tank-events [tank-events {:keys [tanks] :as world}]
  world)

(defn detect-winner [world]
  world)


;;;;;;;;;;;;;
;; BULLETS ;;
;;;;;;;;;;;;;


(defn move-bullet [{:keys [position direction] :as bullet}]
  (cond
    (= direction :north) (merge bullet {:position (north-of position)})
    (= direction :east)  (merge bullet {:position (east-of  position)})
    (= direction :south) (merge bullet {:position (south-of position)})
    (= direction :west)  (merge bullet {:position (west-of  position)})
    :else bullet))

(defn update-energy-when-hit [gameobject nr-of-hits]
  (reduce
   (fn [acc _] (update-in acc [:energy] dec))
   gameobject
   (range nr-of-hits)))

(defn update-hit-objects [object-position-map bullet-position-map]
  (reduce
   (fn [acc [pos [obj]]]
     (conj
      acc
      (update-energy-when-hit obj (count (bullet-position-map pos)))))
   []
   object-position-map))

(defn update-bullet-positions [{:keys [bullets] :as world}]
  (assoc world :bullets (mapv move-bullet bullets)))

(defn update-object-hits [{:keys [bullets tanks trees walls] :as world}]
  (let [bullets-map      (map-positions bullets)
        bullet-positions (into #{} (keys bullets-map))

        tanks-map        (map-positions tanks)
        tank-positions   (into #{} (keys tanks-map))
        tank-hits        (s/intersection bullet-positions tank-positions)

        trees-map        (map-positions trees)
        trees-positions  (into #{} (keys trees-map))
        trees-hits       (s/intersection bullet-positions trees-positions)

        walls-map        (map-positions walls)
        walls-positions  (into #{} (keys walls-map))
        walls-hits       (s/intersection bullet-positions walls-positions)

        ;; Decrease energy of hit tanks
        updated-tanks (update-hit-objects tanks-map bullets-map)

        ;; Decrease energy of hit trees
        updated-trees (update-hit-objects trees-map bullets-map)

        ;; Remove bullets used on tanks, trees and walls
        used-bullet-positions (s/union tank-hits trees-hits walls-hits)
        updated-bullets       (remove-objects bullets-map used-bullet-positions)]

    ;; now update the world with the newly calculated values
    (-> world
        (assoc :tanks   updated-tanks)
        (assoc :trees   updated-trees)
        (assoc :bullets updated-bullets))))


;;;;;;;;;;;;;;;;
;; EXPLOSIONS ;;
;;;;;;;;;;;;;;;;


(defn update-explosion-energy [explosion-position-map]
  (into {} (map (fn [[pos [obj]]] {pos [(update-in obj [:energy] dec)]}) explosion-position-map)))

(defn cleanup-explosions [explosion-position-map]
  (filter (fn [[pos [obj]]] (> (obj :energy) 0)) explosion-position-map))

(defn create-new-explosions [explosion-positions]
  (mapv (fn [pos] {:position pos :energy 7}) explosion-positions))

(defn update-explosions [{:keys [tanks trees explosions] :as world}]
  (let [tank-map             (map-positions tanks)
        dead-tank-positions  (dead-object-positions tank-map)
        updated-tanks        (remove-objects tank-map dead-tank-positions)

        tree-map             (map-positions trees)
        dead-tree-positions  (dead-object-positions tree-map)
        updated-trees        (remove-objects tree-map dead-tree-positions)

        explosion-map        (map-positions explosions)
        decreased-explosions (update-explosion-energy explosion-map)
        dead-explosion-pos   (dead-object-positions decreased-explosions)
        cleanedup-explosions (remove-objects decreased-explosions dead-explosion-pos)
        new-explosion-pos    (s/union dead-tank-positions dead-tree-positions)
        updated-explosions   (into cleanedup-explosions (create-new-explosions new-explosion-pos))]

    (-> world
        (assoc :tanks      updated-tanks)
        (assoc :trees      updated-trees)
        (assoc :explosions updated-explosions))))


;;;;;;;;;;;
;; WORLD ;;
;;;;;;;;;;;


(defn update-world [tank-events {:keys [tanks bullets trees walls] :as world}]
  (->> world
       (update-bullet-positions)
       (update-object-hits)
       (update-explosions)
       (detect-winner)
       (apply-tank-events tank-events)))

;; Hardcoded! Based on [32 18] grid!
(def trees [{:position [15  3] :energy 3}
            {:position [18  3] :energy 3}
            {:position [15  4] :energy 3}
            {:position [15  5] :energy 3}
            {:position [15  6] :energy 3}
            {:position [15  7] :energy 3}

            {:position [ 3  7] :energy 3}
            {:position [ 4  7] :energy 3}
            {:position [ 4  8] :energy 3}
            {:position [ 4  9] :energy 3}
            {:position [ 3 10] :energy 3}
            {:position [ 4 10] :energy 3}

            {:position [27  7] :energy 3}
            {:position [28  7] :energy 3}
            {:position [27  8] :energy 3}
            {:position [27  9] :energy 3}
            {:position [27 10] :energy 3}
            {:position [28 10] :energy 3}

            {:position [15 13] :energy 3}
            {:position [16 13] :energy 3}
            {:position [17 13] :energy 3}
            {:position [18 13] :energy 3}
            {:position [15 14] :energy 3}
            {:position [18 14] :energy 3}])

(defn init-world []
  (let [cols 32 rows 18]                                              ; hardcoded!
    {:last-update (System/currentTimeMillis)
     :dimensions  {:width cols :height rows}
     :av-ids      #{1 2 3 4}                                          ; hardcoded! assumption: 4 tanks per game
     :av-pos      (into #{} (shuffle #{[1 8] [17 1] [30 9] [16 16]})) ; hardcoded! based on [32 18] grid!
     :av-cls      (into #{} (shuffle #{:red :green :yellow :blue}))   ; hardcoded! assumption: 4 tanks per game
     :tanks       []
     :trees       trees
     :walls       (create-walls cols rows)
     :lasers      []
     :explosions  []}))

(defn -main
  "It all starts here"
  [& args]
  (init-world))

(comment
;; Sequence is in line with the order in which the events came in (fair play)
(def tank-cmd-seq [[1 :turn-east] [1 :stop] [1 :fire] [2 :drive]] )

)
