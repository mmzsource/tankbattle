(ns tankbattle.floodfill)


(defn north-of [[row col]] [(dec row) col])
(defn south-of [[row col]] [(inc row) col])
(defn west-of  [[row col]] [row (dec col)])
(defn east-of  [[row col]] [row (inc col)])

(defn neighbours [rows cols cell]
  (filter
   (fn [[i j]] (and (< -1 i rows) (< -1 j cols)))
   ((juxt north-of east-of south-of west-of) cell)))

(defn flooded? [fallen-walls rows cols cell]
  (some fallen-walls (for [n (neighbours rows cols cell)] #{n cell})))

(defn find-unflooded-neighbours [fallen-walls rows cols cell]
  (let [n (neighbours rows cols cell)]
    (remove #(visited? fallen-walls rows cols %) n)))

(defn is-target-char? [c t]
  (= c t))

(defn flood [board [row col] target-char replacement-char]
  (if (is-target-char? (get-in board [row col]) target-char)
          (assoc-in board [row col] replacement-char)
          board))

(defn floodfill [board cell target-char]
  (let [replacement \~]
    (loop [flooded board
           backlog #{cell}]
      (if (not (empty? backlog))
        (let [flooded (flood flooded (first backlog) target-char replacement)
              ufn     (find-unflooded-neighbours board )])
        "flood (first backlog) && add unflooded neighbours to (rest backlog)"
        "return flooded board"))))



(defn test [board [col row] target-char]
  (let [backlog #{cell}]
    (if (not-empty? backlog)
      (str "add unflooded neighbours of" (first backlog) "to the backlog")
      (str "return flooded board" ))))

(def testworld [["wwww"] ["w..w"] ["wwww"]])

(defn to-chars [board]
  (mapv (fn [[row]] (into [] (seq row))) board))

(to-chars testworld)

;; row, col lookup (instead of col, row which would map better with x, y
(get-in (to-chars testworld) [1 1])


(is-target-char? (get-in (to-chars testworld) [1 1]) \.)

(let [char-world (to-chars testworld)]
  (floodfill char-world [1 1] \.))
