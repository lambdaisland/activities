(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint]
            [reitit.core]
            [crux.api :as crux])
  (:import [java.util UUID]))

#_
(crux/submit-tx (user/crux)
                [[:crux.tx/put
                  {:crux.db/id #uuid "f190baa5-5c9d-4506-b7bc-901ef175dcf0"
                   :activity/title "Activity 01"}]
                 [:crux.tx/put
                  {:crux.db/id #uuid "9d70e4e9-4863-4457-8523-79d3f14c8454"
                   :activity/title "Activity 02"}]])

(defn debug-request [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (with-out-str (clojure.pprint/pprint req))})

(defn path [req route & [params]]
  (-> req
      :reitit.core/router
      (reitit.core/match-by-name route params)
      :path))

;; GET /
(defn redirect-to-activities [req]
  {:status 301
   :headers {"Location" (path req :activities.system/activities)}})

(defn layout
  "Mounts a page template given a title and a hiccup body."
  [title body]
  [:html
   [:head
    [:title title]
    [:link {:rel "stylesheet" :href "/styles.css"}]]
   [:body body]])

(defn response
  "Returns a ring response with an HTML body. Supports changing the status code,
  replacing or adding headers and setting the title."
  ([body]
   (response {} body))
  ([opts body]
   (let [{:keys [status headers title]
          :or {status 200
               headers {"Content-Type" "text/html"}
               title "Activities"}} opts]
     {:status status
      :headers (if (contains? headers "Content-Type")
                 headers
                 (assoc headers "Content-Type" "text/html"))
      :body (-> (layout title body)
                hiccup/html
                str)})))

;; GET /activity/new
(defn new-activity-form [_]
  (response [:div
             [:form {:method "POST" :action "/activities"}
              [:div
               [:label {:for "title"} "Title: "]
               [:input {:id "title" :name "title" :type "text"}]]
              [:div
               [:label {:for "desc"} "Description: "]
               [:textarea {:id "desc" :name "description" :type "msg"}]]
              [:div
               [:input {:type "submit"}]]]]))

;; POST /activity
(defn create-activity [{{:strs [title description]} :form-params
                        crux :crux}]
  (let [uuid (UUID/randomUUID)]
    ;; store activity in the database and assign it an id
    (crux/submit-tx crux [[:crux.tx/put
                           {:crux.db/id uuid
                            :activity/title title
                            :activity/description description}]])
    ;; redirect to /activity/<id>
    {:status 303
     :headers {"Location" (str "/activity/" uuid)}}))

;; GET /activity/:id
(defn get-activity [req]
  (let [id          (get-in req [:path-params :id])
        db          (crux/db (:crux req))
        activity    (crux/entity db id)
        title       (:activity/title activity)
        description (:activity/description activity)]
    (response [:div
               [:h1 title]
               [:p description]
               [:a {:href (path req :activities.system/edit-activity {:id id})}
                [:button "EDIT"]]
               [:form {:method "POST" :action (path req :activities.system/activity {:id id})}
                [:input {:type "hidden" :name "_method" :value "delete"}]
                [:input {:type "submit" :value "Delete"}]]])))

(defn get-activities [db]
  (map
   #(crux/entity db (first %))
   (crux/q db '{:find [id]
                :where [[id :activity/title]]})))

;; GET /activities
(defn list-activities [req]
  (let [activities (get-activities (crux/db (:crux req)))]
    (response [:div
               (map (fn [{id :crux.db/id
                          title :activity/title}]
                      [:article
                       [:h1
                        [:a {:href (path req :activities.system/activity {:id id})} title]]])
                    activities)])))

;; POST /activity/:id
(defn update-activity [req]
  (let [id              (get-in req [:path-params :id])
        uuid            (UUID/fromString id)
        new-title       (get-in req [:params :title])
        new-description (get-in req [:params :description])
        db              (:crux req)]
    (crux/submit-tx db [[:crux.tx/put
                         {:crux.db/id           uuid
                          :activity/title       new-title
                          :activity/description new-description}]])
    {:status  303
     :headers {"Location" (str "/activity/" id)}}))

;; GET /activity/:id/edit
(defn edit-activity
  "Returns a page with a form to edit an existing activity."
  [req]
  (let [id          (get-in req [:path-params :id])
        db          (crux/db (:crux req))
        activity    (crux/entity db id)
        title       (:activity/title activity)
        description (:activity/description activity)]
    (response [:div
               [:form
                {:method "POST"
                 :action (path req :activities.system/activity {:id id})}
                [:div
                 [:label {:for "title"} "Title: "]
                 [:input {:id "title" :name "title" :type "text" :value title}]]
                [:div
                 [:label {:for "description"} "Description: "]
                 [:textarea {:id "description" :name "description" :type "msg"}
                  :value description]]
                [:div
                 [:input {:type "submit"}]]]])))

;; DELETE /activity/:id
(defn delete-activity [req]
  (let [id   (get-in req [:path-params :id])
        uuid (UUID/fromString id)
        db   (:crux req)]
    (crux/submit-tx db [[:crux.tx/delete uuid]])
    (response [:div
               [:p "Activity successfully deleted."]])))
