(ns repl-sessions.user)

(last @user/last-requests)

(nth (reverse @user/last-requests) 3)

(def create-activity
  (last
   (filter (comp #{:post} :request-method) @user/last-requests)))


create-activity

(activities.user/req->uuid (last @user/last-requests))

(user/entity #uuid "4415f920-5f5b-4277-87e7-da0cb9ac221d")
(user/entity #uuid "abf230e5-2af2-4c44-8490-1864857d0b69")

(user/q )
