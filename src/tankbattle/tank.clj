(ns tankbattle.tank
  (:require [tankbattle.position :as pos]))

(def orientations #{:north :east :south :west})

(defn- explosion
  [position time]
  {:position   position
   :start-time time
   :end-time   (+ time 4000)
   :uuid       (java.util.UUID/randomUUID)})

(defn find-tank [world tankid]
  (let [tanks (world :tanks)]
    (first (filter #(= (% :id) tankid) tanks))))

(defn valid-tankid? [world tankid]
  (not (nil? (find-tank world tankid))))

(defn valid-tank-cmd? [cmd]
  (contains? #{"north" "east" "south" "west" "fire"} cmd))

(defn create-tank [id position color name]
  {:id          id
   :uuid        (java.util.UUID/randomUUID)
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
  (if (pos? (count (world :available)))
    (let [available-options (world :available)
          option            (first available-options)
          remaining-options (vec (rest available-options))
          id                (option :id)
          position          (option :position)
          color             (option :color)
          new-tank          (create-tank id position color tank-name)]
      (-> world
          (assoc     :last-update (System/currentTimeMillis))
          (assoc     :available   remaining-options)
          (update-in [:playing]   conj option)
          (update-in [:tanks]     conj new-tank)))
    world))

(defn move [world tankid direction]
  (let [tanks-map (pos/map-positions (world :tanks))
        trees-map (pos/map-positions (world :trees))
        walls-map (pos/map-positions (world :walls))
        tank      (find-tank world tankid)
        tank-pos  (tank :position)
        restarted (< (tank :restarted) (System/currentTimeMillis))
        new-pos   (cond
                    (= direction "north") (pos/north-of tank-pos)
                    (= direction "east")  (pos/east-of  tank-pos)
                    (= direction "south") (pos/south-of tank-pos)
                    (= direction "west")  (pos/west-of  tank-pos))
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
            new-world (assoc world :tanks (pos/unmap-positions new-tanks))]
        new-world))))

(defn fire [world tankid]
  (let [now         (System/currentTimeMillis)
        tanks-map   (pos/map-positions (world :tanks))
        trees-map   (pos/map-positions (world :trees))
        walls-map   (pos/map-positions (world :walls))
        tank        (find-tank world tankid)
        tank-pos    (tank :position)
        orientation (tank :orientation)
        reloaded    (< (tank :reloaded) now)
        safe        (< (+ (tank :last-move) 2000) now)]

    (if (and reloaded safe)
      (let [nrst-tree (pos/nearest-pos-given-orient tank-pos (set (keys trees-map)) orientation)
            nrst-tank (pos/nearest-pos-given-orient tank-pos (set (keys tanks-map)) orientation)
            nrst-wall (pos/nearest-pos-given-orient tank-pos (set (keys walls-map)) orientation)
            nrst-pos  (pos/nearest-pos-given-orient tank-pos (set [nrst-tree nrst-tank nrst-wall]) orientation)
            object-to-hit  (cond
                             (= nrst-pos nrst-tree) :tree
                             (= nrst-pos nrst-tank) :tank
                             :else                  :wall)
            updated-lasers (conj
                            (world :lasers)
                            {:start-position (pos/neighbour tank-pos orientation)
                             :end-position   nrst-pos
                             :direction      orientation
                             :start-time     now
                             :end-time       (+ now 2000)
                             :uuid           (java.util.UUID/randomUUID)})]

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
                updated-tanks      (pos/unmap-positions updated-tank-map)

                ;; explosion administration
                updated-explosions (if destroyed?
                                     (conj
                                      (world :explosions)
                                      (explosion hit-tank-pos now))
                                     (world :explosions))

                ;; subscription administration
                destroyed-tank     (first (filter (fn [{:keys [id]}] (= id hit-tankid)) (world :playing)))
                updated-playing    (if destroyed?
                                     (vec (remove (fn [{:keys [id]}] (= id hit-tankid)) (world :playing)))
                                     (world :playing))
                updated-available  (if destroyed?
                                     (conj (world :available) destroyed-tank)
                                     (world :available))]

            (-> world
                (assoc :tanks       updated-tanks)
                (assoc :explosions  updated-explosions)
                (assoc :lasers      updated-lasers)
                (assoc :available   updated-available)
                (assoc :playing     updated-playing)
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
                updated-tanks    (pos/unmap-positions updated-tank-map)

                ;; tree-map administration
                updated-tree-map (if destroyed?
                                   (dissoc trees-map tree-pos)
                                   (assoc  trees-map tree-pos [updated-tree]))
                updated-trees    (pos/unmap-positions updated-tree-map)

                ;; explosion administration
                updated-explosions (if destroyed?
                                     (conj
                                      (world :explosions)
                                      (explosion tree-pos now))
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
                updated-tanks    (pos/unmap-positions updated-tank-map)]

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
