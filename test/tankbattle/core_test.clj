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
      (is (= (keys (first wls)) [:position])))))


;;;;;;;;;;;
;; TANKS ;;
;;;;;;;;;;;


(deftest tank-creation
  (testing "creation of a tank"
    (let [tank (create-tank 1 [2 3] :green "ALPHA-BRAVO-CHARLIE")]
      (is (map? tank))
      (is (= (into #{} (keys tank))
             #{:id :name :position :orientation :energy :color :last-shot :reloaded :last-move :restarted}))
      (is (= (tank :id)       1))
      (is (= (tank :name)     "ALPHA-BRAVO-CHARLIE"))
      (is (= (tank :position) [2 3]))
      (is (= (tank :color)    :green)))))

(deftest tank-subscription
  (testing "subscribing a tank to a game"
    (let [old-world {:last-update 123567890
                     :av-ids      #{1 2 3 4}
                     :av-pos      #{[1 1] [2 2] [3 3] [4 4]}
                     :av-cls      #{:red :green :yellow :blue}
                     :tanks       []}
          new-world (subscribe-tank old-world "Neo")
          new-tank  (first (new-world :tanks))]
      (is (= (count (:tanks  new-world)) 1))
      (is (= (count (:av-ids new-world)) 3))
      (is (= (count (:av-pos new-world)) 3))
      (is (= (count (:av-cls new-world)) 3))
      (is (= (conj (new-world :av-ids) (new-tank :id))       (old-world :av-ids)))
      (is (= (conj (new-world :av-pos) (new-tank :position)) (old-world :av-pos)))
      (is (= (conj (new-world :av-cls) (new-tank :color))    (old-world :av-cls)))
      (is (> (new-world :last-update) (old-world :last-update))))))

(deftest tank-subscription-locked-when-all-positions-are-taken
  (testing "if gameboard is filled, no more tanks are able to subscribe"
    (let [old-world {:tanks [{:tank :dummy} {:tank :dummy} {:tank :dummy} {:tank :dummy}]}
          new-world (subscribe-tank old-world "Dr.Strange")]
      (is (= new-world old-world)))))

(deftest tank-only-fires-when-not-moving
  (testing "tank only fires when it's not moving"
    (let [stopped-tank {:moving false :firing false}
          moving-tank  {:moving true  :firing false}]
      (is (= (fire moving-tank)  {:moving true :firing false}))
      (is (= (fire stopped-tank) {:moving false :firing true})))))


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
           (remove-objects bullet-position-map used-bullet-positions)
           [{:unused :bullet3} {:unused :bullet4} {:unused :bullet5}])))))

(deftest update-object-hits-test
  (testing "for every bullet hit, tank and tree energy should decrease
            in addition: all bullets involved in a hit should be removed from the world"
    (let [world    {:bullets [{:position [1 1]}
                              {:position [2 2] :direction :north}
                              {:position [2 2] :direction :east}
                              {:position [4 4]}
                              {:position [5 5]}
                              {:position [6 6]}]
                    :tanks   [{:position [1 1] :energy 5} {:position [2 2] :energy 7}]
                    :trees   [{:position [4 4] :energy 3}]
                    :walls   [{:position [6 6]}]
                    :therest :dontcare}
          result   (update-object-hits world)]
      (is (= (keys result) (keys world)))
      (is (= (result :tanks)   [{:position [1 1] :energy 4} {:position [2 2] :energy 5}]))
      (is (= (result :trees)   [{:position [4 4] :energy 2}]))
      (is (= (result :walls)   (world :walls)))
      (is (= (result :bullets  [{:position [5 5]}]))))))

(deftest update-explosions-test
  (testing "explosions should decrease every gametick, zero energy explosions should be removed and
            tanks and trees with zero energy (or less) should be replaced by explosions"
    (let [world  {:tanks      [{:position [0 0] :energy 0} {:position [1 1] :energy 1}]
                  :trees      [{:position [2 2] :energy 0} {:position [3 3] :energy 2}]
                  :explosions [{:position [4 4] :energy 1} {:position [5 5] :energy 3}]}
          result (update-explosions world)]
      (is (= (result :tanks)      [{:position [1 1] :energy 1}]))
      (is (= (result :trees)      [{:position [3 3] :energy 2}]))
      (is (= (result :explosions) [{:position [5 5] :energy 2}
                                   {:position [2 2] :energy 7}
                                   {:position [0 0] :energy 7}])))))

;;;;;;;;;;;;;;;;;;
;; WORLD / GAME ;;
;;;;;;;;;;;;;;;;;;

(deftest game-started
  (testing "has the start command already been executed?"
    (let [started     {:game-end 1234567890 :therest :dontcare}
          not-started {:no-game-end-key :in-this-map}]
      (is (started? started))
      (is (not (started? not-started))))))

(deftest players-subscribed
  (testing "at least one player subscribed to the game"
    (let [one-subscription  {:tanks [{:some  :tank}]}
          two-subscriptions {:tanks [{:other :tank}]}
          no-subscriptions  {:tanks []}]
      (is (players-subscribed? one-subscription))
      (is (players-subscribed? two-subscriptions))
      (is (not (players-subscribed? no-subscriptions))))))

(deftest game-start
  (testing "starting of a game"
    (let [ready-to-start-game  {:tanks [{:some :tank}]}
          result               (start-game ready-to-start-game)
          already-started-game {:game-start 123 :game-end 12345}]
      (is (contains? result :game-start))
      (is (contains? result :game-end))
      (is (= (- (result :game-end) (result :game-start)) 300000)) ;; 5 minutes in milliseconds
      (is (= (result :game-start) (result :last-updated)))
      (is (= (start-game already-started-game) already-started-game)))))
