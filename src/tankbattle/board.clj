(ns tankbattle.board
  (:require [clojure.string :as str]
            [tankbattle.floodfill :as ff]))

(defn- empty-world [char-world]
  (when (empty? (flatten char-world))
    "An empty world is not allowed."))

(defn- rows-size [char-world]
  (when (or  (< (count char-world) 3)
             (> (count char-world) 31))
    "Number of rows should be >= 3 and <= 31."))

(defn- cols-size [char-world]
  (when (or (< (count (first char-world)) 3)
            (> (count (first char-world)) 31))
    "Number of columns should be >= 3 and <= 31."))

(defn- cols-count [char-world]
  (let [cols (count (first char-world))]
    (when-not (every? #(= (count %) cols) char-world)
      "Each row should have an equal amount of columns.")))

(def valid-chars #{\w \t \1 \2 \3 \4 \.})

(defn- known-chars [char-world]
  (when-not (every? #(contains? valid-chars %) (flatten char-world))
    "Only valid characters are: wall: w , tree: t , tank: 1 2 3 or 4 , empty: ."))

(defn- tank-presence [char-world]
  (when-not (some #(= % \1) (flatten char-world))
    "Should contain at least 1 tank and that one should have id 1."))

(defn- count-tank-occurrences [char-world tank-char]
  (count (filter #(= tank-char %) (flatten char-world))))

(defn- tank-duplication [char-world]
  (let [occurrences (map #(count-tank-occurrences char-world %) [\1 \2 \3 \4])]
    (when-not (every? #(< % 2) occurrences)
      "A tankid should not be duplicated on the board")))

(defn- valid-boundary [char-coll]
  (every? #(or (= \w %) (= \. %)) char-coll))

(defn- north [char-world]
  (let [first-row (first char-world)]
    (valid-boundary first-row)))

(defn- east [char-world]
  (let [last-col (map last char-world)]
    (valid-boundary last-col)))

(defn- south [char-world]
  (let [last-row (last char-world)]
    (valid-boundary last-row)))

(defn- west [char-world]
  (let [first-col (map first char-world)]
    (valid-boundary first-col)))

(defn- checkborders [char-world]
  (every? true? ((juxt north east south west) char-world)))

(defn- collect-tankpos [char-world]
  (for [row   (range 0 (count char-world))
        col   (range 0 (count (first char-world)))
        :when (contains? #{\1 \2 \3 \4} (get-in char-world [row col]))]
    [row col]))

(defn- replace-volatiles
  "only walls and empty spaces last 'forever'. the rest of the gameobjects is volatile"
  [world-row]
  [(str/replace (apply str world-row) #"[^w.]" ".")])

(defn- prepare-floodfill [char-world]
  (ff/to-chars (mapv replace-volatiles char-world)))

(defn- surrounded [char-world]
  (let [tankpos  (collect-tankpos char-world)
        prepared (prepare-floodfill char-world)
        flooded  (map #(ff/floodfill prepared % \.) tankpos)
        checked  (map checkborders flooded)]
    (when-not (every? #(= true %) checked)
      "Tanks should be surrounded by walls so they cannot vanish into the void")))

(def world-structure-rules
  [empty-world rows-size cols-size cols-count known-chars tank-presence tank-duplication surrounded])

(defn validate [world]
  (let [char-world        (mapv (fn [[row]] (vec (seq row))) world) ;; convert to vec of vec of chars
        validation-errors (->> world-structure-rules
                               (map #(% char-world))
                               (remove nil?)
                               sequence)]
    (if (empty? validation-errors)
      {:out {:result "World is valid"} :err :none}
      {:out :none                      :err {:result (into [] validation-errors)}})))

(comment

(validate [["wwwwwwwwwwww"]
           ["w....1.....w"]
           ["w..........w"]
           ["w...tttt...w"]
           ["w..t....t..w"]
           ["w..t....t.4w"]
           ["w3.t....t..w"]
           ["w..t....t..w"]
           ["w...tttt...w"]
           ["w..........w"]
           ["w.....2....w"]
           ["wwwwwwwwwwww"]])

(validate
  [["wwwwwww.............."]
   ["w1.....w............."]
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
   [".w2...w......w.....3w"]
   ["wwwwww........wwwwwww"]])

)
