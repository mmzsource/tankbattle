(ns tankbattle.core-test
  (:require [clojure.test :refer :all]
            [tankbattle.core :refer :all]))


;;;;;;;;;;;;;;;
;; POSITIONS ;;
;;;;;;;;;;;;;;;


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


;;;;;;;;;;;
;; WALLS ;;
;;;;;;;;;;;


(deftest north-wall-position-calculation
  (testing "north wall positions calculation"
    (let [positions (north-wall-positions 4 6)]
      (is (= positions #{[0 0] [1 0] [2 0] [3 0]})))))

(deftest east-wall-position-calculation
  (testing "east wall positions calculation"
    (let [positions (east-wall-positions 4 6)]
      (is (= positions #{[3 0] [3 1] [3 2] [3 3] [3 4] [3 5]})))))

(deftest south-wall-position-calculation
  (testing "south wall positions calculation"
    (let [positions (south-wall-positions 4 6)]
      (is (= positions #{[0 5] [1 5] [2 5] [3 5]})))))

(deftest west-wall-position-calculation
  (testing "west wall positions calculation"
    (let [positions (west-wall-positions 4 6)]
      (is (= positions #{[0 0] [0 1] [0 2] [0 3] [0 4] [0 5]})))))

(deftest wall-generation
  (testing "wall generation of a board"
    (let [wls (create-walls 4 6)]
      (is (vector? wls))
      (is (= (count wls) 16))
      (is (= (keys (first wls)) [:position :energy])))))


;;;;;;;;;;;
;; TANKS ;;
;;;;;;;;;;;


(deftest tank-creation
  (testing "creation of a tank"
    (let [tank (create-tank 1 [2 3] :green)]
      (is (map? tank))
      (is (= (keys tank) [:id :position :orientation :energy :color :moving :firing :bullets]))
      (is (= (tank :id)       1))
      (is (= (tank :position) [2 3]))
      (is (= (tank :color)    :green)))))

(deftest tank-driving
  (testing "tank reacts to driving command"
    (let [stopped-tank {:moving false}]
      (is (= (drive stopped-tank) {:moving true})))))

(deftest tank-stopping
  (testing "tank reacts to stop command"
    (let [moving-tank  {:moving true}]
      (is (= (stop moving-tank) {:moving false})))))

(deftest tank-turning
  (testing "turing of a tank"
    (let [tank {:orientation :north}]
      (is (= (turn tank :east)  {:orientation :east}))
      (is (= (turn tank :south) {:orientation :south}))
      (is (= (turn tank :west)  {:orientation :west}))
      (is (= (turn tank :north) {:orientation :north})))))

(deftest tank-only-fires-when-not-moving
  (testing "tank only fires when it's not moving"
    (let [stopped-tank {:moving false :firing false}
          moving-tank  {:moving true  :firing false}]
      (is (= (fire moving-tank)  {:moving true :firing false}))
      (is (= (fire stopped-tank) {:moving false :firing true})))))

(deftest tank-hold-your-fire
  (testing "tank holds fire when commanded to"
    (let [tank {:firing true}]
      (is (= (hold-fire tank) {:firing false})))))

(deftest decrements-bullet-when-firing
  (testing "number of bullets is decremented when tank is firing"
    (let [tank        {:bullets 10}
          no-amo-tank {:bullets  0}]
      (is (= (shot-bullet tank) {:bullets 9}))
      (is (= (shot-bullet no-amo-tank) {:bullets 0})))))

(deftest executes-sequence-of-commands
  (testing "a tank can execute a sequence of commands"
    (let [tank         {:moving true :firing false :orientation :south}
          cmds         [:stop :fire :turn-east]
          updated-tank (execute-cmds tank cmds)]
      (is (= updated-tank {:moving false :firing true :orientation :east})))))


;;;;;;;;;;;;;
;; BULLETS ;;
;;;;;;;;;;;;;


(deftest bullet-movement
  (testing "bullet moves 1 cell in the direction it's flying"
    (let [north {:position [3 3] :direction :north}
          east  {:position [3 3] :direction :east}
          south {:position [3 3] :direction :south}
          west  {:position [3 3] :direction :west}]
      (is (= (move-bullet north) {:position [3 2] :direction :north}))
      (is (= (move-bullet east)  {:position [4 3] :direction :east}))
      (is (= (move-bullet south) {:position [3 4] :direction :south}))
      (is (= (move-bullet west)  {:position [2 3] :direction :west})))))
