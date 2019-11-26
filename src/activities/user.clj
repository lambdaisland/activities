(ns activities.user)

(defn req->id [req]
  (get-in req [:session :identity]))
