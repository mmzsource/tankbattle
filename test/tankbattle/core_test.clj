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

(deftest update-bullet-positions-test
  (testing "should update the positions of the bullets in the world"
    (let [bullet1       {:position [1 1] :energy 1 :direction :east  :tankid 1}
          bullet2       {:position [2 2] :energy 1 :direction :south :tankid 2}
          world         {:bullets    [bullet1 bullet2]
                         :tanks      :dontcare
                         :trees      :dontcare
                         :alltherest :dontcare}
          result        (update-bullet-positions world)
          moved-bullets (:bullets result)]
      (is (= (count moved-bullets) 2))
      (is (= (into #{} (map :position moved-bullets)) #{[2 1] [2 3]}))
      (is (= (keys result)  (keys world)))
      (is (= (keys bullet1) (keys (first moved-bullets)))))))

(deftest update-energy-when-hit-test
  (testing "should decrease energy of gameobject with 'nr-of-hits'"
    (is (= (update-energy-when-hit {:energy 5} 3) {:energy 2}))))

(deftest update-hit-objects-test
  (testing "should decrease the energy of a gameobject when it's hit by one or more bullets"
    (let [tanks-position-map  {[1 1] [{:energy 3 :other-properties :dont-care}]
                               [2 2] [{:energy 9 :other-properties :dont-care}]}
          bullet-position-map {;; one bullet in position [1 1]
                               [1 1] [{:properties :dontcare}]
                               ;; two bullets in position [2 2]
                               [2 2] [{:properties :dontcare} {:properties :dontcare}]}
          resulting-tanks     (update-hit-objects tanks-position-map bullet-position-map)]
      (is (= resulting-tanks [{:energy 2 :other-properties :dont-care}
                              {:energy 7 :other-properties :dont-care}])))))

(deftest remove-used-bullets-test
  (testing "should remove bullets that are used / have decreased gameobject energy"
    (let [bullet-position-map  {[1 1] [{:used   :bullet1}]
                                [2 2] [{:used   :bullet2}]
                                [3 3] [{:unused :bullet3}]
                                [4 4] [{:unused :bullet4} {:unused :bullet5}]}
          used-bullet-positions #{[1 1] [2 2]}]
      (is (=
           (remove-used-bullets bullet-position-map used-bullet-positions)
           [{:unused :bullet3} {:unused :bullet4} {:unused :bullet5}])))))

(deftest update-object-hits-test
  (testing "for every bullet hit, tank and tree energy should decrease
            in addition: all bullets involved in a hit should be removed from the world"
    (let [bullet1  {:position [1 1]}
          bullet2a {:position [2 2]}
          bullet2b {:position [2 2]}
          bullet4  {:position [4 4]}
          bullet5  {:position [5 5]}
          bullet6  {:position [6 6]}
          tank1    {:position [1 1] :energy 5}
          tank2    {:position [2 2] :energy 7}
          tree1    {:position [4 4] :energy 3}
          wall1    {:position [6 6]}
          world    {:bullets  [bullet1 bullet2a bullet2b bullet4 bullet5 bullet6]
                    :tanks    [tank1 tank2]
                    :trees    [tree1]
                    :walls    [wall1]
                    :therest  :dontcare}
          result   (update-object-hits world)]
      (is (= (keys result) (keys world)))
      (is (= (result :tanks)   [{:position [1 1] :energy 4} {:position [2 2] :energy 5}]))
      (is (= (result :trees)   [{:position [4 4] :energy 2}]))
      (is (= (result :walls)   (world :walls)))
      (is (= (result :bullets  [bullet5])))
      (is (= (result :therest) :dontcare)))))
