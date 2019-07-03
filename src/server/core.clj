(ns server.core
  (:require
    [yada.yada :as yada]
    [schema.core :as s]
    [tankbattle.core :as tb])
  (:gen-class))

; simple atom for exposing a global function so the server can close itself
(def server (atom nil))

(def new-world (tb/init-world))

(def world (atom (tb/init-world)))

(defn world-resource []
  (yada/resource
   {:methods {:get
              {:produces #{"application/json" "application/edn"}
               :response (fn [ctx]
                           (let [clean-world (reset! world (tb/cleanup @world))]
                             (assoc
                               (assoc clean-world :time (System/currentTimeMillis))
                               :headers
                               {"Access-Control-Allow-Origin" "*"})))}}}))

(defn update-world-resource []
  (yada/resource
   {:methods {:post
              {:response (fn [ctx]
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
  (> (count (world :av-ids)) 0))

(defn subscribe-tank-resource []
  (yada/resource
   {:methods {:post
              {:parameters {:body {:name s/Str}}
               :consumes   "application/json"
               :response   (fn [ctx]
                             (if (free-spot? @world)
                               (let [name         (get-in ctx [:parameters :body :name])
                                    updated-world (tb/subscribe-tank @world name)]
                                 (reset! world updated-world))
                               (->
                                ctx
                                :response
                                (assoc :status 403))))}}}))

(defn start-game-resource []
  (yada/resource
   {:methods {:post
              {:response (fn [ctx]
                           (let [updated-world (tb/start-game @world)]
                             (reset! world updated-world)))}}}))

(defn tank-resource-inputs-valid? [world tankid command]
  (let [tankid-valid?  (tb/valid-tankid?   world tankid)
        command-valid? (tb/valid-tank-cmd? command)]
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
                                 (let [new-world (tb/update-tank @world tankid command)]
                                   (reset! world new-world))
                                 (->
                                   ctx
                                   :response
                                   (assoc :status 400)))))}}}))

(defn routes []
  ["/"
   {
    "world"     (world-resource)
    "subscribe" (subscribe-tank-resource)
    "reset"     (reset-world-resource)
    "start"     (start-game-resource)
    "tank"      (cmd-tank-resource)
    "update"    (update-world-resource)}])

(defn run []
  (let [listener (yada/listener (routes) {:port 3000})
        done     (promise)]
    (reset! server (fn []
                     ((:close listener))
                     (deliver done :done)))
    done))

(defn -main [& args]
  (let [done (run)]
    (println "server running on port 3000... GET \"http://localhost/die\" to kill")
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
