(ns db
  (:require [amazonica.aws.dynamodbv2 :as dynamo]
            [clojure.spec.alpha :as s]
            [models])
  (:refer-clojure :exclude [get]))

(def table-name "clj-prod-jmglov-aggregates")

(s/fdef get
        :args (s/cat :datapoint :aggregate/datapoint)
        :ret :aggregate/aggregate)

(defn get [datapoint]
  (dynamo/get-item :table-name table-name
                   :key {:datapoint {:s datapoint}}))

(s/fdef put!
        :args (s/cat :aggregate :aggregate/aggregate)
        :ret any?)

(defn put! [aggregate]
  (dynamo/put-item :table-name table-name
                   :item aggregate))
