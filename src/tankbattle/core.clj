(ns tankbattle.core
  (:require [tankbattle.board :as board]))

(defn started? [world]
  (contains? world :game-end))

(defn players-subscribed?
  "more than zero players subscribed?"
  [world]
  (pos? (count (world :tanks))))

;; DEPRICATED
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

(defn create [world]
  {:moment-created (System/currentTimeMillis)
   :last-update    (System/currentTimeMillis)
   :dimensions     (board/get-dimensions world)
   :walls          (board/get-walls world)
   :trees          (board/get-trees world)
   :available      (board/get-tanks world)
   :playing        []
   :tanks          []
   :lasers         []
   :explosions     []})
