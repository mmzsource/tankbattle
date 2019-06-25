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
    (mapv (fn [wall-position] {:position wall-position :energy -1}) wps)))

;;;;;;;;;;;
;; TANKS ;;
;;;;;;;;;;;


(def commands #{:drive :stop :turn-north :turn-east :turn-south :turn-west :fire :hold-fire})

(defn create-tank [id position color]
  {:id          id
   :position    position
   :orientation (first (shuffle orientations))
   :energy      10
   :color       color
   :moving      false
   :firing      false
   :bullets     100})

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

(defn hit-by-bullet [object]
  (update object :energy dec))

(defn destroyed? [object]
  (= (:energy object) 0))


;;;;;;;;;;;
;; WORLD ;;
;;;;;;;;;;;


(def tanks [{:id          1
             :position    [2 2]
             :orientation :south
             :energy      5
             :color       :blue
             :moving      true
             :firing      false
             :bullets     256}
            {:id          2
             :position    [3 4]
             :orientation :east
             :energy      2
             :color       :red
             :moving      false
             :firing      true
             :bullets     311}])

(def trees [{:position [3 3] :energy 3}])

(def walls [{:position [0 0] :energy -1}
            {:position [1 0] :energy -1}
            {:position [2 0] :energy -1}
            {:position [3 0] :energy -1}
            {:position [4 0] :energy -1}
            {:position [5 0] :energy -1}
            {:position [6 0] :energy -1}
            {:position [0 1] :energy -1}
            {:position [6 1] :energy -1}
            {:position [0 2] :energy -1}
            {:position [6 2] :energy -1}
            {:position [0 3] :energy -1}
            {:position [6 3] :energy -1}
            {:position [0 4] :energy -1}
            {:position [6 4] :energy -1}
            {:position [0 5] :energy -1}
            {:position [6 5] :energy -1}
            {:position [0 6] :energy -1}
            {:position [1 6] :energy -1}
            {:position [2 6] :energy -1}
            {:position [3 6] :energy -1}
            {:position [4 6] :energy -1}
            {:position [5 6] :energy -1}
            {:position [6 6] :energy -1}])

(def bullets [{:position [4 4] :energy 1 :direction :east :tankid 1}])

(def explosions [{:position [1 2] :energy 7}
                 {:position [5 5] :energy 4}])

(def world
  {:dimensions {:width 7 :height 7}
   :tanks      tanks
   :trees      trees
   :walls      walls
   :bullets    bullets
   :explosions explosions})


;; TODO: position tanks and trees randomly based on grid size
(defn init-world [cols rows]
  {:dimensions {:width cols :height rows}
   :tanks      tanks
   :trees      trees
   :walls      (walls cols rows)
   :bullets    bullets
   :explosions explosions})

(defn -main
  "It all starts here"
  [& args]
  (init-world 10 10))

(defn update-bullet-positions [{:keys [bullets] :as world}]
  (assoc world :bullets (mapv move-bullet bullets)))

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

(defn remove-used-bullets [bullets-map used-bullet-positions]
  (reduce
   (fn [acc [pos [& bullets]]]
     (if (not (contains? used-bullet-positions pos))
       (into acc bullets)
       acc))
   []
   bullets-map))

(defn update-tank-hits [{:keys [bullets tanks] :as world}]
  (let [bullets-map (map-positions bullets)
        tanks-map   (map-positions tanks)

        ;; Decrease energy of hit tanks
        updated-tanks (update-hit-objects tanks-map bullets-map)

        ;; Remove used bullets from world
        used-bullet-positions (into #{} (s/union (keys bullets-map) (keys tanks-map)))
        updated-bullets       (remove-used-bullets bullets-map used-bullet-positions)]

    ;; now update the world with the newly calculated values
    (-> world
        (assoc :tanks   updated-tanks)
        (assoc :bullets updated-bullets))))


(defn update-tree-hits [{:keys [bullets trees] :as world}]
  (let [bullets-map (map-positions bullets)
        trees-map   (map-positions trees)

        ;; Decrease energy of hit trees
        updated-trees (update-hit-objects trees-map bullets-map)

        ;; Remove used bullets from world
        used-bullets    (into #{} (s/union (keys bullets-map) (keys trees-map)))
        updated-bullets (remove-used-bullets bullets-map used-bullets)]

    (-> world
        (assoc :trees   updated-trees)
        (assoc :bullets updated-bullets))))

(defn update-wall-hits [{:keys [bullets walls] :as world}]
  (let [bullets-map (map-positions bullets)
        walls-map   (map-positions walls)

        ;; Decrease energy of hit walls (not really necessary, but I want the duplication to be clear TODO:
        updated-walls (update-hit-objects walls-map bullets-map)

        ;; Remove used bullets from world
        used-bullets    (into #{} (s/union (keys bullets-map) (keys walls-map)))
        updated-bullets (remove-used-bullets bullets-map used-bullets)]

    (-> world
        (assoc :walls   updated-walls) ;; also not needed, just to make duplication clear TODO:
        (assoc :bullets updated-bullets))))

(defn update-explosions [{:keys [bullets tanks trees walls explosions] :as world}]
  ;; replace tanks and trees with energy <= 0 with explosions
  ;; update explosion energies
  world)

(defn detect-winner [world]
  world)

(defn apply-tank-events [tank-events {:keys [tanks] :as world}]
  world)

(defn update-world [tank-events {:keys [tanks bullets trees walls] :as world}]
  (->> world
       (update-bullet-positions)
       (update-tank-hits)
       (update-tree-hits)
       (update-wall-hits)
       (update-explosions)
       (detect-winner)
       (apply-tank-events tank-events)))

(comment

;; Sequence is in line with the order in which the events came in (fair play)
(def tank-cmd-seq [[1 :turn-east] [1 :stop] [1 :fire] [2 :drive]] )

)
