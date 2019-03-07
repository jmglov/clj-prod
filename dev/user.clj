(ns user
  (:require [amazonica.core :as amazonica]
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
