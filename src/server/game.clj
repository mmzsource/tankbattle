(ns server.game)

(def nato [:alpha :bravo :charlie :delta :echo :foxtrot :golf :hotel :india :juliet :kilo :lima :mike :november :oscar :papa :quebec :romeo :sierra :tango :uniform :victor :whiskey :x-ray :yankee :zulu])

(defn random-from [arr]
  (arr (rand-int (count arr))))

(defn player-id []
  (clojure.string/join "_" (map name (repeatedly 3 #(random-from nato)))))

(player-id)

(defn make []
  {:players {}})

(defn joinable? [game]
  (-> game (:identities) (keys) (count) (> 4)))

(defn novel-player-id [ids]
  (let [id (player-id)]
    (if (contains? ids id)
      (recur ids)
      id)))

(defn join [game]
  (let [ids (set (keys (game :players)))
        id (novel-player-id ids)]
    (assoc-in game [:players id] 1)))

(-> (make) (join) (join))
