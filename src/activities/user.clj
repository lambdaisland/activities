(ns activities.user
  (:require [crux.api :as crux]))

(defn req->uuid [req]
  (get-in req [:session :identity]))

;; (defn uuid->activities [db uuid]
;;   (crux/q db '{:find [activities]
;;                :where [[ :activity/participants #{uuid}]]}))
