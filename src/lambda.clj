(ns lambda
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [uswitch.lambada.core :refer [deflambdafn]]))

(deflambdafn com.klarna.cljprod.ProcessEvents [in out ctx]
  (let [events (json/parse-stream (io/reader in) true)]
    (println events)))
