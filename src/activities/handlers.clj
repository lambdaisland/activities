(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint])
  (:import [java.util UUID]))

;; requsest -> response

;; {:path "/foo" :request-method :get :path-params {:id 123}}
;; ->
;; {:status 200, :headers {}, :body "..."}

(def activities (atom {}))

#_
{123 {:id 123
      :title "Go bouldering"}}

(defn new-activity-id []
  (str (UUID/randomUUID)))

;; (new-activity-id)
;; => "a0752e33-2e8d-42a3-b258-a06ca20eeda7"

(defn debug-request [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (with-out-str (clojure.pprint/pprint req))})

;; GET /activity/new
(defn new-activity-form [req]
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
(defn create-activity [req]
  (let [params (:form-params req)]
    #_{"title" "Go bouldering"}
    ;; - store activity in atom
    ;; - Assign it an id
    ;; - Redirect to /activity/<id>

    {:status 301
     :headers {"Location" "..."}
     :body ""})
  #_(debug-request req)
  )

;; GET /activity/:id
(defn get-activity [req]
  ;; render the title with hiccup
  )
