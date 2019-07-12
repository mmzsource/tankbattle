(defproject tankbattle "0.1.0-SNAPSHOT"
  :description "Tank Battle game. Control your tank via a REST API"
  :url "http://example.com/FIXME"
  :license {:name "see licencefile"
            :url "FIXME"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [yada                "1.2.16"]
                 [org.slf4j/slf4j-nop "1.7.25"]]
  :main server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :plugins  [[venantius/yagni "0.1.7"]
             [lein-kibit      "0.1.6"]
             [lein-cloverage  "1.1.1"]])
