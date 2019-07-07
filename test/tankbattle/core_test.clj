(ns tankbattle.core-test
  (:require [clojure.test :refer :all]
            [tankbattle.core :refer :all]))

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
