(defproject clj-prod "0.1.0-SNAPSHOT"
  :description "Clojure in Production"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [amazonica "0.3.139" :exclusions [com.amazonaws/aws-java-sdk
                                                   com.amazonaws/amazon-kinesis-client
                                                   com.amazonaws/dynamodb-streams-kinesis-adapter]]
                 [cheshire "5.8.1"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.475"]
                 [uswitch/lambada "0.1.2"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[com.amazonaws/aws-java-sdk "1.11.475"]]}
             :uberjar {:aot :all}}
  :uberjar-name "clj-prod.jar")
