(ns tankbattle.floodfill
  (:require [tankbattle.position :as pos]))

;; https://en.wikipedia.org/wiki/Flood_fill

(defn- neighbours [board cell]
  (let [rows (count board)
        cols (count (first board))]
    (filter
     (fn [[i j]] (and (< -1 i rows) (< -1 j cols)))
     ((juxt pos/north-of pos/east-of pos/south-of pos/west-of) cell))))

(defn to-chars [board]
    (mapv (fn [[row]] (vec (seq row))) board))

(defn- is-target-char? [c t]
  (= c t))

(defn- find-unflooded-neighbours [board cell target-char]
  (let [neighbours (neighbours board cell)]
    (filter #(is-target-char? (get-in board %) target-char) neighbours)))

(defn- flood [board [row col] target-char replacement-char]
  (if (is-target-char? (get-in board [row col]) target-char)
          (assoc-in board [row col] replacement-char)
          board))

(defn floodfill [board cell target-char]
  (loop [flooded board
         backlog (if (is-target-char? (get-in board cell) target-char)
                   #{cell}
                   #{})]
    (if-not (empty? backlog)
      (let [cell    (first backlog)
            flooded (flood flooded cell target-char \~)
            ufn     (find-unflooded-neighbours flooded cell target-char)
            backlog (into (rest backlog) ufn)]
        (recur flooded backlog))
      flooded)))
