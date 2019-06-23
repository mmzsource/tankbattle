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
  (let [[col row] position]
    (cond
      (= direction :north) (merge bullet {:position [col (dec row)]})
      (= direction :east)  (merge bullet {:position [(inc col) row]})
      (= direction :south) (merge bullet {:position [col (inc row)]})
      (= direction :west)  (merge bullet {:position [(dec col) row]})
      :else                bullet)))

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

(comment

;; Sequence is in line with the order in which the events came in (fair play)
(def tank-cmd-seq [[1 :turn-east] [1 :stop] [1 :fire] [2 :drive]] )

)
