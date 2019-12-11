(ns activities.views
  (:require [hiccup2.core :as hiccup]
            [activities.flexmark :as flexmark]
            [java-time :as time]
            [activities.utils :refer [path] :as utils]
            [activities.user :as user]
            [crux.api :as crux]
            [activities.activity :as activity]
            [clojure.string]))

(defn navbar [req username]
  [:nav.navbar
   [:h1.navbar-title "Activities"]
   (when username
     [:span "Logged in as " username])
   [:ul.navbar-menu
    [:li
     [:a {:href (path req :activities.system/activities)} "Activities List"]]
    [:li
     [:a {:href (path req :activities.system/new-activity)} "New Activity"]]
    [:li
     [:a {:href (path req :activities.system/login-form)} "Login"]]
    [:li
     [:a {:href (path req :activities.system/register)} "Signup"]]]])

(defn layout
  "Mounts a page template given a title and a hiccup body."
  [req title username main]
  [:html
   [:head
    [:title title]
    [:meta {:name "viewport" :content "width=device-width"}]
    [:link {:rel "stylesheet" :href "/styles.css"}]]
   [:body
    (navbar req username)
    main]])

(defn register [& [name email msg]]
  (let [error-msg [:div [:small.auth-error msg]]]
    [:main.sign
     [:header.sign-title [:h1.sign-title-heading "Activities"]]
     [:div.auth
      [:form {:method "POST" :action "/register"}
       [:header.auth-title
        [:h1 "Register"]]
       [:div.auth-field-title
        [:label {:for "name"} "Name: "
         [:div
          [:input.auth-field-input {:id "name" :name "name" :type "text" :value name}]]]]
       [:div.auth-field-title
        [:label {:for "email"} "Email: "]
        [:div
         [:input.auth-field-input {:id "email"
                                   :name "email"
                                   :type "email"
                                   :required true
                                   :value email}]]]
       (when msg error-msg)
       [:div
        [:label.auth-field-title {:for "password"} "Password: "]
        [:div
         [:input.auth-field-input {:id "password"
                                   :name "password"
                                   :type "password"
                                   :required true
                                   :minlength "8"
                                   :autocomplete "new-password"}]]]
       [:div
        [:label.auth-field-title {:for "password"} "Repeat password: "]
        [:div
         [:input.auth-field-input {:id "password"
                                   :name "password"
                                   :type "password"
                                   :required true
                                   :minlength "8"
                                   :autocomplete "new-password"}]]]
       [:div
        [:div
         [:button.auth-submit {:type "submit" :value "Submit"} "Sign Up"]]]]]]))

(defn input-field [req opts]
  [:input (assoc opts :value (get-in req [:params (keyword (:name opts))]))])

(defn login [req & [msg]]
  (let [error-msg [:div [:small.auth-error msg]]]
    [:main.sign
     [:header.sign-title [:h1.sign-title-heading "Activities"]]
     [:div.auth
      [:form {:method "POST" :action "/login"}
       [:header.auth-title
        [:h1 "Welcome back!"]]
       [:div.auth-field-title
        [:label {:for "email"} "Email: "]
        [:div
         (input-field req {:id "email"
                           :name "email"
                           :type "email"
                           :class "auth-field-input"
                           :required true})]]
       [:div.auth-field-title
        [:label {:for "password"} "Password (8 characters minimum): "]
        [:div
         [:input.auth-field-input {:id "password"
                                   :name "password"
                                   :type "password"
                                   :required true
                                   :minlength "8"
                                   :autocomplete "current-password"}]]]
       (when msg error-msg)
       [:div
        [:div
         [:button.auth-submit {:type "submit" :value "Login"} "Login"]]]]]]))

;; (defn activities-list [activities]
;;   (common)
;;   [:main
;;    [:header [:h1 "List of activities"]]
;;    [:div activities]])

(defn activity-form [req]
  [:div
   [:form {:method "POST" :action (path req :activities.system/activities)}
    [:header
     [:h2 "New Activity Proposal"]]
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
      [:input {:type "submit" :value "Submit Activity"}]]]]])

(defn activity-page
  [req]
  (let [activity     (activity/req->activity req)
        activity-id  (str (get activity :crux.db/id))
        title        (get activity :activity/title)
        description  (get activity :activity/description)
        date-time    (time/local-date-time (get activity :activity/date-time) (time/zone-id "UTC"))
        date         (time/format (time/local-date date-time))
        time         (time/format (time/local-time date-time))
        duration     (get activity :activity/duration)
        capacity     (get activity :activity/capacity)
        participants (get activity :activity/participants)
        creator      (get activity :activity/creator)
        user-uuid    (user/req->uuid req)]
    [:div
     [:h1 title]
     [:div
      (hiccup/raw (flexmark/md->html description))]
     [:div
      [:p (str "On: " date)]
      [:p (str "At: " time)]
      [:p (str "For: " duration "m")]
      [:p (str "Participants: " (count participants) "/" capacity)]]
     [:div
      (if (= creator user-uuid)
        [:div
         [:form {:method "POST"
                 :action (path req
                               :activities.system/delete-activity
                               {:id activity-id})}
          [:input {:type "hidden" :name "_method" :value "delete"}]
          [:input {:type "submit" :value "Delete"}]]
         [:form {:method "GET"
                 :action (path req
                               :activities.system/edit-activity
                               {:id activity-id})}
          [:input {:type "submit" :value "Edit"}]]]
        (if (contains? participants user-uuid)
          [:form {:method "POST"
                  :action (path req
                                :activities.system/join-activity
                                {:id activity-id})}
           [:input {:type "hidden" :name "_method" :value "delete"}]
           [:input {:type "submit" :value "Leave"}]]
          [:form {:method "POST"
                  :action (path req
                                :activities.system/join-activity
                                {:id activity-id})}
           [:input {:type "submit" :value "Join"}]]))]]))


;; (defn user-page [req]
;;   (let [node (:crux req)
;;         db (crux/db node)
;;         u-uuid (user/req->uuid req)
;;         user (crux/entity db u-uuid)
;;         activities (user/uuid->activities u-uuid)])
;;   [:div
;;    [:h2 name]
;;    [:p email]
;;    [:ul
;;     (map (fn [a-uuid]))]])

(defn edit-activity-form
  [req]
  (let [activity    (activity/req->activity req)
        activity-id (:crux.db/id activity)
        title       (:activity/title activity)
        description (:activity/description activity)
        inst        (:activity/date-time activity)
        datetime    (-> inst
                        (time/local-date-time (time/zone-id "UTC"))
                        (time/truncate-to :minutes)
                        (time/format))
        duration    (:activity/duration activity)
        capacity    (:activity/capacity activity)]
    [:div
     [:form
      {:method "POST"
       :action (path req :activities.system/activity {:id activity-id})}
      [:div
       [:label {:for "title"} "Title: "]
       [:div
        [:input {:id    "title"
                 :name  "title"
                 :type  "text"
                 :value title}]]]
      [:div
       [:label {:for "description"} "Description: "]
       [:div
        [:textarea {:id   "description"
                    :name "description"
                    :type "msg"
                    :value description}]]]
      [:div
       [:label {:for "datetime"} "Date-time: "]
       [:div
        [:input {:id    "datetime"
                 :type  "datetime-local"
                 :name  "datetime"
                 :value datetime}]]]
      [:div
       [:label {:for "duration"} "Duration: "]
       [:div
        [:input {:id    "duration"
                 :type  "number"
                 :name  "duration"
                 :value (str duration)}]]]
      [:div
       [:label {:for "capacity"} "Capacity: "]
       [:div
        [:input {:id    "capacity"
                 :type  "number"
                 :name  "capacity"
                 :value (str capacity)}]]]
      [:div
       [:div
        [:input {:type "submit"}]]]]]))

(defn- activity-card [activity]
  (let [title       (:activity/title activity)
        description (:activity/description activity)
        inst        (:activity/date-time activity)
        date-time   (time/local-date-time inst (time/zone-id "UTC"))
        time        (time/format (time/local-time date-time))
        capacity    (:activity/capacity activity)
        user-count  (count (:activity/participants activity))]
    [:article.card
     [:div.card-main
      [:header.card-header
       [:h2.card-header--title title]
       [:small.card-header--tags
        (clojure.string/join " " (map #(str "#" %) ["tags" "test"]))]]
      [:p.card-description description]]
     [:footer.card-footer
      [:small.card-footer--time time]
      [:small.card-footer--capacity (str user-count "/" capacity)]]
     [:button.card--button "More"]]))

(defn activities
  [req]
  (let [activities (activity/req->activities req)]
    [:main.activities
     (map activity-card activities)]))
