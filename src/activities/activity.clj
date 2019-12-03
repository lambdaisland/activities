(ns activities.activity
  (:require [clojure.spec.alpha :as s]
            [crux.api :as crux]
            [activities.utils :as utils])
  (:import [java.util UUID]))

(s/def ::title string?)
(s/def ::description string?)
(s/def ::date-time inst?)
(s/def ::duration int?)
(s/def ::capacity int?)
(s/def ::creator uuid?)
(s/def ::participants
  (s/coll-of uuid? :kind set? :distinct true :min-count 0 :into #{}))

(s/def :activities/activity
  (s/keys :req [:crux.db/id ::title ::date-time ::creator ::duration ::capacity]
          :opts [::description ::participants]))

;; TODO fix namespaces?
(defn req->new-activity
  "Takes a request with form input for a new activity and returns a properly
  namespaced activity map."
  [{{:keys [title description datetime duration capacity]} :params
    {:keys [identity]} :session}]
  (let [new-uuid (UUID/randomUUID)
        datetime (utils/datetime->inst datetime)
        duration (Long/parseLong duration)
        capacity (Long/parseLong capacity)
        activity {:crux.db/id            new-uuid
                  :activity/creator      identity
                  :activity/title        title
                  :activity/description  description
                  :activity/date-time    datetime
                  :activity/duration     duration
                  :activity/capacity     capacity
                  :activity/participants #{}}]
    activity))                          ;TODO validate with spec

(defn req->activity
  "Takes a request containing an activity id in the path and returns the
  associated entity in the database."
  [req]
  (let [db       (crux/db (:crux req))
        id       (get-in req [:path-params :id])
        uuid     (UUID/fromString id)
        activity (crux/entity db uuid)]
    activity))                          ;TODO validate with spec
