(ns jvw.field
  (:require [jvw.entity :as ent]))

(defn make [width height]
  {:dimensions {:width width :height height}
              :entities   {}
              :tank-positions {}})

(defn tank-by-id [tank-id]
  )

(defn tank-position [field tank]
  (get-in field [:tank-positions (ent/id tank)]))

(defn update-tank-position [field tank position]
  (-> field
    (update :entities dissoc (tank-position field tank))
    (assoc-in [:entities position] tank)
    (assoc-in [:tank-positions (ent/id tank)] position)))

(defn move-tank [field tank direction]
  (update-tank-position field tank (adjacent (tank-position field tank) direction)))

(defn in? [field [c r]]
  (let [width  (get-in field [:dimensions :width])
        height (get-in field [:dimensions :height])]
    (and (<= 0 c (dec width)) (<= 0 r (dec height)))))

(defn adjacent [[c r] direction]
  (case direction
    :north  [c        (dec r)]
    :east   [(inc c)  r]
    :south  [c        (inc r)]
    :west   [(dec c)  r]))

(defn entity-at [field location]
  (get-in field [:entities location]))

(defn hitscan
  "In %field, originating a ray from %location (including %location) in %direction, returns the first non-empty location the ray encounters."
  [field location direction]
  (if (in? field location)
    (let [entity (get-in field [:entities location])]
      (if entity
        [location entity]
        (recur field (adjacent location direction) direction)))
    [nil nil]))
