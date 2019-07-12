(ns jvw.mirror
  (:require [jvw.entity :as ent]))

(defn make [forward]
  (assoc (ent/make :mirror -1) :forward forward))

(defn is? [entity] (ent/is-type? entity :mirror))

(defn reflection [mirror direction]
  (if (mirror :forward)
    (case direction
      :north  :east
      :east   :north
      :south  :west
      :west   :south)
    (case direction
      :north  :west
      :east   :south
      :south  :east
      :west   :north)))
