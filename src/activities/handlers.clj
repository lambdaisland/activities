(ns activities.handlers
  (:require [hiccup2.core :as hiccup]
            [clojure.pprint]
            [reitit.core]
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
(defn new-activity-form [req]
  (let [user-uuid (user/req->uuid req)
        db (crux/db (:crux req))]
    (if (crux/entity db user-uuid)
      (response req {:title "New Activity"} (views/activity-form req))
      {:status 302
       :headers {"Location" (path req :activities.system/login-form)}})))

;; POST /activity
(defn create-activity
  [{:keys [crux params session] :as req}]
  (let [new-uuid   (UUID/randomUUID)
        creator    (:identity session)
        attributes (assoc params :uuid new-uuid :creator creator)
        activity   (activity/new-activity attributes)
        router     (:reitit.core/router req)
        path       (path router :activities.system/activity {:id new-uuid})]
    (if (s/valid? :activities/activity activity)
      (do (submit-tx crux [[:crux.tx/put activity]])
          {:status  303
           :headers {"Location" path}})
      (s/explain :activities/activity activity)))) ;should it throw?

;; GET /activity/:id
(defn get-activity [req]
  (response req (views/activity-page req)))


;; GET /activities
(defn list-activities [req]
  (let [view (views/activities req)]
    (response req {:title "All Activities"} view)))

;; POST /activity/:id
(defn update-activity [req]
  (let [node              (:crux req)
        current-activity  (activity/req->activity req)
        activity-uuid     (:crux.db/id current-activity)
        activity-id       (str activity-uuid)
        new-title         (get-in req [:params :title])
        new-description   (get-in req [:params :description])
        new-datetime      (utils/datetime->inst (get-in req [:params :datetime]))
        new-duration      (Long/parseLong (get-in req [:params :duration]))
        new-capacity      (Long/parseLong (get-in req [:params :capacity]))
        modified-keys-map {:activity/title       new-title
                           :activity/description new-description
                           :activity/date-time   new-datetime
                           :activity/duration    new-duration
                           :activity/capacity    new-capacity}
        updated-activity  (merge current-activity modified-keys-map)]
    (submit-tx node [[:crux.tx/put updated-activity]])
    {:status  301
     :headers {"Location" (path req :activities.system/activity {:id activity-id})}}))

;; GET /activity/:id/edit
(defn edit-activity
  "Returns a page with a form to edit an existing activity."
  [req]
  (response req (views/edit-activity-form req)))

;; DELETE /activity/:id
(defn delete-activity [req]
  (let [id       (get-in req [:path-params :id])
        uuid     (UUID/fromString id)
        node     (:crux req)
        db       (crux/db node)
        creator  (:activity/creator (crux/entity db uuid))
        user-id  (user/req->uuid req)]
    (if (and creator user-id (= creator user-id))
      (do
        (submit-tx node [[:crux.tx/delete uuid]])
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
  [req]
  (let [node         (:crux req)
        activity     (activity/req->activity req)
        activity-id  (get activity :crux.db/id)
        participants (get activity :activity/participants)
        user-uuid    (user/req->uuid req)
        new-activity (->> (conj participants user-uuid)
                          (assoc activity :activity/participants))]
    (submit-tx node [[:crux.tx/put new-activity]])
    {:status  303
     :headers {"Location" (path req
                                :activities.system/activity
                                {:id activity-id})}}))

;; DELETE /activity/:id/join
(defn leave-activity
  "Takes a request, removes the user in session from the list of participants in
  the activity and redirects back to the activity page."
  [req]
  (let [node         (:crux req)
        activity     (activity/req->activity req)
        activity-id  (get activity :crux.db/id)
        participants (get activity :activity/participants)
        user-uuid    (user/req->uuid req)
        new-activity (->> (disj participants user-uuid)
                          (assoc activity :activity/participants))]
    (submit-tx node [[:crux.tx/put new-activity]])
    {:status  303
     :headers {"Location" (path req
                                :activities.system/activity
                                {:id activity-id})}}))

;; (defn get-user [req]
;;   (response req (views/user-page req)))

(declare get-user)
