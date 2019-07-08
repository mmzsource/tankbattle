(ns server.core
  (:require
    [yada.yada :as yada]
    [schema.core :as s]
    [tankbattle.core :as core]
    [tankbattle.tank :as tank]))

; simple atom for exposing a global function so the server can close itself
(def server (atom nil))

(def new-world (core/init-world))

(def world (atom (core/init-world)))

(defn world-resource []
  (yada/resource
   {:methods {:get
              {:produces       #{"application/json" "application/edn"}
               :response       (fn [_]
                                 (let [clean-world (reset! world (core/cleanup @world))]
                                   (assoc clean-world :time (System/currentTimeMillis))))}}
    :access-control {:allow-origin "*"
                     :allow-credentials false
                     :expose-headers #{"X-Custom"}
                     :allow-methods #{:get}
                     :allow-headers ["Api-Key"]}}))

(defn update-world-resource []
  (yada/resource
   {:methods {:post
              {:response (fn [_]
                           (swap! world assoc :last-update (System/currentTimeMillis)))}}}))

(defn reset-world-resource []
  (yada/resource
   {:methods {:post
              {:parameters {:body {:secret s/Str}}
               :consumes   "application/json"
                :response (fn [ctx]
                           (if (= "do not cheat!" (get-in ctx [:parameters :body :secret]))
                            (reset! world new-world)))}}}))

(defn free-spot? [world]
  (pos? (count (world :av-ids))))

(defn subscribe-tank-resource []
  (yada/resource
   {:methods {:post
              {:parameters {:body {:name s/Str}}
               :consumes   "application/json"
               :response   (fn [ctx]
                             (if (free-spot? @world)
                               (let [name         (get-in ctx [:parameters :body :name])
                                     updated-world (tank/subscribe-tank @world name)]
                                 (reset! world updated-world))
                               (-> ctx :response (assoc :status 403))))}}}))

(defn start-game-resource []
  (yada/resource
   {:methods {:post
              {:response (fn [_]
                           (let [updated-world (core/start-game @world)]
                             (reset! world updated-world)))}}}))

(defn tank-resource-inputs-valid? [world tankid command]
  (let [tankid-valid?  (tank/valid-tankid?   world tankid)
        command-valid? (tank/valid-tank-cmd? command)]
    (and tankid-valid? command-valid?)))

(defn cmd-tank-resource []
  (yada/resource
   {:methods {:post
              {:parameters {:body {:tankid  s/Num
                                   :command s/Str}}
               :consumes   "application/json"
               :response   (fn [ctx]
                             (let [tankid        (get-in ctx [:parameters :body :tankid])
                                   command       (get-in ctx [:parameters :body :command])]
                               (if (tank-resource-inputs-valid? @world tankid command)
                                 (let [new-world (tank/update-tank @world tankid command)]
                                   (reset! world new-world))
                                 (->
                                   ctx
                                   :response
                                   (assoc :status 400)))))}}}))

(defn validate-world-resource []
  (yada/resource
   {:methods {:post
              {:parameters {:body {:world s/Str}}
               :consumes   "application/json"
               :produces   #{"application/json" "application/edn"}
               :response   (fn [ctx]
                             (let [world-to-validate (get-in ctx [:parameters :body :world])]
                               (core/validate world-to-validate)))}}}))

(defn routes []
  ["/"
   {
    "world"     (world-resource)
    "subscribe" (subscribe-tank-resource)
    "reset"     (reset-world-resource)
    "start"     (start-game-resource)
    "tank"      (cmd-tank-resource)
    "update"    (update-world-resource)
    "validate"  (validate-world-resource)}])

(defn run []
  (let [listener (yada/listener (routes) {:port 3000})
        done     (promise)]
    (reset! server (fn []
                     ((:close listener))
                     (deliver done :done)))
    done))

(defn -main [& args]
  (let [done (run)]
    (println "server running on port 3000... GET \"http://localhost:3000/die\" to kill")
    @done))


;;;;;;;;;;;;;;;;;
;; DEV HELPERS ;;
;;;;;;;;;;;;;;;;;

(comment

"to run in a repl, eval this:"
(def server-promise (run))

"then either wait on the promise:"
@server-promise

"or with a timeout"
(deref server-promise 1000 :timeout)

"or close it yourself"
(@server)

)
