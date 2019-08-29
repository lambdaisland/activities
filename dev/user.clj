(ns user
  (:require [integrant.repl :as ig-repl]
            [activities.system :as system]))

(ig-repl/set-prep! system/read-config)

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
