(ns jvw.laser)

(defn make [moment-created segments]
  {:moment-created moment-created :segments segments :lifetime 3000})
