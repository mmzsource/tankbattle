(ns tankbattle.position-test
  (:require [clojure.test :refer :all]
            [tankbattle.position :refer :all]))

(deftest rndpos1
  (testing "random position generation on the game board"
    (let [rndpos (generate-random-position 10 5)]
      (is (vector? rndpos))
      (is (=  (count rndpos)  2))
      (is (>= (first rndpos)  0))
      (is (<= (first rndpos) 10))
      (is (>= (last  rndpos)  0))
      (is (<= (last  rndpos)  5)))))

(deftest rndpos2
  (testing "random position generation on a restricted area on the game board"
    (let [rndpos (generate-random-position 2 4 6 8)]
      (is (>= (first rndpos) 2))
      (is (<= (first rndpos) 4))
      (is (>= (last  rndpos) 6))
      (is (<= (last  rndpos) 8)))))

(deftest rndpos3
  (testing "'random' position generation in column 3, row 4"
    (let [rndpos (generate-random-position 3 3 4 4)]
      (is (= rndpos [3 4])))))

(deftest calculate-positions
  (testing "proper north-east-south-west calculations on the board"
    (let [nesw ((juxt north-of east-of south-of west-of) [4 4])]
      (is (= nesw [[4 3] [5 4] [4 5] [3 4]])))))

(deftest creates-position-map
  (testing "given a collection of gameobjects, creates a map from position to object"
    (let [explosion1   {:position [1 2] :energy 5}
          explosion2   {:position [5 5] :energy 3}
          explosions   [explosion1 explosion2]
          position-map (map-positions explosions)]
      (is (= (keys position-map) [[1 2] [5 5]]))
      (is (= (position-map [1 2]) [{:position [1 2] :energy 5}]))
      (is (= (position-map [5 5]) [{:position [5 5] :energy 3}])))))

(deftest can-have-multiple-objects-on-one-position
  (testing "one position on the board can contain multiple gameobjects"
    (let [bullet1 {:position [2 2] :energy 1 :direction :east  :tankid 1}
          bullet2 {:position [2 2] :energy 1 :direction :south :tankid 2}
          position-map (map-positions [bullet1 bullet2])]
      (is (= (keys position-map) [[2 2]]))
      (is (= (count (position-map [2 2])) 2))
      (is (= (map :direction position-map)) [:east :south]))))

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

(deftest can-find-nearest-neighbour-given-an-orientation
  (testing "finds the nearest neighbour of a position given an orientation"
    (let [position-set #{            [2 0]
                                     [2 1]
                         [0 2] [1 2] [2 2] [3 2]
                                     [2 3]      }]
      (is (= (nearest-pos-given-orient [2 2] position-set :north) [2 1]))
      (is (= (nearest-pos-given-orient [2 2] position-set :east)  [3 2]))
      (is (= (nearest-pos-given-orient [2 2] position-set :south) [2 3]))
      (is (= (nearest-pos-given-orient [2 2] position-set :west)  [1 2])))))
