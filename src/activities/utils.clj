(ns activities.utils
  (:require [reitit.core :as reitit]
            [java-time :as time]))

(defn path [router route & [params]]
  (:path (reitit/match-by-name router route params)))

(defn datetime->inst [datetime-local]
  (-> (time/formatter "yyyy-MM-dd'T'HH:mm")
      (time/local-date-time datetime-local)
      (time/zoned-date-time (time/zone-id "UTC"))
      (time/instant)))

(defn inst->zoned-local-date-time [inst]
  (time/local-date-time inst (time/zone-id "UTC")))
