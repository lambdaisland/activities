(ns activities.system
  (:require [org.httpkit.server :as httpkit]
            [clojure.java.io :as io]
            [aero.core :as aero]
            [integrant.core :as integrant]
            [reitit.ring :as reitit]
            [activities.handlers :as handlers]
            ))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (integrant/ref value))

(defn read-config []
  (aero/read-config (io/resource "activities/system.edn")))

(extend-protocol reitit.core/Expand
  clojure.lang.Var
  (expand [this _] {:handler this}))

(def routes
  [["/" ::index]
   ["/activities"
    ["/new" {:name ::new-activity
             :get  #'handlers/new-activity-form}]]
   ["/activity"
    {:name ::create-activity
     :post #'handlers/create-activity}]
   ["/activity"
    ["/:id" {:name ::activity
             :get  #'handlers/get-activity}]]])

(defmethod integrant/init-key :router [_ config]
  (reitit.ring/router routes))

(defmethod integrant/init-key :ring-handler [_ config]
  (reitit.ring/ring-handler (:router config)))

(defmethod integrant/init-key :http-kit [_ config]
  (assoc config
         :stop-server
         (httpkit/run-server (:handler config) config)))

(defmethod integrant/halt-key! :http-kit [_ {stop-server :stop-server}]
  (stop-server))
