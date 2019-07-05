(ns tankbattle.core-test
  (:require [clojure.test :refer :all]
            [tankbattle.core :refer :all]))

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


(deftest find-tank-test
  (testing "finding a tank in the world"
    (let [world {:tanks [{:id 1} {:id 2}]}]
      (is (= (find-tank world 1) {:id 1}))
      (is (= (find-tank world 2) {:id 2}))
      (is (= (find-tank world 3) nil)))))

(deftest valid-tankid-test
  (testing "is a given tankid (still) valid?"
    (let [world {:tanks [{:id 1} {:id 2}]}]
      (is (valid-tankid? world 1))
      (is (valid-tankid? world 2))
      (is (not (valid-tankid? world 3))))))

(deftest valid-tank-cmd-test
  (testing "valid tank commands"
    (is (valid-tank-cmd? "north"))
    (is (valid-tank-cmd? "east"))
    (is (valid-tank-cmd? "south"))
    (is (valid-tank-cmd? "west"))
    (is (valid-tank-cmd? "fire"))
    (is (not (valid-tank-cmd? "burp!")))))

(deftest tank-creation
  (testing "creation of a tank"
    (let [tank (create-tank 1 [2 3] :green "ALPHA-BRAVO-CHARLIE")]
      (is (map? tank))
      (is (= (into #{} (keys tank))
             #{:id :name :position :orientation :energy :color
               :last-shot :reloaded :last-move :restarted :hits :kills}))
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

(deftest move-tank-to-empty-position
  (testing "moving tank to an empty position"
    (let [world {:walls   []
                 :trees   []
                 :tanks   [{:id 1 :position [2 2] :restarted 12345 :orientation :foo}]
                 :therest :dontcare}
          north (move world 1 "north")
          east  (move world 1 "east")
          south (move world 1 "south")
          west  (move world 1 "west")
          north-tank (first (north :tanks))
          east-tank  (first (east  :tanks))
          south-tank (first (south :tanks))
          west-tank  (first (west  :tanks))]

      (is (= (north-tank :position)    [2 1]))
      (is (= (north-tank :orientation) :north))
      (is (= (east-tank  :position)    [3 2]))
      (is (= (east-tank  :orientation) :east))
      (is (= (south-tank :position)    [2 3]))
      (is (= (south-tank :orientation) :south))
      (is (= (west-tank  :position)    [1 2]))
      (is (= (east-tank  :orientation) :east)))))

(deftest cannot-move-tank-to-occupied-position
  (testing "you cannot move a tank to an occupied position
            to test that, there are walls, trees and other tanks surrounding
            the tank under test"
    (let [world {:walls [{:position [2 1]}]
                 :trees [{:position [3 2]} {:position [2 3]}]
                 :tanks [{:id 1 :position [2 2] :restarted 12345}
                         {:id 2 :position [1 2] :restarted 67890}]}
          north (move world 1 "north")
          east  (move world 1 "east")
          south (move world 1 "south")
          west  (move world 1 "west")]
      (is (= north world))
      (is (= east  world))
      (is (= south world))
      (is (= west  world)))))

(deftest cannot-move-tank-thats-not-restarted
  (testing "a tank that isn't restarted yet, is not allowed to move"
    (let [now-plus-24-hours-in-millis (+ (System/currentTimeMillis) (* 24 60 60 1000))
          world {:walls []
                 :trees []
                 :tanks [{:id 1 :position [2 2] :restarted now-plus-24-hours-in-millis}]}
          north (move world 1 "north")]
      (is (= north world)))))

(deftest test-shooting-a-wall
  (testing "shooting a wall"
    (let [world {:tanks  [{:id 1 :position [1 2] :orientation :east
                           :last-move 12345 :restarted 123456
                           :last-shot 12345 :reloaded 123456}]
                 :walls  [{:position [2 2]}]
                 :trees  []
                 :lasers []}
          result (fire world 1)]
      (is (> ((first (result :tanks)) :last-shot)) ((first (world :tanks)) :last-shot))
      (is (> ((first (result :tanks)) :reloaded)) ((first (world :tanks)) :reloaded))
      (is (= (count (result :lasers)) 1))
      (is (= (into #{} (keys result)) #{:tanks :walls :trees :lasers :last-update})))))

(deftest test-shooting-a-tree
  (testing "shooting a tree"
    (let [world {:tanks      [{:id 1 :position [1 2] :orientation :east
                               :last-move 12345 :restarted 123456
                               :last-shot 12345 :reloaded  123456}]
                 :walls      []
                 :trees      [{:position [2 2] :energy 3}]
                 :lasers     []
                 :explosions []}
          result (fire world 1)]
      (is (> ((first (result :tanks)) :last-shot)) ((first (world :tanks)) :last-shot))
      (is (> ((first (result :tanks)) :reloaded)) ((first (world :tanks)) :reloaded))
      (is (= ((first (result :trees)) :energy)) 2)
      (is (= (count (result :explosions)) 0))
      (is (= (count (result :lasers)) 1))
      (is (= (into #{} (keys result)) #{:tanks :walls :trees :lasers :last-update :explosions})))))

(deftest test-exploding-a-tree
  (testing "exploding a tree"
    (let [world {:tanks      [{:id 1 :position [1 2] :orientation :east
                               :last-move 12345 :restarted 123456
                               :last-shot 12345 :reloaded  123456}]
                 :walls      []
                 :trees      [{:position [2 2] :energy 1}]
                 :lasers     []
                 :explosions []}
          result (fire world 1)]
      (is (> ((first (result :tanks)) :last-shot)) ((first (world :tanks)) :last-shot))
      (is (> ((first (result :tanks)) :reloaded)) ((first (world :tanks)) :reloaded))
      (is (= (count (result :trees)) 0))
      (is (= (count (result :explosions)) 1))
      (is (= (count (result :lasers)) 1))
      (is (= (into #{} (keys result)) #{:tanks :walls :trees :lasers :last-update :explosions})))))

(deftest shooting-a-tank
  (testing "shooting a tank"
    (let [world {:tanks      [{:id 1 :position [1 2] :orientation :east
                               :last-move 12345 :restarted 123456
                               :last-shot 12345 :reloaded  123455
                               :hits [] :kills []}
                              {:id 2 :position [2 2] :energy 10}]
                 :walls      []
                 :trees      []
                 :lasers     []
                 :explosions []}
          result (fire world 1)
          source (first (result :tanks))
          target (last  (result :tanks))]
      (is (> (source :last-shot)) ((first (world :tanks)) :last-shot))
      (is (> (source :reloaded))  ((first (world :tanks)) :reloaded))
      (is (= (source :hits) [2])) ;; hit tank with :id *2*
      (is (= (source :kills) []))
      (is (= (target :energy) 9))
      (is (= (count (result :explosions)) 0))
      (is (= (count (result :lasers)) 1))
      (is (= (into #{} (keys result)) #{:tanks :walls :trees :lasers :last-update :explosions})))))

(deftest exploding-a-tank
  (testing "exploding a tank"
    (let [world {:tanks      [{:id 1 :position [1 2] :orientation :east
                               :last-move 12345 :restarted 123456
                               :last-shot 12345 :reloaded  123455
                               :hits [] :kills []}
                              {:id 2 :position [2 2] :energy 1}]
                 :walls      []
                 :trees      []
                 :lasers     []
                 :explosions []}
          result (fire world 1)
          source (first (result :tanks))]
      (is (> (source :last-shot)) ((first (world :tanks)) :last-shot))
      (is (> (source :reloaded))  ((first (world :tanks)) :reloaded))
      (is (= (source :hits) [2]))  ;; hit tank with :id *2*
      (is (= (source :kills) [2])) ;; killed tank with :id *2*
      (is (= (source :id) 1))      ;; did we remove the correct tank?
      (is (= (count (result :tanks)) 1))
      (is (= (count (result :explosions)) 1))
      (is (= (count (result :lasers)) 1))
      (is (= (into #{} (keys result)) #{:tanks :walls :trees :lasers :last-update :explosions})))))

(defn update-tanks [world tank]
  (let [tanks         (world :tanks)
        updated-tanks (conj tanks tank)]
    (assoc world :tanks updated-tanks)))

(deftest find-the-right-object-to-shoot
  (testing "should shoot the first object in the direction its oriented to"
    (let [world {:tanks       [{:id 20 :position [2 0] :energy 20}
                               {:id 32 :position [3 2] :energy 32}
                               {:id 23 :position [2 3] :energy 23}
                               {:id 2  :position [0 2] :energy 2}]
                 :trees       [{:position [2 1] :energy 21}
                               {:position [4 2] :energy 42}]
                 :walls       [{:position [2 4]}
                               {:position [1 2]}]
                 :lasers      []
                 :explosition []}
          north-oriented {:id 1 :position [2 2] :orientation :north
                          :last-move 12345 :restarted 123456
                          :last-shot 12345 :reloaded  123456
                          :hits [] :kills []}
          east-oriented  (assoc north-oriented :orientation :east)
          south-oriented (assoc north-oriented :orientation :south)
          west-oriented  (assoc north-oriented :orientation :west)
          north          (fire (update-tanks world north-oriented) 1)
          east           (fire (update-tanks world east-oriented) 1)
          south          (fire (update-tanks world south-oriented) 1)
          west           (fire (update-tanks world west-oriented) 1)]
      (is (= ((first (north :trees))  :energy)) 20)    ;; hit!
      (is (= ((find-tank north 20)    :energy)) 20)    ;; untouched
      (is (= ((find-tank east  32)    :energy)) 31)    ;; hit!
      (is (= ((second (east  :trees)) :energy)) 42)    ;; untouched
      (is (= ((find-tank south 23)    :energy)) 22)    ;; hit!
      (is (= ((find-tank west  2)     :energy))  1)))) ;; hit!

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
