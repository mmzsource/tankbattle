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


;;;;;;;;;;;;;
;; BORDERS ;;
;;;;;;;;;;;;;


(deftest north-border-position-calculation
  (testing "north border positions calculation"
    (let [positions (north-border-positions 4 6)]
      (is (= positions #{[0 0] [1 0] [2 0] [3 0]})))))

(deftest east-border-position-calculation
  (testing "east border positions calculation"
    (let [positions (east-border-positions 4 6)]
      (is (= positions #{[3 0] [3 1] [3 2] [3 3] [3 4] [3 5]})))))

(deftest south-border-position-calculation
  (testing "south border positions calculation"
    (let [positions (south-border-positions 4 6)]
      (is (= positions #{[0 5] [1 5] [2 5] [3 5]})))))

(deftest west-border-position-calculation
  (testing "west border positions calculation"
    (let [positions (west-border-positions 4 6)]
      (is (= positions #{[0 0] [0 1] [0 2] [0 3] [0 4] [0 5]})))))

(deftest border-generation
  (testing "border generation of a board"
    (let [borders (borders 4 6)]
      (is (map? borders))
      (is (= (count borders) 16))
      (is (= (first (vals borders)) {:energy -1 :type :border})))))


;;;;;;;;;;;;;;;
;; OBSTACLES ;;
;;;;;;;;;;;;;;;


(deftest rndobs1
  (testing "random obstacle position calculation"
    (let [rop (random-obstacle-position 10 10)]
      (is (vector? rop))
      (is (>= (first rop) 2))
      (is (<= (last  rop) 8)))))

(deftest rndobs2
  (testing "random obstacle position calculation forced into 1 cell"
    (let [rop (random-obstacle-position 4 4)]
      (is (= rop [2 2])))))

(deftest rndobs3
  (testing "random obstacle position*s* calculation proportional to board size"
    (let [rops-small (random-obstacle-positions  10  10)
          rops-large (random-obstacle-positions 100 100)]
      (is (> (count rops-small)    5))
      (is (< (count rops-small)   11))
      (is (> (count rops-large)  500))
      (is (< (count rops-large) 1001)))))

(deftest create-random-obstacle
  (testing "creation of random obstacle"
    (let [rndobs (random-obstacle)]
      (is (or (= rndobs {:energy  3 :type :tree})
              (= rndobs {:energy  5 :type :wall})
              (= rndobs {:energy 10 :type :bouncy-wall}))))))


(deftest obstacles-creation
  (testing "creation of obstacles"
    (let [obstacles (obstacles 10 10)]
      (is (map?      obstacles))
      (is (>         (count obstacles)) 5)
      (is (vector?   (first (keys obstacles))))
      (is (map?      (first (vals obstacles))))
      (is (contains? (first (vals obstacles)) :energy))
      (is (contains? (first (vals obstacles)) :type)))))


;;;;;;;;;;;
;; TANKS ;;
;;;;;;;;;;;


(deftest tank-creation
  (testing "creation of a tank"
    (let [tank     (create-tank 1 [2 3] :green)
          tank-map (tank 1)]
      (is (map? tank-map))
      (is (contains? tank-map :position))
      (is (contains? tank-map :orientation))
      (is (contains? tank-map :energy))
      (is (contains? tank-map :color))
      (is (contains? tank-map :moving))
      (is (contains? tank-map :firing))
      (is (contains? tank-map :bullets)))))

(deftest tank-driving
  (testing "tank reacts to driving command"
    (let [stopped-tank {:moving false}]
      (is (:moving (drive stopped-tank))))))

(deftest tank-stopping
  (testing "tank reacts to stop command"
    (let [moving-tank  {:moving true}]
      (is (= (:moving (stop moving-tank)) false)))))

(deftest tank-turning
  (testing "turing of a tank"
    (let [tank {:orientation :north}]
      (is (= (:orientation (turn tank :east))  :east))
      (is (= (:orientation (turn tank :south)) :south))
      (is (= (:orientation (turn tank :west))  :west))
      (is (= (:orientation (turn tank :north)) :north)))))

(deftest tank-only-fires-when-not-moving
  (testing "tank only fires when it's not moving"
    (let [stopped-tank {:moving false :firing false}
          moving-tank  {:moving true  :firing false}]
      (is (= (:firing (fire moving-tank))  false))
      (is (= (:firing (fire stopped-tank)) true)))))

(deftest tank-hold-your-fire
  (testing "tank holds fire when commanded to"
    (let [tank {:firing true}]
      (is (= (:firing (hold-fire tank)) false)))))

(deftest decrements-bullet-when-firing
  (testing "number of bullets is decremented when tank is firing"
    (let [tank        {:bullets 10}
          no-amo-tank {:bullets  0}]
      (is (= (:bullets (shot-bullet tank))        9))
      (is (= (:bullets (shot-bullet no-amo-tank)) 0)))))

(deftest executes-sequence-of-commands
  (testing "a tank can execute a sequence of commands"
    (let [tank         {:moving true :firing false :orientation :south}
          cmds         [:stop :fire :turn-east]
          updated-tank (execute-cmds tank cmds)]
      (is (= (:moving      updated-tank) false))
      (is (= (:firing      updated-tank) true))
      (is (= (:orientation updated-tank) :east)))))
