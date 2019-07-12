(ns jvw.portal
  (:require [jvw.entity :as ent]))

(defn make [target]
  (assoc (ent/make :portal -1) :target target))

(defn is? [entity]
  (ent/is-type? entity :portal))

(defn target [portal]
  (portal :target))
