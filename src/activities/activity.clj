(ns activities.activity
  (:require [clojure.spec.alpha :as s]
            [java-time :as time]))

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

(defn params->activity [{:strs [title description datetime duration capacity]}]
  (let [date-time (time/local-date-time datetime) ;; TODO use inst
        duration  (Long/parseLong duration)
        capacity  (Long/parseLong capacity)]
    {::title       title
     ::description description
     ::date-time   date-time
     ::duration    duration
     ::capacity    capacity}))
