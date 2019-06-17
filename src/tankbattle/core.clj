(ns tankbattle.core
  (:require [clojure.set :as s]))

(def directions #{:north :east :south :west})

;; Validation of ids, commands, moves, directions etc will be done once on the
;; server boundary (as opposed to validating the values in each and every
;; function)


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


;;;;;;;;;;;;
;; BORDER ;;
;;;;;;;;;;;;


(defn north-border-positions [cols rows]
  (into #{} (for [x (range cols)] [x 0])))

(defn east-border-positions [cols rows]
  (into #{} (for [y (range rows)] [(dec cols) y])))

(defn south-border-positions [cols rows]
  (into #{} (for [x (range cols)] [x (dec rows)])))

(defn west-border-positions [cols rows]
  (into #{} (for [y (range rows)] [0 y])))

(defn border-positions [cols rows]
  (s/union
   (north-border-positions cols rows)
   (east-border-positions  cols rows)
   (south-border-positions cols rows)
   (west-border-positions  cols rows)))

(defn borders [cols rows]
  (zipmap (border-positions cols rows) (repeat {:energy -1 :type :border})))


;;;;;;;;;;;;;;;
;; OBSTACLES ;;
;;;;;;;;;;;;;;;


(defn random-obstacle-positions
  "returns a set of random obstacle positions proportional to the size of the board"
  [cols rows]
  (let [nr-of-obstacles (int (* 0.1 (* cols rows)))]
    (into #{} (repeatedly nr-of-obstacles #(generate-random-position 2 (- cols 2) 2 (- rows 2))))))

(defn obstacles
  "returns a map of obstacles with random positions"
  [cols rows]
  (let [positions (random-obstacle-positions cols rows)
        obstacles (repeatedly
                   (count positions)
                   #(rand-nth [{:energy  3 :type :tree}
                               {:energy  5 :type :wall}
                               {:energy 10 :type :bouncy-wall}]))]
    (zipmap positions obstacles)))

;;;;;;;;;;;
;; TANKS ;;
;;;;;;;;;;;


(defn create-tank [id position color]
  {id {:position  position
       :direction (first (shuffle directions))
       :energy    10
       :color     color
       :moving    false
       :firing    false
       :bullets   100}})

(defn get-tank [world id]
  (get-in world [:tanks id]))

(defn drive [tank]
  (merge tank {:moving true}))

(defn stop [tank]
  (merge tank {:moving false}))

(defn change-direction [tank direction]
  (merge tank {:orientation direction}))

(defn fire [tank]
  (merge tank {:firing true}))

(defn hold-fire [tank]
  (merge tank {:firing false}))

(defn shot-bullet [tank]
  (update tank :bullets dec))


;;;;;;;;;;;;;
;; BULLETS ;;
;;;;;;;;;;;;;


(defn reverse-direction [current-direction]
  (current-direction
   {:north :south
    :south :north
    :east  :west
    :west  :east}))

(defn hit-by-bullet [object]
  (update object :energy dec))

(defn destroyed? [object]
  (= (:energy object) 0))


;;;;;;;;;;;
;; WORLD ;;
;;;;;;;;;;;


;; Reserved positions for initial world state:
;;
;; bbbbbbbbbbbbbbbbbbbbbb
;; bttttttttttttttttttttb
;; bt******************tb
;; bt******************tb
;; bt******************tb
;; bt******************tb
;; bt******************tb
;; bt******************tb
;; bttttttttttttttttttttb
;; bbbbbbbbbbbbbbbbbbbbbb
;;
;; b = border
;; t = reserved for tank starting positions
;; * = reserved for obstacles (walls, trees, etc)
;;


(defn init-world [cols rows]
  {:tanks      (create-tank 1 [1 4] :red)
   :obstacles  (merge (borders cols rows) (obstacles cols rows))
   :bullets    {}
   :explosions {}})

(defn -main
  "It all starts here"
  [& args]
  (init-world 10 10))


(comment

(def world {:tanks       {1 {:position  [2 2]
                             :direction :south
                             :energy    10
                             :color     :blue
                             :moving    true
                             :firing    false
                             :bullets   256}
                          2 {:position  [3 4]
                             :direction :east
                             :energy    2
                             :color     :red
                             :moving    false
                             :firing    true
                             :bullets   311}}
            :obstacles   {[0 0] {:energy -1 :type :border}
                          [1 0] {:energy -1 :type :border}
                          [2 0] {:energy -1 :type :border}
                          [3 0] {:energy -1 :type :border}
                          [4 0] {:energy -1 :type :border}
                          [5 0] {:energy -1 :type :border}
                          [6 0] {:energy -1 :type :border}
                          [0 1] {:energy -1 :type :border}
                          [6 1] {:energy -1 :type :border}
                          [0 2] {:energy -1 :type :border}
                          [6 2] {:energy -1 :type :border}
                          [0 3] {:energy -1 :type :border}
                          [3 3] {:energy -8 :type :border}
                          [0 4] {:energy -1 :type :border}
                          [6 4] {:energy -1 :type :border}
                          [0 5] {:energy -1 :type :border}
                          [6 5] {:energy -1 :type :border}
                          [0 6] {:energy -1 :type :border}
                          [1 6] {:energy -1 :type :border}
                          [2 6] {:energy -1 :type :border}
                          [3 6] {:energy -1 :type :border}
                          [4 6] {:energy -1 :type :border}
                          [5 6] {:energy -1 :type :border}
                          [6 6] {:energy -1 :type :border}
                          [2 1] {:energy  3 :type :tree}
                          [3 4] {:energy  2 :type :tree}
                          [5 4] {:energy  5 :type :wall}
                          [1 5] {:energy  9 :type :bouncy-wall}}
            :bullets     {[4 4] {:direction :east}}
            :explosions {[1 2] {:counter 3}
                         [5 5] {:counter 5}}})

(def tank-cmd-backlog {1 [:east :stop :fire]
                       2 []})

  )
