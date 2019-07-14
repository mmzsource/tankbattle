(ns tankbattle.position-test
  (:require [clojure.test :refer :all]
            [tankbattle.position :refer :all]))

(deftest calculate-positions
  (let [nesw ((juxt north-of east-of south-of west-of) [4 4])]
    (is (= nesw [[4 3] [5 4] [4 5] [3 4]]))))

(deftest creates-position-map
  (testing "given a collection of gameobjects, creates a map from position to object"
    (let [explosion1   {:position [1 2] :energy 5}
          explosion2   {:position [5 5] :energy 3}
          explosions   [explosion1 explosion2]
          position-map (map-positions explosions)]
      (is (= (keys position-map) [[1 2] [5 5]]))
      (is (= (position-map [1 2]) [{:position [1 2] :energy 5}]))
      (is (= (position-map [5 5]) [{:position [5 5] :energy 3}])))))


(deftest can-filter-a-set-of-positions-based-on-an-orientation
  (testing "given an orientation, this function will only return
            the cells in the direction of the orientation"
         ;; 4 x 4 grid. [2 2]
    (let [position-set #{            [2 0]
                                     [2 1]
                         [0 2] [1 2] [2 2] [3 2]
                                     [2 3]      }]
      (is (= (filter-north-of [2 2] position-set) [[2 0] [2 1]]))
      (is (= (filter-east-of  [2 2] position-set) [[3 2]]))
      (is (= (filter-south-of [2 2] position-set) [[2 3]]))
      (is (= (filter-west-of  [2 2] position-set) [[0 2] [1 2]])))))

(deftest can-find-nearest-neighbour-given-a-positions-and-an-orientation
  (let [position-set #{            [2 0]
                       [2 1]
                       [0 2] [1 2] [2 2] [3 2]
                       [2 3]      }]
    (is (= (nearest-pos-given-orient [2 2] position-set :north) [2 1]))
    (is (= (nearest-pos-given-orient [2 2] position-set :east)  [3 2]))
    (is (= (nearest-pos-given-orient [2 2] position-set :south) [2 3]))
    (is (= (nearest-pos-given-orient [2 2] position-set :west)  [1 2]))))
