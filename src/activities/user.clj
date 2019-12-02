(ns activities.user)

(defn req->uuid [req]
  (get-in req [:session :identity]))
