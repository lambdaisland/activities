(ns activities.views)

;; (def router (:router integrant.repl.state/system))

;; (defn make-nav [router route & [args]]
;;   (let [routes (reitit.core/route-names router)]
;;     (for [r routes]
;;       (if (= r route)
;;         [:li [:a.current title]]
;;         [:li [:a {:href (path-to-route)}]]))))

;; (map (fn [] [:li
;;             (if (= a current)))


;; (defn common [title]
;;   [:header
;;    [:h1 title]
;;    [:nav]])

;; ;; TODO nav: ask for help
;; (defn main-nav [route-name]
;;   (for [route (r/route-names (:router (:reitit.core/router)))])
;;   [:nav
;;    [:ul
;;     [:li]]])

(defn navbar [username]
  [:nav.navbar
   [:h1.navbar-title "Activities"]
   (when username
     [:span "Logged in as " username])
   [:ul.navbar-menu
    [:li
     [:a {:href "/activities"} "Activities List"]]
    [:li
     [:a {:href "/activities/new"} "New Activity"]]
    [:li
     [:a {:href "/login"} "Login"]]
    [:li
     [:a {:href "/register"} "Signup"]]]])

(defn layout
  "Mounts a page template given a title and a hiccup body."
  [title username main]
  [:html
   [:head
    [:title title]
    [:meta {:name "viewport" :content "width=device-width"}]
    [:link {:rel "stylesheet" :href "/styles.css"}]]
   [:body
    (navbar username)
    main]])

(defn register [& [name email msg]]
  (let [error-msg [:div [:small.auth-error msg]]]
    [:main
     [:header.title [:h1.title-heading "Activities"]]
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
    [:main
     [:header.title [:h1.title-heading "Activities"]]
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
