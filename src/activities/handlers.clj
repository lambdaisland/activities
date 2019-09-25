(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint])
  (:import [java.util UUID]))

(defonce activities (atom {}))

(defn new-activity-id []
  (str (UUID/randomUUID)))

(defn debug-request [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (with-out-str (clojure.pprint/pprint req))})

;; GET /activity/new
(defn new-activity-form [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str (hiccup/html [:div
                            [:form {:method "POST" :action "/activity"}
                             [:div
                              [:label]
                              [:input {:name "title"}]]
                             [:div
                              [:input {:type "submit"}]]]]))})

;; POST /activity
(defn create-activity [{{:strs [title]} :form-params}]
  (let [id (new-activity-id)]
    ;; store activity in atom and assign it an id
    (swap! activities #(assoc % id {:id    id
                                    :title title}))
    ;; redirect to /activity/<id>
    {:status 301
     :headers {"Location" (str "/activity/" id)}}))


;; GET /activity/:id
(defn get-activity [{{id :id} :path-params}]
  ;; render the title with hiccup
  (let [{{:keys [title]} id} @activities]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str (hiccup/html [:div
                              [:h1 title]]))}))

