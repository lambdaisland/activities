(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint]
            [reitit.core]
            [activities.db :refer [activities]]
            [crux.api :as crux])
  (:import [java.util UUID]))

(def activities
  (crux/start-node
   {:crux.node/topology :crux.standalone/topology
    :crux.node/kv-store "crux.kv.memdb/kv"
    :crux.standalone/event-log-dir "data/eventlog-1"
    :crux.kv/db-dir "data/db-dir-1"
    :crux.standalone/event-log-kv-store "crux.kv.memdb/kv"}))

(crux/submit-tx activities
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
(defn create-activity [{{:strs [title]} :form-params}]
  (let [uuid (UUID/randomUUID)]
    ;; store activity in the database and assign it an id
    (crux/submit-tx activities [[:crux.tx/put
                                 {:crux.db/id uuid
                                  :activity/title title}]])
    ;; redirect to /activity/<id>
    {:status 303
     :headers {"Location" (str "/activities/activity/" uuid)}}))

(def get-title
  #(crux/q (crux/db activities)
           '{:find [title]
             :where [[n :crux.db/id i]
                     [n :activity/title title]]
             :args [{i %}]}))

;; GET /activity/:id
(defn get-activity [req]
  ;; render the title with hiccup
  (let [id (get-in req [:path-params :id])
        title (get-in @activities [id :title])]
    (response [:div
               [:h1 title]
               [:a {:href (path req :activities.system/edit-activity {:id id})}
                [:button "EDIT"]]
               [:form {:method "POST" :action (path req :activities.system/activity {:id id})}
                [:input {:type "hidden" :name "_method" :value "delete"}]
                [:input {:type "submit" :value "Delete"}]]])))

;; GET /activities
(defn list-activities [req]
  (response [:div
             (map (fn [[id {:keys [title]}]]
                    [:article
                     [:h1
                      [:a {:href (path req :activities.system/activity {:id id})} title]]])
                  @activities)]))

(defn update-activity [req]
  (let [id        (get-in req [:path-params :id])
        new-title (get-in req [:params :title])]
    (swap! activities #(assoc-in % [id :title] new-title))
    {:status 303
     :headers {"Location" (str "/activities/activity/" id)}}))

;; GET /activities/:id/edit
(defn edit-activity [{{id :id} :path-params :as req}]
  (let [activity (get @activities id)]
    (response [:div
               [:form {:method "POST" :action (path req :activities.system/activity {:id id})}
                [:div
                 [:label]
                 [:input {:name "title" :value (:title activity)}]]
                [:div
                 [:input {:type "submit"}]]]])))

(defn delete-activity [req]
  (let [id (get-in req [:path-params :id])]
    (swap! activities #(dissoc % id))
    (response [:div
               [:p "Activity successfully deleted."]])))

(comment
  (reset! activities {}))
