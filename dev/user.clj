(ns user
  (:require [integrant.repl :as ig-repl]
            [activities.system :as system]
            [crux.api :as crux]))

(ig-repl/set-prep! system/read-config)

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)

(defn crux []
  (:crux integrant.repl.state/system))

(defn q [query]
  (crux/q
   (crux/db
    (:crux integrant.repl.state/system))
   query))
