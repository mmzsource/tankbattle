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

(defn neighbour [position orientation]
  (cond
    (= orientation :north) (north-of position)
    (= orientation :east)  (east-of  position)
    (= orientation :south) (south-of position)
    (= orientation :west)  (west-of  position)))

(defn map-positions
  "All gameobjects (maps) have a :position key. Given a collection of
  gameobjects, returns a map from position [col row] to a collection of
  gameobjects on that position"
  [gameobjects]
  (reduce
   (fn [position-map {:keys [position] :as gameobject}]
     (update position-map position (fnil conj []) gameobject))
   {}
   gameobjects))

(defn unmap-positions
  "undo the map-positions operation
   returns a vector of gameobjects"
  [positions-map]
  (reduce
   (fn [acc [_ [obj]]]
     (conj acc obj))
   []
   positions-map))

(defn filter-north-of [[source-col source-row] position-set]
  (filter
   (fn [[target-col target-row]]
     (and (= source-col target-col)
          (< target-row source-row)))
   position-set))

(defn filter-east-of [[source-col source-row] position-set]
  (filter
   (fn [[target-col target-row]]
     (and (= source-row target-row)
          (> target-col source-col)))
   position-set))

(defn filter-south-of [[source-col source-row] position-set]
  (filter
   (fn [[target-col target-row]]
     (and (= source-col target-col)
          (> target-row source-row)))
   position-set))

(defn filter-west-of [[source-col source-row] position-set]
  (filter
   (fn [[target-col target-row]]
     (and (= source-row target-row)
          (< target-col source-col)))
   position-set))

(defn nearest-north-pos [positions]
  (first (sort-by second > positions)))

(defn nearest-east-pos [positions]
  (first (sort-by first < positions)))

(defn nearest-south-pos [positions]
  (first (sort-by second < positions)))

(defn nearest-west-pos [positions]
  (first (sort-by first > positions)))

(defn nearest-pos-given-orient
  [position position-set orientation]
  (cond
    (= orientation :north) (nearest-north-pos (filter-north-of position position-set))
    (= orientation :east)  (nearest-east-pos  (filter-east-of  position position-set))
    (= orientation :south) (nearest-south-pos (filter-south-of position position-set))
    (= orientation :west)  (nearest-west-pos  (filter-west-of  position position-set))))

;;;;;;;;;;;;
;; BORDER ;;
;;;;;;;;;;;;


(defn north-wall-positions [cols _]
  (set (for [x (range cols)] [x 0])))

(defn east-wall-positions [cols rows]
  (set (for [y (range rows)] [(dec cols) y])))

(defn south-wall-positions [cols rows]
  (set (for [x (range cols)] [x (dec rows)])))

(defn west-wall-positions [_ rows]
  (set (for [y (range rows)] [0 y])))

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


(defn find-tank [world tankid]
  (let [tanks (world :tanks)]
    (first (filter #(= (% :id) tankid) tanks))))

(defn valid-tankid? [world tankid]
  (not (nil? (find-tank world tankid))))

(defn valid-tank-cmd? [cmd]
  (contains? #{"north" "east" "south" "west" "fire"} cmd))

(defn create-tank [id position color name]
  {:id          id
   :name        name
   :position    position
   :orientation (first (shuffle orientations))
   :energy      3
   :color       color
   :last-shot   1234567890
   :reloaded    1234572890
   :last-move   1234567891
   :restarted   1234569891
   :hits        []
   :kills       []})

(defn subscribe-tank
  [world tank-name]
  ;; only subscribe when there is still room left for tanks
  (if (< (count (world :tanks)) 4)
    (let [available-ids       (world :av-ids)
          id                  (first available-ids)
          remaining-ids       (set (rest available-ids))
          available-pos       (world :av-pos)
          position            (first available-pos)
          remaining-pos       (set (rest available-pos))
          available-colors    (world :av-cls)
          color               (first available-colors)
          remaining-colors    (set (rest available-colors))
          new-tank            (create-tank id position color tank-name)]
      (-> world
          (assoc     :last-update (System/currentTimeMillis))
          (assoc     :av-ids      remaining-ids)
          (assoc     :av-pos      remaining-pos)
          (assoc     :av-cls      remaining-colors)
          (update-in [:tanks]     conj new-tank)))
    world))

(defn move [world tankid direction]
  (let [tanks-map (map-positions (world :tanks))
        trees-map (map-positions (world :trees))
        walls-map (map-positions (world :walls))
        tank      (find-tank world tankid)
        tank-pos  (tank :position)
        restarted (< (tank :restarted) (System/currentTimeMillis))
        new-pos   (cond
                    (= direction "north") (north-of tank-pos)
                    (= direction "east")  (east-of  tank-pos)
                    (= direction "south") (south-of tank-pos)
                    (= direction "west")  (west-of  tank-pos))
        occupied  (-> #{}
                      (into (keys tanks-map))
                      (into (keys trees-map))
                      (into (keys walls-map)))]

    (if (or (contains? occupied new-pos) ;; if the new position is already occupied
            (not restarted))             ;; .. or the tank is not yet restarted

      ;; return world
      world

      ;; else:
      (let [;; update the tank
            tank      (let [now (System/currentTimeMillis)]
                        (-> tank
                            (assoc :position    new-pos)
                            (assoc :orientation (keyword direction))
                            (assoc :last-move   now)
                            (assoc :restarted   (+ now 2000))))

            ;; update the tanks-map
            new-tanks (assoc tanks-map tank-pos [tank])

            ;; add the new tanks into a new world state
            new-world (assoc world :tanks (unmap-positions new-tanks))]
        new-world))))

(defn fire [world tankid]
  (let [now         (System/currentTimeMillis)
        tanks-map   (map-positions (world :tanks))
        trees-map   (map-positions (world :trees))
        walls-map   (map-positions (world :walls))
        tank        (find-tank world tankid)
        tank-pos    (tank :position)
        orientation (tank :orientation)
        reloaded    (< (tank :reloaded) now)
        safe        (< (+ (tank :last-move) 2000) now)]

    (if (and reloaded safe)
      (let [nrst-tree (nearest-pos-given-orient tank-pos (set (keys trees-map)) orientation)
            nrst-tank (nearest-pos-given-orient tank-pos (set (keys tanks-map)) orientation)
            nrst-wall (nearest-pos-given-orient tank-pos (set (keys walls-map)) orientation)
            nrst-pos  (nearest-pos-given-orient tank-pos (set [nrst-tree nrst-tank nrst-wall]) orientation)
            object-to-hit  (cond
                             (= nrst-pos nrst-tree) :tree
                             (= nrst-pos nrst-tank) :tank
                             :else                  :wall)
            updated-lasers (conj
                            (world :lasers)
                            {:start-position (neighbour tank-pos orientation)
                             :end-position   nrst-pos
                             :direction      orientation
                             :start-time     now
                             :end-time       (+ now 2000)})]

        (cond
          (= object-to-hit :tank)
          ;; handle-tank-hit
          (let [;; hit-tank administration
                hit-tank         (first (tanks-map nrst-tank))
                hit-tankid       (hit-tank :id)
                hit-tank-pos     nrst-tank
                hit-tank-energy  (dec (hit-tank :energy))
                updated-hit-tank (assoc hit-tank :energy hit-tank-energy)
                destroyed?       (< hit-tank-energy 1)

                ;; source-tank administration
                src-tank-hits   (conj (tank :hits) hit-tankid)
                src-tank-kills  (if destroyed?
                                  (conj (tank :kills) hit-tankid)
                                  (tank :kills))
                updated-src-tank (-> tank
                                     (assoc :hits      src-tank-hits)
                                     (assoc :kills     src-tank-kills)
                                     (assoc :last-shot now)
                                     (assoc :reloaded  (+ now 5000))
                                     (assoc :last-move now)
                                     (assoc :restarted (+ now 2000)))

                ;; tank-map administration
                updated-tank-map (if destroyed?
                                   (-> tanks-map
                                       (dissoc hit-tank-pos)
                                       (assoc  tank-pos [updated-src-tank]))
                                   (-> tanks-map
                                       (assoc hit-tank-pos [updated-hit-tank])
                                       (assoc tank-pos     [updated-src-tank])))
                updated-tanks      (unmap-positions updated-tank-map)

                ;; explosion administration
                updated-explosions (if destroyed?
                                     (conj
                                      (world :explosions)
                                      {:position   hit-tank-pos
                                       :start-time now
                                       :end-time   (+ now 4000)})
                                     (world :explosions))]

            (-> world
                (assoc :tanks       updated-tanks)
                (assoc :explosions  updated-explosions)
                (assoc :lasers      updated-lasers)
                (assoc :last-update now)))

          (= object-to-hit :tree)
          ;; handle-tree-hit
          (let [;; tree administration
                tree         (first (trees-map nrst-tree))
                tree-pos     nrst-tree
                tree-energy  (dec (tree :energy))
                updated-tree (assoc tree :energy tree-energy)
                destroyed?   (< tree-energy 1)

                ;; tank administration
                updated-tank (-> tank
                                 (assoc :last-shot now)
                                 (assoc :reloaded  (+ now 5000))
                                 (assoc :last-move now)
                                 (assoc :restarted (+ now 2000)))

                ;; tank-map administration
                updated-tank-map (assoc tanks-map tank-pos [updated-tank])
                updated-tanks    (unmap-positions updated-tank-map)

                ;; tree-map administration
                updated-tree-map (if destroyed?
                                   (dissoc trees-map tree-pos)
                                   (assoc  trees-map tree-pos [updated-tree]))
                updated-trees    (unmap-positions updated-tree-map)

                ;; explosion administration
                updated-explosions (if destroyed?
                                     (conj
                                      (world :explosions)
                                      {:position   tree-pos
                                       :start-time now
                                       :end-time   (+ now 4000)})
                                     (world :explosions))]

            (-> world
                (assoc :tanks       updated-tanks)
                (assoc :trees       updated-trees)
                (assoc :explosions  updated-explosions)
                (assoc :lasers      updated-lasers)
                (assoc :last-update now)))

          (= object-to-hit :wall)
          ;; handle-wall-hit
          (let [updated-tank     (-> tank
                                     (assoc :last-shot now)
                                     (assoc :reloaded  (+ now 5000))
                                     (assoc :last-move now)
                                     (assoc :restarted (+ now 2000)))
                updated-tank-map (assoc tanks-map tank-pos [updated-tank])
                updated-tanks    (unmap-positions updated-tank-map)]

            (-> world
                (assoc :tanks       updated-tanks)
                (assoc :lasers      updated-lasers)
                (assoc :last-update now)))
          :else world))
      world)))

(defn update-tank [world tankid cmd]
  (cond
    (contains? #{"north" "east" "south" "west"} cmd) (move world tankid cmd)
    (= cmd "fire")                                   (fire world tankid)
    :else                                            world))


;;;;;;;;;;;
;; WORLD ;;
;;;;;;;;;;;


(defn started? [world]
  (contains? world :game-end))

(defn players-subscribed?
  "more than zero players subscribed?"
  [world]
  (pos? (count (world :tanks))))

(defn start-game [world]
  (if (and (not (started? world))
           (players-subscribed? world))
    (let [currentTimeMillis     (System/currentTimeMillis)
          gameDurationInMinutes 5]
      (-> world
          (assoc :last-updated currentTimeMillis)
          (assoc :game-start   currentTimeMillis)
          (assoc :game-end     (+ currentTimeMillis (* gameDurationInMinutes 60 1000)))))
    world))

(defn filter-on-time [gameobjects now]
  (filterv (fn [obj] (< now (obj :end-time))) gameobjects))

(defn cleanup [world]
  (let [explosions   (world :explosions)
        lasers       (world :lasers)
        now          (System/currentTimeMillis)
        updated-e    (filter-on-time explosions now)
        updated-l    (filter-on-time lasers now)]
    (-> world
        (assoc :explosions  updated-e)
        (assoc :lasers      updated-l)
        (assoc :last-update now))))

(defn tree [pos]
  {:position pos :energy 1})

(defn initial-trees [c r]
  (let [center-c (int (/ c 2))
        center-r (int (/ r 2))
        north-south (range (- center-c 2) (+ center-c 2))
        east-west   (range (- center-r 2) (+ center-r 2))
        ns3         (map (fn [c] [c 3])       north-south)
        ns4         (map (fn [c] [c (- r 4)]) north-south)
        ew3         (map (fn [r] [3 r])       east-west)
        ew4         (map (fn [r] [(- c 4) r]) east-west)]
    (mapv tree (concat ns3 ns4 ew3 ew4))))

(defn initial-av-pos [c r]
  (let [center-c (int (/ c 2))
        center-r (int (/ r 2))]
    [[center-c 1]
     [(- c 2)  center-r]
     [center-c (- r 2) ]
     [1        center-r]]))

(defn init-world []
  (let [cols 12 rows 12 moment-created (System/currentTimeMillis)] ; hardcoded!
    {:moment-created moment-created
     :last-update    moment-created
     :dimensions     {:width cols :height rows}
     :av-ids         #{1 2 3 4}                                    ; hardcoded! assumption: 4 tanks per game
     :av-pos         (set (shuffle (initial-av-pos cols rows)))
     :av-cls         (set (shuffle #{:red :green :yellow :blue}))  ; hardcoded! assumption: 4 tanks per game
     :tanks          []
     :trees          (initial-trees cols rows)
     :walls          (create-walls cols rows)
     :lasers         []
     :explosions     []}))
