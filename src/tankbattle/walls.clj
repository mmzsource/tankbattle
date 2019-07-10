(ns tankbattle.walls
  (:require [clojure.set :as s]))

(defn north-wall-positions [cols _]
  (set (for [x (range cols)] [x 0])))

(defn east-wall-positions [cols rows]
  (set (for [y (range rows)] [(dec cols) y])))

(defn south-wall-positions [cols rows]
  (set (for [x (range cols)] [x (dec rows)])))

(defn west-wall-positions [_ rows]
  (set (for [y (range rows)] [0 y])))

(defn wall-positions [cols rows]
  (s/union
   (north-wall-positions cols rows)
   (east-wall-positions  cols rows)
   (south-wall-positions cols rows)
   (west-wall-positions  cols rows)))

(defn create-walls [cols rows]
  (let [wps (wall-positions cols rows)]
    (mapv (fn [wall-position] {:position wall-position :uuid (java.util.UUID/randomUUID)}) wps)))
