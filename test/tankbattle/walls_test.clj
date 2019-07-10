(ns tankbattle.walls-test
  (:require [clojure.test :refer :all]
            [tankbattle.walls :refer :all]))

(deftest north-wall-position-calculation
  (let [positions (north-wall-positions 4 6)]
    (is (= positions #{[0 0] [1 0] [2 0] [3 0]}))))

(deftest east-wall-position-calculation
  (let [positions (east-wall-positions 4 6)]
    (is (= positions #{[3 0] [3 1] [3 2] [3 3] [3 4] [3 5]}))))

(deftest south-wall-position-calculation
  (let [positions (south-wall-positions 4 6)]
    (is (= positions #{[0 5] [1 5] [2 5] [3 5]}))))

(deftest west-wall-position-calculation
  (let [positions (west-wall-positions 4 6)]
    (is (= positions #{[0 0] [0 1] [0 2] [0 3] [0 4] [0 5]}))))

(deftest wall-generation
  (let [wls (create-walls 4 6)]
    (is (vector? wls))
    (is (= (count wls) 16))
    (is (= (into #{} (keys (first wls))) #{:uuid :position}))))
