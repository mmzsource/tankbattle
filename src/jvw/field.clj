(ns jvw.field
  (:require [jvw.entity :as ent]
            [jvw.tank :as tnk]))

(defn make [width height]
  {:dimensions {:width width :height height}
   :time 0
   :entities   {}
   :entity-positioning {}
   :tank-positions {}
   :lasers #{}
   :explosions {}})

(defn entity-id->entity [field entity-id]
  (get-in field [:entities entity-id]))

(defn introduce [field location entity]
  (let [id (ent/id entity)]
    (-> field
      (assoc-in [:entities id] entity)
      (assoc-in [:entity-positioning location] id)
      ((fn [field]
        (if (tnk/is? entity)
          (assoc-in field [:tank-positions id] location)
          field))))))

(defn remove-entity [field location]
  (let [entity-id (entity-id-at field location)]
    (-> field
      (update :entities dissoc entity-id)
      (update :entity-positioning dissoc location)
      ((fn [field]
          (if (tnk/is? (entity-id->entity field entity-id))
            (update field :tank-positions dissoc entity-id)
            field))))))

(defn clear-tank [field tank-id]
  (-> field
    (update :entity-positioning dissoc (tank-position field tank-id))
    (update :tank-positions dissoc tank-id)))

(defn introduce-laser [field laser]
  (update field :lasers conj laser))

(defn introduce-explosion [field location explosion]
  (assoc-in field [:explosions location] explosion))

(defn time [field] (field :time))

(defn tank-position [field tank-id]
  (get-in field [:tank-positions tank-id]))

(defn update-entity [field entity-id & fs]
  (reduce
      (fn [field f]
        (update-in field [:entities entity-id] f))
      field fs))

(defn update-position [field entity-id location]
  (-> field
    (update :entity-positioning dissoc (tank-position field entity-id))
    (assoc-in [:entity-positioning location] entity-id)
    ((fn [field]
      (if (tnk/is? (entity-id->entity field entity-id))
        (assoc-in field [:tank-positions entity-id] location))))))

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

(defn entity-id-at [field location]
  (get-in field [:entity-positioning location]))

(defn entity-at [field location]
  (entity-id->entity field (entity-id-at field location)))
