(ns activities.activity
  (:require [clojure.spec.alpha :as s]
            [java-time :as time]))
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
  [{{:strs [title description datetime duration capacity]} :params
    {:keys [identity]} :session}]
  (let [new-uuid (UUID/randomUUID)
        activity {:crux.db/id            new-uuid
                  :activity/creator      identity
                  :activity/title        title
                  :activity/description  description
                  :activity/date-time    datetime ;TODO parse into instant
                  :activity/duration     duration ;TODO parse into long
                  :activity/capacity     capacity ;TODO parse into long
                  :activity/participants #{}}]
    activity))                          ;TODO validate with spec

