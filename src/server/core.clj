(ns server.core
  (:require
    [yada.yada        :as yada]
    [schema.core      :as s]
    [jvw.core  :as core]
    [server.game  :as g])
  (:gen-class))

; simple atom for exposing a global function so the server can close itself
(def server (atom nil))

(defn new-game [field] (g/make field))

(def game (atom (new-game (core/field-test))))

(defn world-resource []
  (yada/resource
   {:methods {:get
              {:produces       #{"application/json" "application/edn"}
               :response       (fn [_]
                                 game)}}
    :access-control {:allow-origin "*"
                     :allow-credentials false
                     :expose-headers #{"X-Custom"}
                     :allow-methods #{:get}
                     :allow-headers ["Api-Key"]}}))

(defn routes []
  ["/"
   {
    "world"     (world-resource)}])

(defn run []
  (let [listener (yada/listener (routes) {:port 3000})
        done     (promise)]
    (reset! server (fn [] ((:close listener)) (deliver done :done)))
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
