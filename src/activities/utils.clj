(ns activities.utils
  (:require [reitit.core]))

(defn path [req route & [params]]
  (let [router (:reitit.core/router req)]
    (:path (reitit.core/match-by-name router route params))))
