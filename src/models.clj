(ns models
  (:require [clojure.spec.alpha :as s]))

(s/def :return/unit any?)

(s/def :aggregate/datapoint string?)
(s/def :aggregate/items pos-int?)

(s/def :aggregate/aggregate (s/keys :req-un [:aggregate/datapoint
                                             :aggregate/items]))
