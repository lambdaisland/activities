#_
{:crux.db/id    uuid
 :user/name     name
 :user/email    email
 :user/password password-hash
 :user/admin?   true}

#_
{:crux.db/id #uuid "f190baa5-5c9d-4506-b7bc-901ef175dcf0"
 :activity/title "Activity 01"
 :activity/description "Description 01"
 :activity/date-time (time/local-date-time "2019-11-11T09:00")
 :activity/duration (time/duration (time/minutes 30))
 :activity/capacity 8
 :activity/creator uuid?
 :activity/participants #{}}

(let [current-user (:current-user req)]
  (if (:user/admin? current-user)
    ...
    {:status 403
     :body "not allowed"}))
