(ns user
  (:require [amazonica.core :as amazonica]
            [amazonica.aws.logs :as logs]
            [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream)
           (java.nio ByteBuffer)))

(defn refresh-aws-credentials
  ([]
   (refresh-aws-credentials "clj-prod"))
  ([profile]
   (amazonica/defcredential (amazonica/get-credentials {:profile profile}))))

(defn read-uberjar [filename]
  (with-open [out (ByteArrayOutputStream.)]
    (io/copy (io/input-stream filename) out)
    (ByteBuffer/wrap (.toByteArray out))))

(defn get-latest-log-events [log-group-name]
  (let [get-log-events (fn [log-stream-name]
                         (->> (logs/get-log-events :log-group-name log-group-name
                                                   :log-stream-name log-stream-name)
                              :events
                              (map :messages)))]
    (->> (logs/describe-log-streams :log-group-name log-group-name)
         :log-streams
         (sort-by :last-event-timestamp)
         last
         get-log-events)))
