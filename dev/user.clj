(ns user)

(require 'hashp.core)

(defmacro jit
  "Just in time loading of dependencies."
  [sym]
  `(do
     (requiring-resolve '~sym)))

(defn go []
  ((jit integrant.repl/set-prep!) @(jit activities.system/read-config))
  ((jit integrant.repl/go)))

(defn halt []
  ((jit integrant.repl/halt)))

(defn reset []
  ((jit integrant.repl/reset)))

(defn crux []
  (:crux @(jit integrant.repl.state/system)))

(defn db []
  ((jit crux.api/db) (crux)))

(defn q [query]
  ((jit crux.api/q) (db) query))

(defn entity [id]
  ((jit crux.api/entity) (db) id))

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
  ((jit crux.api/submit-tx) (crux) (mount-delete-ops)))

(def last-requests (atom []))
(def last-responses (atom []))

(defn wrap-capture-request-response [handler]
  (fn [req]
    (swap! last-requests conj req)
    (let [res (handler req)]
      (swap! last-responses conj res)
      res)))
