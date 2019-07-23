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

(def game (atom (new-game core/field-test)))

(defn world-resource []
  (yada/resource
   {:methods {:get
              {:produces       #{"application/json" "application/edn"}
               :response       (fn [_]
                                 (@game :field))}}
    :access-control {:allow-origin "*"
                     :allow-credentials false
                     :expose-headers #{"X-Custom"}
                     :allow-methods #{:get}
                     :allow-headers ["Api-Key"]}}))

(defn subscribe-tank-resource []
  (yada/resource
   {:methods {:post
              {:parameters {:body {:name s/Str}}
               :consumes   "application/json"
               :produces   #{"application/edn" "application/json"}
               :response   (fn [ctx]
                             (if (g/joinable? @game)
                               (let [name          (get-in ctx [:parameters :body :name])
                                     [game-new secret]  (g/join @game name)
                                     just-do-it    (reset! game game-new)]
                                 {:secret secret})
                               (-> ctx :response (assoc :status 403))))}}}))

(defn command-resource []
  (yada/resource
   {:methods {:post
              {:parameters {:body {:secret  s/Str
                                   :command s/Str
                                   :direction  s/Str}}
               :consumes   "application/json"
               :produces #{"application/json" "application/edn"}
               :response   (fn [ctx]
                             (let [secret        (get-in ctx [:parameters :body :secret])
                                   command       (get-in ctx [:parameters :body :command])
                                   direction       (get-in ctx [:parameters :body :direction])
                                   [game-new had-effect?] (g/command @game secret :move :east)
                                   just-do-it! (reset! game game-new)]
                               {:secret secret
                                 :command (keyword command)
                                 :direction (keyword direction)}))}}}))

(defn routes []
  ["/"
   {
    "world"     (world-resource)
    "subscribe" (subscribe-tank-resource)
    "tank" (command-resource)}])

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
