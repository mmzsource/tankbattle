(ns jvw.entity)

(defn make [type energy]
  {:id (java.util.UUID/randomUUID) :type type :energy energy})

(defn id [entity]
  (entity :id))

(defn is-type? [entity type]
  (= (entity :type) type))

(defn reduce-energy [entity]
  (update-in entity [:energy] dec))
