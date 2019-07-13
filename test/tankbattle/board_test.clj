(ns tankbattle.board-test
  (:require [tankbattle.board :refer :all]
            [clojure.test :refer :all]
            [tankbattle.board :as board]))

(defn first-error [result]
  (first (get-in result [:err :result])))

(deftest should-check-for-empty-worlds
  (let [world  [[] [] []]
        result (validate world)]
    (is (= (result :out) :none))
    (is (= (first-error result) "An empty world is not allowed."))))

(deftest should-check-for-rowsize
  (let [too-little-rows (validate [["w"] ["w"]])
        too-many-rows   (validate [(take 32 (repeatedly #(vec "w")))])]
    (is (= (too-little-rows :out) :none))
    (is (= (too-many-rows   :out) :none))
    (is (= (first-error too-little-rows) "Number of rows should be >= 3 and <= 31."))
    (is (= (first-error too-many-rows)   "Number of rows should be >= 3 and <= 31."))))

(defn thirty-two-chars [] (reduce str (repeat 32 "w")))

(deftest should-check-for-colssize
  (let [too-little-cols (validate [["ww"] ["ww"] ["ww"]])
        too-many-cols   (validate [[(thirty-two-chars)] [(thirty-two-chars)] [(thirty-two-chars)]])]
    (is (= (too-little-cols :out) :none))
    (is (= (too-many-cols   :out) :none))
    (is (= (first-error too-little-cols) "Number of columns should be >= 3 and <= 31."))
    (is (= (first-error too-many-cols)   "Number of columns should be >= 3 and <= 31."))))

(deftest should-check-for-equal-columns
  (let [unequal-cols (validate [["www"] ["wwww"] ["wwwww"]])]
    (is (= (unequal-cols :out) :none))
    (is (= (first-error unequal-cols) "Each row should have an equal amount of columns."))))

(deftest should-check-for-valid-chars
  (let [unvalid-chars (validate [["123"] ["456"] ["789"]])]
    (is (= (unvalid-chars :out) :none))
    (is (= (first-error unvalid-chars) "Only valid characters are: wall: w , tree: t , tank: 1 2 3 or 4 , empty: ."))))

(deftest should-check-for-tank-presence
  (let [no-tanks (validate [["www"] ["www"] ["www"]])]
    (is (= (no-tanks :out) :none))
    (is (= (first-error no-tanks) "Should contain at least 1 tank and that one should have id 1."))))

(deftest should-check-for-tank-duplication
  (let [duplicate-tanks (validate [["wwww"] ["w11w"] ["wwww"]])]
    (is (= (duplicate-tanks :out) :none))
    (is (= (first-error duplicate-tanks) "A tankid should not be duplicated on the board"))))

(deftest ensures-tanks-cannot-fall-off-the-board
  (let [leaking-board (validate [["www"] ["w1w"] ["w.w"]])]
    (is (= (leaking-board :out) :none))
    (is (first-error leaking-board) "Tanks should be surrounded by walls so they cannot vanish into the void")))

(deftest passes-valid-worlds
  (let [valid (validate [[".w."] ["w1w"] [".w."]])]
    (is (= (valid :err) :none))
    (is (= (valid :out) {:result "World is valid"}))))
