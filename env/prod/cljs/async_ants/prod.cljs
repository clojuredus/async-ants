(ns async-ants.prod
  (:require
    [async-ants.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
