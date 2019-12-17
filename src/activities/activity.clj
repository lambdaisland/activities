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

(defn new-activity
  "Takes a map of activity attributes and returns a properly namespaced and
  conformed activity map with some defaults filled in."
  [{:keys [uuid creator title description datetime duration capacity]}]
  (let [inst     (utils/datetime->inst datetime) ;There is a NullPointerException coming from this line
        duration (Long/parseLong duration)
        capacity (Long/parseLong capacity)]
    (s/conform :activities/activity {:crux.db/id    uuid
                                     ::creator      creator
                                     ::title        title
                                     ::description  description
                                     ::date-time    inst
                                     ::duration     duration
                                     ::capacity     capacity
                                     ::participants #{}})))

(defn find-activities
  "Given a database, returns the set of currently available activity maps."
  [db]
  (let [ids (crux/q db {:find  '[id]
                        :where [['id ::title]]})]
    (map #(crux/entity db (first %)) ids)))

;; Is it a nice trick to define just the queries here?
(def activities-query '{:find '[id]
                        :where [['id ::title]]})

;; TODO Can I query the db to get all the entities of a certain type in one
;; sweep? As Arne mentioned, reading the database at every pass is far from
;; ideal, but expanding on this thing below doesn't feel right to me.
;; (crux/q db '{:find [id title description creator date-time capacity
;;                     participants]
;;              :where [[_ :crux.db/id id]
;;                      [id :activities.activity/title title]
;;                      [id :activities.activity/description description]
;;                      [id :activities.activity/capacity capacity]
;;                      [id :activities.activity/]]})

(comment
  (activities.activity/new-activity
   {:uuid         #uuid "06817b67-e31c-4c5e-98fc-ee91be58694b"
    :title        "teste"
    :creator      #uuid "dab570cc-2027-4dd5-be95-7f669faed9bd"
    :description  "hetuheu"
    :capacity     "3"
    :duration     "30"
    :participants #{}
    :datetime    "2019-10-11T09:00"}))
