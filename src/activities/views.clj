(ns activities.views
  (:require [hiccup2.core :as hiccup]
            [activities.flexmark :as flexmark]
            [java-time :as time]
            [activities.utils :refer [path] :as utils]
            [activities.activity :as a]
            [clojure.string]))

(defn navbar [router username]
  [:nav.navbar
   [:h1.navbar-title "Activities"]
   (when username
     [:span "Logged in as " username])
   [:ul.navbar-menu
    [:li
     [:a {:href (path router :activities.system/activities)} "Activities List"]]
    [:li
     [:a {:href (path router :activities.system/new-activity)} "New Activity"]]
    [:li
     [:a {:href (path router :activities.system/login-form)} "Login"]]
    [:li
     [:a {:href (path router :activities.system/register)} "Signup"]]]])

(defn layout
  "Mounts a page template given a title and a hiccup body."
  [{:keys [:reitit.core/router]} title username main]
  [:html
   [:head
    [:title title]
    [:meta {:name "viewport" :content "width=device-width"}]
    [:link {:rel "stylesheet" :href "/styles.css"}]]
   [:body
    (navbar router username)
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

(defn activity-form [{:keys [:reitit.core/router]}]
  [:div
   [:form {:method "POST" :action (path router :activities.system/activities)}
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

;; TODO instead of passing the user-uuid, pass the role and check that
(defn activity-page
  "Takes an activity map, a user-uuid and a router, destructures the activity
  namespace, modifies the format of some values to fit the purpose of the view
  and returns a form with button actions that correspond to the user role."
  [{:keys [:crux.db/id ::a/title ::a/description ::a/date-time ::a/duration
           ::a/capacity ::a/participants ::a/creator]} user-uuid router]
  (let [description (-> (flexmark/md->html description) hiccup/raw)
        date-time   (utils/inst->zoned-local-date-time date-time)
        date        (-> date-time time/local-date time/format)
        time        (-> date-time time/local-time time/format)
        make-path   #(path router % {:id id})]
    [:div
     [:h1 title]
     [:div description]
     [:div
      [:p (str "On: " date)]
      [:p (str "At: " time)]
      [:p (str "For: " duration "m")]
      [:p (str "Participants: " (count participants) "/" capacity)]]
     [:div
      (if (= creator user-uuid)
        [:div
         [:form {:method "POST"
                 :action (make-path :activities.system/delete-activity)}
          [:input {:type "hidden" :name "_method" :value "delete"}]
          [:input {:type "submit" :value "Delete"}]]
         [:form {:method "GET"
                 :action (make-path :activities.system/edit-activity)}
          [:input {:type "submit" :value "Edit"}]]]
        (let [join-path (make-path :activities.system/join-activity)]
          (if (contains? participants user-uuid)
            [:form {:method "POST"
                    :action join-path}
             [:input {:type "hidden" :name "_method" :value "delete"}]
             [:input {:type "submit" :value "Leave"}]]
            [:form {:method "POST"
                    :action join-path}
             [:input {:type "submit" :value "Join"}]])))]]))


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

;; TODO authorization
(defn edit-activity-form
  [{:keys [:crux.db/id ::a/title ::a/description ::a/date-time ::a/duration
           ::a/capacity]} _user-uuid router]
  (let [date-time (-> date-time
                      utils/inst->zoned-local-date-time
                      (time/truncate-to :minutes)
                      time/format)
        post-path (path router :activities.system/activity {:id id})]
    [:div
     [:form
      {:method "POST"
       :action post-path}
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
                    :type "msg"}
         description]]]
      [:div
       [:label {:for "datetime"} "Date-time: "]
       [:div
        [:input {:id    "datetime"
                 :type  "datetime-local"
                 :name  "datetime"
                 :value date-time}]]]
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

(defn- activity-card
  [{:keys [:crux.db/id ::a/title ::a/description ::a/date-time ::a/capacity
           ::a/participants]} router]
  (let [date-time   (time/local-date-time date-time (time/zone-id "UTC"))
        time        (time/format (time/local-time date-time))
        user-count  (count participants)
        activity-id (str id)
        path        (path router :activities.system/activity {:id activity-id})]
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
     [:button.card--button
      [:a.card--button-link {:href path} "More"]]]))

(defn activities
  [activities router]
  [:main.activities
   (map #(activity-card % router) activities)])
