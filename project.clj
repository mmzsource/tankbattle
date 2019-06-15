(defproject tankbattle "0.1.0-SNAPSHOT"
  :description "Tank Battle game. Control your tank via a REST API"
  :url "http://example.com/FIXME"
  :license {:name "see licencefile"
            :url "FIXME"}
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :main ^:skip-aot tankbattle.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
