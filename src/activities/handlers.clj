(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint]
            [crux.api :as crux]
            [activities.user :as user]
            [activities.activity :as activity]
            [java-time :as time]
            [activities.render :refer [flash-message]]
            [activities.views :as views]
            [buddy.hashers]
            [activities.utils :refer [path] :as utils]
            [clojure.spec.alpha :as s])
  (:import [java.util UUID]))

(defn submit-tx [crux tx-ops]
  (let [tx-time (:crux.tx/tx-time (crux/submit-tx crux tx-ops))]
    (crux/sync crux tx-time (time/duration 1000))
    nil))

(defn debug-request [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (with-out-str (clojure.pprint/pprint req))})

;; GET /
(defn redirect-to-activities [{:keys [reitit.core/router]}]
  {:status 301
   :headers {"Location" (path router :activities.system/activities)}})

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
         user-id (user/req->uuid req)
         username (when user-id
                    (-> req :crux crux/db (crux/entity user-id) :user/name))]
     {:status status
      :headers (if (contains? headers "Content-Type")
                 headers
                 (assoc headers "Content-Type" "text/html"))
      :body (-> (views/layout req title username body)
                hiccup/html
                str)})))

;; GET /activity/new
(defn new-activity-form [{:keys [reitit.core/router] :as req}]
  (let [user-uuid (user/req->uuid req)
        db        (crux/db (:crux req))]
    (if (crux/entity db user-uuid)
      (response req {:title "New Activity"} (views/activity-form req))
      {:status  302
       :headers {"Location" (path router :activities.system/login-form)}})))

;; POST /activity
(defn create-activity
  [{:keys [crux params session reitit.core/router]}]
  (let [new-uuid   (UUID/randomUUID)
        creator    (:identity session)
        attributes (assoc params :uuid new-uuid :creator creator)
        activity   (activity/new-activity attributes)
        path       (path router :activities.system/activity {:id new-uuid})]
    (if (s/valid? :activities/activity activity)
      (do (submit-tx crux [[:crux.tx/put activity]])
          {:status  303
           :headers {"Location" path}})
      {:status 404
       :body (s/explain-str :activities/activity activity)})))

;; GET /activity/:id
(defn get-activity
  [{:keys [crux path-params session reitit.core/router] :as req}]
  (let [db          (crux/db crux)
        activity-id (:id path-params)
        activity    (crux/entity db activity-id)
        user-uuid   (:identity session)]
    (response req (views/activity-page activity user-uuid router))))

;; GET /activities
(defn list-activities [{:keys [crux reitit.core/router] :as req}]
  (let [db         (crux/db crux)
        activities (activity/list-activities db)
        view       (views/activities activities router)]
    (response req {:title "All Activities"} view)))

;; POST /activity/:id
(defn update-activity
  [{:keys [crux path-params params reitit.core/router]}]
  (let [db                    (crux/db crux)
        activity-id           (:id path-params)
        current-activity      (crux/entity db activity-id)
        {:keys [:crux.db/id
                ::activity/creator]} current-activity
        new-activity          #p (merge params {:uuid id :creator creator})
        updated-activity      (activity/new-activity new-activity)]
    (submit-tx crux [[:crux.tx/put updated-activity]])
    {:status  301
     :headers {"Location" (path router :activities.system/activity {:id activity-id})}}))

;; GET /activity/:id/edit
(defn edit-activity
  "Returns a page with a form to edit an existing activity."
  [{:keys [crux path-params session reitit.core/router] :as req}]
  (let [db          (crux/db crux)
        activity-id (:id path-params)
        activity    (crux/entity db activity-id)
        user-id     (:identity session)]
    (response req (views/edit-activity-form activity user-id router))))

;; DELETE /activity/:id
(defn delete-activity
  [{:keys [path-params crux session] :as req}]
  (let [id       (:id path-params)
        uuid     (UUID/fromString id)
        db       (crux/db crux)
        activity (crux/entity db uuid)
        creator  (::activity/creator activity)
        user-id  (:identity session)]
    (if (and creator user-id (= creator user-id))
      (do
        (submit-tx crux [[:crux.tx/delete uuid]])
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
          (submit-tx db [[:crux.tx/put
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

;; POST /activity/:id/join
(defn join-activity
  "Takes a request, adds the user in session to the list of participants in the
  activity and redirects back to the activity page."
  [{:keys [crux path-params session reitit.core/router]}]
  (let [db           (crux/db crux)
        activity-id  (:id path-params)
        activity     (crux/entity db activity-id)
        participants (get activity ::activity/participants)
        user-uuid    (:identity session)
        new-activity (->> (conj participants user-uuid)
                          (assoc activity ::activity/participants))]
    (submit-tx crux [[:crux.tx/put new-activity]])
    {:status  303
     :headers {"Location" (path router
                                :activities.system/activity
                                {:id activity-id})}}))

;; DELETE /activity/:id/join
(defn leave-activity
  "Takes a request, removes the user in session from the list of participants in
  the activity and redirects back to the activity page."
  [{:keys [crux path-params session reitit.core/router]}]
  (let [
        db           (crux/db crux)
        activity-id  (:id path-params)
        activity     (crux/entity db activity-id)
        activity-id  (get activity :crux.db/id)
        participants (get activity ::activity/participants)
        user-uuid    (:identity session)
        new-activity (->> (disj participants user-uuid)
                          (assoc activity ::activity/participants))]
    (submit-tx crux [[:crux.tx/put new-activity]])
    {:status  303
     :headers {"Location" (path router
                                :activities.system/activity
                                {:id activity-id})}}))

;; (defn get-user [req]
;;   (response req (views/user-page req)))

(declare get-user)
