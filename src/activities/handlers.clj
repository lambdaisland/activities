(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint]
            [reitit.core]
            [crux.api :as crux]
            [activities.flexmark :as flexmark]
            [java-time :as time]
            [activities.render :refer [flash-message]]
            [activities.views :as views]
            [buddy.hashers])
  (:import [java.util UUID]))

#_
(crux/submit-tx (user/crux)
                [[:crux.tx/put
                  {:crux.db/id #uuid "f190baa5-5c9d-4506-b7bc-901ef175dcf0"
                   :activity/title "Activity 01"
                   :activity/description "Description 01"
                   :activity/date-time (time/local-date-time "2019-11-11T09:00")
                   :activity/duration (time/duration (time/minutes 30))
                   :activity/capacity 8
                   :activity/creator nil
                   :activity/participants #{}}]
                 [:crux.tx/put
                  {:crux.db/id #uuid "9d70e4e9-4863-4457-8523-79d3f14c8454"
                   :activity/title "Activity 02"
                   :activity/description "Description 02"
                   :activity/date-time (time/local-date-time "2019-12-25T21:00")
                   :activity/duration (time/duration (time/minutes 120))
                   :activity/capacity 7
                   :activity/creator nil
                   :activity/participants #{}}]])

(defn debug-request [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (with-out-str (clojure.pprint/pprint req))})

(defn path [req route & [params]]
  (let [router (:reitit.core/router req)]
    (:path (reitit.core/match-by-name router route params))))

;; GET /
(defn redirect-to-activities [req]
  {:status 301
   :headers {"Location" (path req :activities.system/activities)}})

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
      :body (-> (views/layout title body)
                hiccup/html
                str)})))

;; GET /activity/new
(defn new-activity-form [_]
  (response [:div
             [:form {:method "POST" :action "/activities"}
              [:header
               [:h2 "New Activity Proposal"]
               [:p "So you have an awesome idea for an activity? Publish it to
                    the community so that others can join you :3"]]
              [:div
               [:label {:for "title"} "Title: "]
               [:div
                [:input {:id "title" :name "title" :type "text"}]]]
              [:div
               [:label {:for "desc"} "Description: "]
               [:div
                [:textarea {:id "desc" :name "description" :type "msg"}]]]
              [:div
               [:label {:for "datetime"} "Date-time: "]
               [:div
                [:input {:id "datetime"
                         :type "datetime-local"
                         :name "datetime"}]]]
              [:div
               [:label {:for "duration"} "And a duration (in minutes): "]
               [:div
                [:input {:id "duration"
                         :type "number"
                         :name "duration"}]]]
              [:div
               [:label {:for "capacity"} "Max. number of participants: "]
               [:input {:id "capacity"
                        :type "number"
                        :name "capacity"
                        :min 1 :max 10}]]
              [:div
               [:div
                [:input {:type "submit" :value "Submit Activity"}]]]]]))

;; POST /activity
(defn create-activity
  [{{:strs [title description datetime duration capacity]} :form-params
    crux :crux
    :as req}]
  (let [uuid (UUID/randomUUID)
        date-time (time/local-date-time datetime)
        duration (time/duration (time/minutes (Long/parseLong duration)))
        capacity (Long/parseLong capacity)
        activity {:crux.db/id uuid
                  :activity/title title
                  :activity/description description
                  :activity/date-time date-time
                  :activity/duration duration
                  :activity/capacity capacity}]
    ;; store activity in the database and assign it an id
    (if (s/valid? :activities/activity activity)
      (crux/submit-tx crux [[:crux.tx/put activity]])
      {:status 400 :body (s/explain-str ...)})
    ;; redirect to /activity/<id>
    {:status 303
     :headers
     {"Location" (path req :activities.system/activity {:id (str uuid)})}}))

;; GET /activity/:id
(defn get-activity [req]
  (let [id          (get-in req [:path-params :id])
        db          (crux/db (:crux req))
        activity    (crux/entity db id)
        title       (:activity/title activity)
        description (:activity/description activity)
        date        (time/format "dd/MM/yyyy" (:activity/date-time activity))
        time        (time/format "hh:mm" (:activity/date-time activity))
        duration    (time/as (:activity/duration activity) :minutes)
        capacity    (:activity/capacity activity)]
    (response [:div
               [:h1 title]
               [:div
                (hiccup/raw (flexmark/md->html description))]
               [:div
                [:p (str "On: " date)]
                [:p (str "At: " time)]
                [:p (str "For: " duration "m")]]
               [:div
                [:p (str "Capacity: " capacity)]]
               [:a {:href (path req :activities.system/edit-activity {:id id})}
                [:button "EDIT"]]
               [:form {:method "POST"
                       :action (path req :activities.system/activity {:id id})}
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
    (response {:title "Activities"}
              [:div
               (map (fn [{id :crux.db/id title :activity/title}]
                      [:article
                       [:h1
                        [:a
                         {:href (path req :activities.system/activity {:id id})}
                         title]]])
                    activities)])))

;; POST /activity/:id
(defn update-activity [req]
  (let [id              (get-in req [:path-params :id])
        uuid            (UUID/fromString id)
        new-title       (get-in req [:params :title])
        new-description (get-in req [:params :description])
        new-date        (-> req
                            (get-in [:params :datetime])
                            time/local-date-time)
        new-duration    (-> req
                            (get-in [:params :duration])
                            Long/parseLong
                            time/minutes
                            time/duration)
        new-capacity    (-> req
                            (get-in [:params :capacity])
                            Long/parseLong)
        db              (:crux req)]
    (crux/submit-tx db [[:crux.tx/put
                         {:crux.db/id           uuid
                          :activity/title       new-title
                          :activity/description new-description
                          :activity/date-time   new-date
                          :activity/duration    new-duration
                          :activity/capacity    new-capacity}]])
    {:status  303
     :headers {"Location" (path req :activities.system/activity {:id id})}}))

;; GET /activity/:id/edit
(defn edit-activity
  "Returns a page with a form to edit an existing activity."
  [req]
  (let [id          (get-in req [:path-params :id])
        db          (crux/db (:crux req))
        activity    (crux/entity db id)
        title       (:activity/title activity)
        description (:activity/description activity)
        datetime    (-> activity
                        :activity/date-time
                        (time/truncate-to :minutes)
                        str)
        duration    (time/as (:activity/duration activity) :minutes)
        capacity    (:activity/capacity activity)]
    (response [:div
               [:form
                {:method "POST"
                 :action (path req :activities.system/activity {:id id})}
                [:div
                 [:label {:for "title"} "Title: "]
                 [:div
                  [:input {:id "title"
                           :name "title"
                           :type "text"
                           :value title}]]]
                [:div
                 [:label {:for "description"} "Description: "]
                 [:div
                  [:textarea {:id "description"
                              :name "description"
                              :type "msg"}
                   :value description]]]
                [:div
                 [:label {:for "datetime"} "Date-time: "]
                 [:div
                  [:input {:id "datetime"
                           :type "datetime-local"
                           :name "datetime"
                           :value datetime}]]]
                [:div
                 [:label {:for "duration"} "Duration: "]
                 [:div
                  [:input {:id "duration"
                           :type "number"
                           :name "duration"
                           :value (str duration)}]]]
                [:div
                 [:label {:for "capacity"} "Capacity: "]
                 [:div
                  [:input {:id "capacity"
                           :type "number"
                           :name "capacity"
                           :value (str capacity)}]]]
                [:div
                 [:div
                  [:input {:type "submit"}]]]]])))

;; DELETE /activity/:id
(defn delete-activity [req]
  (let [id   (get-in req [:path-params :id])
        uuid (UUID/fromString id)
        db   (:crux req)]
    (crux/submit-tx db [[:crux.tx/delete uuid]])
    (response [:div
               [:p "Activity successfully deleted."]])))

;; GET /login
(defn login-form [req]
  (response {:title "Login"} (views/login req)))

(defn get-user-id [db email]
  (not-empty (crux/q (crux/db db) {:find '[uuid]
                                   :where [['uuid ':user/email email]]})))

;; POST /login
(defn login-submission [req]
  (let [email    (get-in req [:params :email])
        password (get-in req [:params :password])
        db       (:crux req)]
    (if-let [uuid (ffirst (get-user-id db email))]
      (let [user (crux/entity (crux/db db) uuid)]
        (if (buddy.hashers/check password (:user/password user))
          (let [next-session (-> (assoc (:session req) :identity uuid)
                                 (with-meta {:recreate true}))]
            (-> (redirect-to-activities req)
                (assoc :session next-session)
                (flash-message "Login successful, welcome back!")))
          (response {:title "Login"}
                    (views/login req "Wrong password."))))
      (response {:title "Login"}
                (views/login req "Email not found. Please register")))))

;; GET /register
(defn register-form [_]
  (response (views/register)))

;; POST /register
(defn register-submission [req]
  (let [name        (get-in req [:params :name])
        email       (get-in req [:params :email])
        password    (get-in req [:params :password])
        [pwd1 pwd2] password
        db          (:crux req)]
    (if (get-user-id db email)
      (response {:title "Register"}
                (views/register name email "Email already registered."))
      (if (= pwd1 pwd2)
        (let [password-hash (buddy.hashers/derive pwd1)
              uuid          (java.util.UUID/randomUUID)
              next-session  (assoc (:session req) :identity uuid)]
          (crux/submit-tx db [[:crux.tx/put
                               {:crux.db/id    uuid
                                :user/name     name
                                :user/email    email
                                :user/password password-hash}]])
          (-> (redirect-to-activities req)
              (assoc :session next-session)
              (flash-message "Registration successful. Welcome on board!")))
        (response {:title "Register"}
                  (views/register name email "Passwords don't match"))))))

;; GET /logout
(defn logout [req]
  (-> (redirect-to-activities req)
      (assoc :session nil)))

