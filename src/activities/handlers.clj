(ns activities.handlers
  (:require [hiccup.core :as hiccup1]
            [hiccup2.core :as hiccup]))



;; GET /activity/new
(defn new-activity-form [req]
  {:status 200
   :body (str (hiccup/html [:div
                            [:form {:method "POST" :action "/activity"}
                             [:div
                              [:label]
                              [:input {:name "title"}]]
                             [:div
                              [:input {:type "submit"}]]]]))
   })

;; POST /activity
(defn create-activity [req]
  {:status 200
   :body "ok"}
  )

;; GET /activity/:id
(defn get-activity [req]
  )
