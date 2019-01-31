(ns db
  (:require [amazonica.aws.dynamodbv2 :as dynamo])
  (:refer-clojure :exclude [get]))

(defonce table-name (atom "clj-prod-jmglov3-datapoints"))

(defn get [datapoint]
  (dynamo/get-item :table-name @table-name
                   :key {:datapoint {:s datapoint}}))
