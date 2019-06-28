(ns server.core
  (:require
    [yada.yada :as yada]
    [schema.core :as s]
    [tankbattle.core :as tb]))

; simple atom for exposing a global function so the server can close itself
(def server (atom nil))

(def new-world {:count 0})

(def world (atom {:count 0}))

(defn world-resource
  []
  "Modifiable world. Look at yada.resources.atom-resource for ways to add
  last-modified headers and the like"
  (let [state world]
    (yada/resource
      {:methods {:get  {:produces "application/json"
                        :response (fn [ctx] @state)}}})))

(defn update-world-resource
  []
  "Modifiable world. Look at yada.resources.atom-resource for ways to add
  last-modified headers and the like"
  (let [state world]
    (yada/resource
     {:methods {:post
                {:response (fn [ctx]
                             (swap! state update-in [:count] inc))}}})))

(defn reset-world-resource
  []
  "Modifiable world. Look at yada.resources.atom-resource for ways to add
  last-modified headers and the like"
  (let [state world]
    (yada/resource
     {:methods {:post
                {:response (fn [ctx]
                             (reset! world new-world))}}})))

(defn routes []
  ["/"
   {
    "world"  (world-resource)
    "update" (update-world-resource)
    "reset"  (reset-world-resource)
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
