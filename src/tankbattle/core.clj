(ns tankbattle.core
  (:require [tankbattle.walls :as wall]
            [clojure.string   :as str]
            [the-flood.core   :as flood]))

;; Validation of ids, commands, moves, directions etc will be done once on the
;; server boundary (as opposed to validating the values in each and every
;; function)

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

(def valid-chars #{\w \t \1 \2 \3 \4 \.})

(defn empty-world? [split-world]
  (when (empty? (flatten split-world))
    "An empty world is not allowed."))

(defn rows-size? [split-world]
  (when (< (count split-world) 3)
    "Number of rows should be at least 3."))

(defn cols-size? [split-world]
  (when (< (count (first split-world)) 3)
    "Number of columns should be at least 3."))

(defn cols-count? [split-world]
  (let [cols (count (first split-world))]
    (when (not (every? #(= (count %) cols) split-world))
      "Each row should have an equal amount of cols.")))

(defn known-chars? [split-world]
  (when (not (every? #(contains? valid-chars %) (flatten split-world)))
    (format "Only valid characters are: wall: w , tree: t , tank: 1 2 3 or 4 , empty: .")))

(defn tanks? [split-world]
  (when (not (some #(= % \1) (flatten split-world)))
    "Should contain at least 1 tank and that one should have id 1."))

(defn north-walls? [split-world]
  (when (not (every? #(= \w %) (first split-world)))
    "First row should only contain walls"))

(defn south-walls? [split-world]
  (when (not (every? #(= \w %) (last split-world)))
    "Last row should only contain walls"))

(defn west-walls? [split-world]
  (let [first-col (map first split-world)]
    (when (not (every? #(= \w %) first-col))
      "First col should only contain walls")))

(defn east-walls? [split-world]
  (let [last-col (map last split-world)]
    (when (not (every? #(= \w %) last-col))
      "Last col should only contain walls")))

(def world-structure-rules
  [empty-world? rows-size? cols-size? cols-count? known-chars? tanks?
   north-walls? south-walls? west-walls? east-walls?])

(defn validate-world [world]
  (let [split-world (mapv vec (str/split-lines world))]
    (->> world-structure-rules
         (map #(% split-world))
         (remove nil?)
         sequence)))

(defn validate [world]
  (let [char-world        (mapv (fn [[row]] (into [] (seq row))) world) ;; convert to vec of vec of chars
        validation-errors (->> world-structure-rules
                               (map #(% char-world))
                               (remove nil?)
                               sequence)]
    (if (empty? validation-errors)
      {:out {:result "World is valid"} :err :none}
      {:out :none                      :err {:result (into [] validation-errors)}})))

(comment

(validate [["wwwwwwwwwwww"]
           ["w....11....w"]
           ["w..........w"]
           ["w...tttt...w"]
           ["w..t....t..w"]
           ["w3.t....t.4w"]
           ["w3.t....t.4w"]
           ["w..t....t..w"]
           ["w...tttt...w"]
           ["w..........w"]
           ["w....22....w"]
           ["wwwwwwwwwwwt"]])

(def default-board
  [["wwwwwwwwwwww"]
   ["w....11....w"]
   ["w..........w"]
   ["w...tttt...w"]
   ["w..t....t..w"]
   ["w3.t....t.4w"]
   ["w3.t....t.4w"]
   ["w..t....t..w"]
   ["w...tttt...w"]
   ["w..........w"]
   ["w....22....w"]
   ["wwwwwwwwwwww"]])

(def lambda-board
  [["wwwwwww.............."]
   ["w......w............."]
   ["wwww....w............"]
   ["....w....w..........."]
   [".....w....w.........."]
   ["......w....w........."]
   [".......w....w........"]
   ["......w......w......."]
   [".....w........w......"]
   ["....w....ww....w....."]
   ["...w....w..w....w...."]
   ["..w....w....w....wwww"]
   [".w....w......w......w"]
   ["wwwwww........wwwwwww"]])

(let [char-world (mapv (fn [[row]] (into [] (seq row))) lambda-board)]
  (flood/flood-fill char-world [10 0] "F" nil))

)
