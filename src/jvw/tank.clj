(ns jvw.tank
  (:require [jvw.entity :as ent]))

(defn make []
  (ent/make :tank 3))

(defn is? [entity]
  (ent/is-type? entity :tank))
