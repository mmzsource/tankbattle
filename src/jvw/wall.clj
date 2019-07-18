(ns jvw.wall
  (:require [jvw.entity :as ent]))

(defn make []
  (ent/make :wall -1))

(defn is? [entity]
  (ent/is-type? entity :wall))
