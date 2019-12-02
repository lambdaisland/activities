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

(defn db []
  (crux/db (crux)))

(defn q [query]
  (crux/q (db) query))

(defn entity [id]
  (crux/entity (db) id))

(defn get-uuids []
  (q '{:find [uuid]
       :where [[uuid :crux.db/id]]}))

(defn get-users-uuids []
  (q '{:find [uuid]
       :where [[uuid :user/email]]}))

(defn get-activities-uuids []
  (q '{:find [uuid]
       :where [[uuid :activity/title]]}))

(defn mount-delete-ops []
  (into [] (map (fn [[uuid]] [:crux.tx/delete uuid]) (get-uuids))))

(defn clear-db []
  (crux/submit-tx (crux) (mount-delete-ops)))

(def last-requests (atom []))
(def last-responses (atom []))

(defn wrap-capture-request-response [handler]
  (fn [req]
    (swap! last-requests conj req)
    (let [res (handler req)]
      (swap! last-responses conj res)
      res)))
