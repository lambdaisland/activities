(ns activities.system
  (:require [activities.handlers :as handlers]
            [aero.core :as aero]
            [clojure.java.io :as io]
            [integrant.core :as integrant]
            [org.httpkit.server :as httpkit]
            [reitit.ring :as reitit]
            [ring.middleware.defaults]))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (integrant/ref value))

(defn read-config []
  (aero/read-config (io/resource "activities/system.edn")))

(extend-protocol reitit.core/Expand
  clojure.lang.Var
  (expand [this _]
    {:handler this}))

(def routes
  [["/" ::index]
   ["/activities" {}
    ["" {:get    #'handlers/list-activities
         :post   #'handlers/create-activity}]
    ["/new" {:name ::new-activity
             :get  #'handlers/new-activity-form}]
    ["/activity" {}
     ["/:id" {}
      ["" {:get #'handlers/get-activity
           :put #'handlers/edit-activity
           :delete #'handlers/delete-activity}]
      ["/edit" {:name ::edit-activity-form
                :get #'handlers/edit-activity-form}]]]]])

(defmethod integrant/init-key :router [_ config] ;; {}
  (reitit.ring/router routes))

(def ring-config
  (-> ring.middleware.defaults/site-defaults
      (assoc-in [:security :anti-forgery] false)))

(defmethod integrant/init-key :ring-handler [_ config] ;; {:router reitit-router}
  (-> (:router config)
      reitit.ring/ring-handler
      (ring.middleware.defaults/wrap-defaults ring-config)))

(defmethod integrant/init-key :http-kit [_ config] ;; {:port 5387 :handler ...ring-handler...}
  (assoc config
         :stop-server
         (httpkit/run-server (:handler config) config)))

(defmethod integrant/halt-key! :http-kit [_ {stop-server :stop-server}]
  (stop-server))

#_
{:router ...reitit-router...
 :ring-handler ...ring-handler...
 :http-kit {:port 5387
            :handler ...ring-handler...
            :stop-server (fn ...)}}

(comment
  (def router (:router integrant.repl.state/system))

  (:path (reitit.core/match-by-name router ::activity {:id 456}))
  ;; => "/activity/456"

  (reitit.core/match-by-path router "/activity/123"))
