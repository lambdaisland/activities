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

(defn redirect-to-activities [req]
  {:status 301
   :headers {"Location" (path req :activities.system/activities)}})

(defn layout [body title]
  [:html
   [:head
    [:title title]
    [:link {:rel "stylesheet" :href "/styles.css"}]]
   [:body body]])

(defn response
  ([markup]
   (response {} markup))
  ([opts markup]
   (let [{:keys [status headers title]
          :or {status 200
               headers {"Content-Type" "text/html"}
               title "Activities"}} opts]
     {:status status
      :headers (if (contains? headers "Content-Type")
                 headers
                 (assoc headers "Content-Type" "text/html"))
      :body (-> markup
                (layout title)
                hiccup/html
                str)})))

;; GET /activity/new
(defn new-activity-form [_]
  (response [:div
             [:form {:method "POST" :action "/activities"}
              [:div
               [:label]
               [:input {:name "title"}]]
              [:div
               [:input {:type "submit"}]]]]))

;; POST /activity
(defn create-activity [{{:strs [title]} :form-params
                        crux :crux}]
  (let [uuid (UUID/randomUUID)]
    ;; store activity in the database and assign it an id
    (crux/submit-tx crux [[:crux.tx/put
                           {:crux.db/id uuid
                            :activity/title title}]])
    ;; redirect to /activity/<id>
    {:status 303
     :headers {"Location" (str "/activities/activity/" uuid)}}))

(defn get-title [db id]
  (let [uuid (UUID/fromString id)]
    (ffirst (crux/q db
                    {:find '[title]
                     :where [['n :crux.db/id uuid]
                             '[n :activity/title title]]}))))

;; (get-title (crux/db (user/crux)) "9d70e4e9-4863-4457-8523-79d3f14c8454")

;; GET /activity/:id
(defn get-activity [req]
  ;; render the title with hiccup
  (let [id    (get-in req [:path-params :id])
        db    (crux/db (:crux req))
        title (get-title db id)]
    (response [:div
               [:h1 title]
               [:a {:href (path req :activities.system/edit-activity {:id id})}
                [:button "EDIT"]]
               [:form {:method "POST" :action (path req :activities.system/activity {:id id})}
                [:input {:type "hidden" :name "_method" :value "delete"}]
                [:input {:type "submit" :value "Delete"}]]])))

(defn get-activities [db]
  (map
   #(crux/entity db (first %))
   (user/q '{:find [id]
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

;; TODO: change to use crux
(defn update-activity [req]
  (let [id        (get-in req [:path-params :id])
        new-title (get-in req [:params :title])]
    (swap! activities #(assoc-in % [id :title] new-title))
    {:status 303
     :headers {"Location" (str "/activities/activity/" id)}}))

;; GET /activities/:id/edit
(defn edit-activity [req]
  (let [id    (get-in req [:path-params :id])
        db    (crux/db (:crux req))
        title (get-title db id)]
    (response [:div
               [:form {:method "POST" :action (path req :activities.system/activity {:id id})}
                [:div
                 [:label]
                 [:input {:name "title" :value title}]]
                [:div
                 [:input {:type "submit"}]]]])))

;; TODO: change to use crux
(defn delete-activity [req]
  (let [id (get-in req [:path-params :id])]
    (swap! activities #(dissoc % id))
    (response [:div
               [:p "Activity successfully deleted."]])))

(comment
  (reset! activities {}))
