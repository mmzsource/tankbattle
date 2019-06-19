(ns server.core
  (:require
    [yada.yada :as yada]
    [schema.core :as s]))

; simple atom for exposing a global function so the server can close itself
(def server (atom nil))

(defn routes []
  ["/"
   {
    "hello" (yada/as-resource "Hello World!")
    "json"  (yada/as-resource {:message "yo!"})
    "die"   (yada/as-resource (fn []
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
