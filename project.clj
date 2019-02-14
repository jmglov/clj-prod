(defproject clj-prod "0.1.0-SNAPSHOT"
  :description "Clojure in Production"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [amazonica "0.3.139"]
                 [cheshire "5.8.1"]]
  :profiles {:dev {:source-paths ["dev"]})
