(ns activities.system
  (:require [org.httpkit.server :as httpkit]
            [clojure.java.io :as io]
            [aero.core :as aero]
            [integrant.core :as integrant]))

(defn read-config []
  (aero/read-config (io/resource "activities/system.edn")))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "still good...."})

(defmethod integrant/init-key :http-kit [_ config]
  (assoc config
         :stop-server
         (httpkit/run-server #'app config)))

(defmethod integrant/halt-key! :http-kit [_ {stop-server :stop-server}]
  (stop-server))
