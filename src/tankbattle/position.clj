(ns tankbattle.position)

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
