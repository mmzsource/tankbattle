(ns server.core
  (:require
    [yada.yada :as yada]
    [schema.core :as s]
    [tankbattle.core :as tb]))

; simple atom for exposing a global function so the server can close itself
(def server (atom nil))

(def new-world (tb/init-world 32 18))

(def world (atom (tb/init-world 32 18)))

(defn world-resource []
  (yada/resource
   {:methods {:get  {:produces "application/json"
                     :response (fn [ctx] @world)}}}))

(defn update-world-resource []
  (yada/resource
   {:methods {:post
              {:response (fn [ctx]
                           (swap! world assoc :last-update (System/currentTimeMillis)))}}}))

(defn reset-world-resource []
  (yada/resource
   {:methods {:post
              {:response (fn [ctx]
                           (reset! world new-world))}}}))

(defn subscribe-tank-resource []
  (yada/resource
   {:methods {:post
              {:parameters {:body {:name s/Str}}
               :consumes   "application/json"
               :response   (fn [ctx]
                             (let [body (get-in ctx [:parameters :body])]
                               (swap! world assoc :name (body :name))
                               (swap! world assoc :last-update (System/currentTimeMillis))))}}}))

(defn routes []
  ["/"
   {
    "world"     (world-resource)
    "reset"     (reset-world-resource)
    "update"    (update-world-resource)

    "subscribe" (subscribe-tank-resource)

    "die"     (yada/as-resource (fn []
                                  (future (Thread/sleep 100) (@server))
                                  "shutting down in 100ms..."))}])

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
