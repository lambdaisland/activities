(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint]
            [reitit.core]
            [crux.api :as crux]
            [activities.flexmark :as flexmark]
            [activities.user :as user]
            [java-time :as time]
            [activities.render :refer [flash-message]]
            [activities.views :as views]
            [buddy.hashers]
            [clojure.spec.alpha :as s])
            [activities.utils :refer [path]])
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

;; GET /
(defn redirect-to-activities [req]
  {:status 301
   :headers {"Location" (path req :activities.system/activities)}})

(defn response
  "Returns a ring response with an HTML body. Supports changing the status code,
  replacing or adding headers and setting the title."
  ([req body]
   (response req {} body))
  ([req opts body]
   (let [{:keys [status headers title]
          :or {status 200
               headers {"Content-Type" "text/html"}
               title "Activities"}} opts
         user-id (user/req->id req)
         username (when user-id
                    (-> req :crux crux/db (crux/entity user-id) :user/name))]
     {:status status
      :headers (if (contains? headers "Content-Type")
                 headers
                 (assoc headers "Content-Type" "text/html"))
      :body (-> (views/layout title username body)
                hiccup/html
                str)})))

;; GET /activity/new
(defn new-activity-form [req]
  (response req [:div
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

;; (def test-activity
;;   {:crux.db/id            #uuid "867f7291-ea56-4102-9053-8e52d2570504"
;;    :activity/title        "bru"
;;    :activity/description  "Curabitur lacinia pulvinar nibh."
;;    :activity/date-time    #inst "2019-11-25T09:20:21.986435"
;;    :activity/duration     0
;;    :activity/capacity     0
;;    :activity/creator      #uuid "ca21035b-d15f-4fc5-a132-def7da608953"
;;    :activity/participants #{#uuid "14fa320b-dcee-4ed5-a171-46ac148fdba1" #uuid "21e5c0ae-5b1c-4093-b908-05dcf38a79d8"}})

;; (s/def :crux.db/id uuid?)
(s/def :activity/title string?)
(s/def :activity/description string?)
(s/def :activity/date-time any?)
(s/def :activity/duration any?)
(s/def :activity/capacity int?)
;; (s/def ::creator uuid?)
(s/def :activity/participants (s/coll-of uuid? :kind set? :distinct true :min-count 0 :into #{}))

(s/def :activities/activity (s/keys :req [:crux.db/id :activity/title :activity/date-time :activity/duration :activity/capacity]
                                    :opts [:activity/description :activity/participants]))

;; (defn create-activity
;;   "Handler. Receives a request with form parameters to generate a new activity,
;;    conforms the input and, if a valid activity, inserts the new activity in the
;;    database and redirects the user to the new activity page. If invalid, returns
;;    a 400 with a Spec explanation string."
;;  [{:keys [form-params crux]
;;    :as   request}
;;   (let [activity (s/conform ::create-activity-form
;;                             (select-keys form-params [,,,]))]
;;     (case params
;;       :clojure.spec/invalid {:status 400
;;                              :body (s/explain-str
;;                                     ::create-activity-form form-params)}
;;       :else (do (crux/submit-tx crux [[:crux.tx/put activity]])
;;                 {:status 303
;;                  :headers {"Location"
;;                            (path request
;;                                  :activities.system/activity
;;                                  {:id (str (java.util.UUID/randomUUID))})}})))])

;; POST /activity
(defn create-activity
  [{{:strs [title description datetime duration capacity]} :form-params
    crux                                                   :crux
    :as                                                    req}]
  (let [uuid      (UUID/randomUUID)
        date-time (time/local-date-time datetime)
        duration  (time/duration (time/minutes (Long/parseLong duration)))
        capacity  (Long/parseLong capacity)
        creator   (user/req->id req)
        activity  {:crux.db/id           uuid
                   :activity/title       title
                   :activity/description description
                   :activity/creator     creator
                   :activity/date-time   date-time
                   :activity/duration    duration
                   :activity/capacity    capacity}]
    (if (s/valid? :activities/activity activity)
      ;; store activity in the database and assign it an id
      (do (crux/submit-tx crux [[:crux.tx/put activity]])
          ;; redirect to /activity/<id>
          {:status 303 :headers {"Location" (path req :activities.system/activity {:id (str uuid)})}})
      {:status 400 :body (s/explain-str :activities/activity activity)})))

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
    (response req [:div
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
    (response req {:title "Activities"}
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
    (response req [:div
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
  (let [id       (get-in req [:path-params :id])
        uuid     (UUID/fromString id)
        node     (:crux req)
        db       (crux/db node)
        creator  (:activity/creator (crux/entity db uuid))
        user-id  (user/req->id req)]
    (if (and creator user-id (= creator user-id))
      (do
        (crux/submit-tx node [[:crux.tx/delete uuid]])
        (response req [:div
                       [:p "Activity successfully deleted."]]))
      (response req {:status 403}
                [:div
                 [:p "Not allowed"]]))))

;; GET /login
(defn login-form [req]
  (response req {:title "Login"} (views/login req)))

(defn get-user-id [node email]
  (not-empty (crux/q (crux/db node) {:find '[uuid]
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
          (response req {:title "Login"}
                    (views/login req "Wrong password."))))
      (response req {:title "Login"}
                (views/login req "Email not found. Please register")))))

;; GET /register
(defn register-form [req]
  (response req (views/register)))

;; POST /register
(defn register-submission [req]
  (let [name        (get-in req [:params :name])
        email       (get-in req [:params :email])
        password    (get-in req [:params :password])
        [pwd1 pwd2] password
        db          (:crux req)]
    (if (get-user-id db email)
      (response req {:title "Register"}
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
        (response req {:title "Register"}
                  (views/register name email "Passwords don't match"))))))

;; GET /logout
(defn logout [req]
  (-> (redirect-to-activities req)
      (assoc :session nil)))

