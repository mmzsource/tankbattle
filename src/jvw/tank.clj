(ns jvw.tank
  (:require [jvw.entity :as ent]))

(defn make [orientation]
  (-> (ent/make :tank 3)
    (merge {:orientation orientation
            :moment-last-shot -999999
            :moment-last-move -999999
            :hits []
            :kills []})))

(defn is? [entity]
  (ent/is-type? entity :tank))

(defn orientation [tank] (tank :orientation))

(defn set-orientation [direction]
  (fn [tank] (assoc tank :orientation direction)))

(defn set-moment-last-shot [time]
  (fn [tank] (assoc tank :moment-last-shot time)))

(defn set-moment-last-move [time]
  (fn [tank] (assoc tank :moment-last-move time)))

(defn add-hit [victim-id]
  (fn [tank] (update tank :hits conj victim-id)))

(defn add-kill [victim-id]
  (fn [tank] (update tank :kills conj victim-id)))

(defn set-moment-killed [time]
  (fn [tank] (assoc tank :moment-killed time)))
