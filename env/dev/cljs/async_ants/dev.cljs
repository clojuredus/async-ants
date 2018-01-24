(ns ^:figwheel-no-load async-ants.dev
  (:require
    [async-ants.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)
