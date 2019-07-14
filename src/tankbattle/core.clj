(ns tankbattle.core
  (:require [tankbattle.walls :as wall]
            [tankbattle.board :as board]
            [tankbattle.tank :as tank]))

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
  {:position pos :energy 1 :uuid (java.util.UUID/randomUUID)})

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
  (let [cols 12 rows 12 moment-created (System/currentTimeMillis)]
    {:moment-created moment-created
     :last-update    moment-created
     :dimensions     {:width cols :height rows}
     :av-ids         #{1 2 3 4}                                    ; 4 tanks per game
     :av-pos         (set (shuffle (initial-av-pos cols rows)))    ; 4 tanks per game
     :av-cls         (set (shuffle #{:red :green :yellow :blue}))  ; 4 tanks per game
     :tanks          []
     :trees          (initial-trees cols rows)
     :walls          (wall/create-walls cols rows)
     :lasers         []
     :explosions     []}))

(defn subscribe-tank [world tank-name]
  (tank/subscribe-tank world tank-name))

(defn validate [world]
  (board/validate world))

(defn create [world]
  (let [created    (System/currentTimeMillis)
        dimensions (board/get-dimensions world)
        walls      (board/get-walls world)
        trees      (board/get-trees world)
        available  (board/get-tanks world)]
    {:moment-created created
     :last-update    created
     :dimensions     dimensions
     :walls          walls
     :trees          trees
     :available      available
     :playing        []
     :tanks          []
     :lasers         []
     :explosions     []}))
