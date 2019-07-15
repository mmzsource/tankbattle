(ns jvw.tank
  (:require [jvw.entity :as ent]))

(defn make [orientation]
  (-> (ent/make :tank 3)
    (merge {:orientation orientation
            :moment-last-shot -999999
            :moment-last-move -999999})))

(defn is? [entity]
  (ent/is-type? entity :tank))

(defn orientation [tank] (tank :orientation))

(defn set-orientation [direction]
  (fn [tank] (assoc tank :orientation direction)))

(defn set-moment-last-shot [time]
  (fn [tank] (assoc tank :moment-last-shot time)))

(defn set-moment-last-move [time]
  (fn [tank] (assoc tank :moment-last-move time)))
