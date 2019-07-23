(ns server.game
  (:require [jvw.core :as core]
            [jvw.field :as fld]
            [jvw.entity :as ent]
            [jvw.tank :as tnk]))

(def nato [:alpha :bravo :charlie :delta :echo :foxtrot :golf :hotel :india :juliet :kilo :lima :mike :november :oscar :papa :quebec :romeo :sierra :tango :uniform :victor :whiskey :x-ray :yankee :zulu])

(defn random-from [arr]
  (arr (rand-int (count arr))))

(defn secret []
  (->> #(random-from nato) (repeatedly 4) (map name) (clojure.string/join "_")))

(defn novel-secret [secrets]
  (let [secret (secret)] (if (contains? secrets secret) (recur secrets) secret)))

(defn make [field]
  {:field field
   :secret->tank-id {}})

(defn joinable? [game]
  (fld/has-starting-placement? (game :field)))

(defn join [game name]
  (let [field (game :field)]
    (if (joinable? game)
      (let [secrets (set (keys (game :secret->tank-id)))
            secret (novel-secret secrets)
            [field-placement-taken [position orientation]] (fld/take-starting-placement field)
            tank (tnk/make orientation name)
            tank-id (ent/id tank)
            field-tank-introduced (fld/introduce field-placement-taken position tank)
            game-new (-> game
                      (assoc :field field-tank-introduced)
                      (assoc-in [:secret->tank-id secret] tank-id))]
        [game-new secret]))))

;TODO refactor x-command functions to a single function... how to deal with optional parameters?
(defn move-command [game tank-id direction]
  (let [field (game :field)
        [field-new worked?] (core/move field tank-id direction)]
    (if worked? [(assoc game :field field-new) true] [game false])))

(defn fire-command [game tank-id]
  (let [field (game :field)
        [field-new worked?] (core/shoot field tank-id)]
    (if worked? [(assoc game :field field-new) true] [game false])))

(defn mine-command [game tank-id direction]
  (let [field (game :field)
        [field-new worked?] (core/drop-mine field tank-id direction)]
    (if worked? [(assoc game :field field-new) true] [game false])))

(defn is-registered? [game secret]
  (contains? (game :secret->tank-id) secret))

(defn tank-id [game secret]
  (get-in game [:secret->tank-id secret]))

(defn command [game secret type & args]
  (let [tank-id (get-in game [:secret->tank-id secret])]
    (if tank-id
      (case type
        :move (let [[direction] args] (move-command game tank-id direction))
        :fire (fire-command game tank-id)
        :mine (let [[direction] args] (mine-command game tank-id direction))))))

(def game (make (fld/make 4 2)))
(def joined (join game "tenkske"))
(def game-joined (let [[g _] joined] g))
(def scrt (let [[_ s] joined] s))
(def tnk-id (tank-id game-joined scrt))

(let [[game-new scrt] (join game "kei koele tenk")]
  (tank-id game-new scrt))
