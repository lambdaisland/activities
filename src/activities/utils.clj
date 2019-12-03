(ns activities.utils
  (:require [reitit.core]
            [java-time :as time]))

(defn path [req route & [params]]
  (let [router (:reitit.core/router req)]
    (:path (reitit.core/match-by-name router route params))))

(defn datetime->inst [datetime-local]
  (-> (time/formatter "yyyy-MM-dd'T'HH:mm")
      (time/local-date-time datetime-local)
      (time/zoned-date-time (time/zone-id "UTC"))
      (time/java-date)))
