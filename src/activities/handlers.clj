(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint]
            [reitit.core])
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
                            [:form {:method "POST" :action "/activities"}
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
    {:status 303
     :headers {"Location" (str "/activities/activity/" id)}}))


;; GET /activity/:id
(defn get-activity [req]
  ;; render the title with hiccup
  (let [id (get-in req [:path-params :id])
        title (get-in @activities [id :title])]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str (hiccup/html [:div
                              [:h1 title]]))}))

(defn path [route & [params]]
  (-> (:router integrant.repl.state/system)
      (reitit.core/match-by-name route params)
      :path))

;; GET /activities
(defn list-activities [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str (hiccup/html
               [:div
                (map (fn [[id {:keys [title]}]]
                       [:article [:h1 [:a {:href (path :activities.system/activity {:id id})} title]]]) @activities)]))})

(defn edit-activity [req]
  (let [id        (get-in req [:path-params :id])
        new-title (get-in req [:form-params :title])]
    (swap! activities #(update % id {:id    id
                                     :title new-title}))
    {:status 303
     :header {"Location" (str "/activities/activity" id)}}))

;; GET /activities/:id/edit
(defn edit-activity-form [{{id :id} :path-params}]
  (let [activity (get @activities id)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str (hiccup/html [:div
                              [:form {:method "PUT" :action "/activities"}
                               [:div
                                [:label]
                                [:input {:name "title" :value (:title activity)}]]
                               [:div
                                [:input {:type "submit"}]]]]))}))

(defn delete-activity [req]
  (let [id (get-in req [:path-params :id])]
    (swap! activities #(dissoc % id))
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str (hiccup/html
                 [:div
                  [:p "Activity successfully deleted."]]))}))

(comment
  (reset! activities {}))
