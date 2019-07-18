(ns jvw.mine
  (:require [jvw.entity :as ent]))

(defn make [tank-id]
  (assoc (ent/make :mine -1) :tank-id tank-id))

(defn is? [entity] (ent/is-type? entity :mine))

(defn tank-id [mine] (mine :tank-id))
