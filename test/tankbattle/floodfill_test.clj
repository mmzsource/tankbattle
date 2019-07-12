(ns tankbattle.floodfill-test
  (:require [tankbattle.floodfill :refer :all]
            [clojure.test :refer :all]))

(deftest floodfill-nil
  (let [board [[\w \w \w] [\w \w \w] [\w \w \w]]]
    (is (= (floodfill board nil nil) board))))

(deftest floodfill-wrong-board-structure
  (let [board "not-a-vec-of-vec-of-chars"]
    (is (= (floodfill board nil nil) board))))

(deftest floodfill-one-cell
  (let [board    [[\w \w \w] [\w \. \w] [\w \w \w]]
        expected [[\w \w \w] [\w \~ \w] [\w \w \w]]]
    (is (=  (floodfill board [1 1] \.) expected))))

(deftest floodfill-two-cells
  (let [north   [[\w \. \w] [\w \. \w] [\w \w \w]]
        e-north [[\w \~ \w] [\w \~ \w] [\w \w \w]]
        east    [[\w \w \w] [\w \. \.] [\w \w \w]]
        e-east  [[\w \w \w] [\w \~ \~] [\w \w \w]]
        south   [[\w \w \w] [\w \. \w] [\w \. \w]]
        e-south [[\w \w \w] [\w \~ \w] [\w \~ \w]]
        west    [[\w \w \w] [\. \. \w] [\w \w \w]]
        e-west  [[\w \w \w] [\~ \~ \w] [\w \w \w]]]
    (is (= (floodfill north [1 1] \.) e-north))
    (is (= (floodfill north [0 1] \.) e-north))
    (is (= (floodfill east  [1 1] \.) e-east))
    (is (= (floodfill east  [1 2] \.) e-east))
    (is (= (floodfill south [1 1] \.) e-south))
    (is (= (floodfill south [2 1] \.) e-south))
    (is (= (floodfill west  [1 1] \.) e-west))
    (is (= (floodfill west  [1 0] \.) e-west))))

(deftest floodfill-world
  (let [world (to-chars [["......."]
                         ["...w..."]
                         ["..w.w.."]
                         [".w...w."]
                         ["..w.w.."]
                         ["...w..."]
                         ["......."]])
        inner (to-chars [["......."]
                         ["...w..."]
                         ["..w~w.."]
                         [".w~~~w."]
                         ["..w~w.."]
                         ["...w..."]
                         ["......."]])
        outer (to-chars [["~~~~~~~"]
                         ["~~~w~~~"]
                         ["~~w.w~~"]
                         ["~w...w~"]
                         ["~~w.w~~"]
                         ["~~~w~~~"]
                         ["~~~~~~~"]])]
    (is (= (floodfill world [2 3] \.) inner))
    (is (= (floodfill world [0 0] \.) outer))))

(deftest floodfill-leakd
  (let [world (to-chars [["....."]
                         [".www."]
                         [".w.w."]
                         [".w.w."]
                         ["....."]])
        leakd (to-chars [["~~~~~"]
                         ["~www~"]
                         ["~w~w~"]
                         ["~w~w~"]
                         ["~~~~~"]])]
    (is (= (floodfill world [2 2] \.) leakd))))
