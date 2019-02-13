(ns db
  (:require [amazonica.aws.dynamodbv2 :as dynamo])
  (:refer-clojure :exclude [get]))

(def table-name "clj-prod-jmglov-aggregates")

(defn get [datapoint]
  (dynamo/get-item :table-name table-name
                   :key {:datapoint {:s datapoint}}))

(defn put [aggregate]
  (dynamo/put-item :table-name table-name
                   :item aggregate))
