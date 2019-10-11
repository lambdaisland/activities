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
      ["" {:name ::activity
           :get #'handlers/get-activity
           :post #'handlers/update-activity
           :delete #'handlers/delete-activity}]
      ["/edit" {:name ::edit-activity
                :get #'handlers/edit-activity}]]]]])

(defmethod integrant/init-key :router [_ config] ;; {}
  (reitit.ring/router routes))

(def ring-config
  (-> ring.middleware.defaults/site-defaults
      (assoc-in [:security :anti-forgery] false)))

(defn- hidden-method
  [request]
  (keyword
   (or (get-in request [:form-params "_method"])         ;; look for "_method" field in :form-params
       (get-in request [:multipart-params "_method"])))) ;; or in :multipart-params

(def wrap-hidden-method
  {:name ::wrap-hidden-method
   :wrap (fn [handler]
           (fn [request]
             (clojure.pprint/pprint [:request request])
             (if-let [fm (and (= :post (:request-method request)) ;; if this is a :post request
                              (hidden-method request))] ;; and there is a "_method" field 
               (do (prn (str "it's a " fm))
                   (handler (assoc request :request-method fm)))
               (do (prn "it's not!")
                   (handler request)))))})

(defmethod integrant/init-key :ring-handler [_ config] ;; {:router reitit-router}
  (-> (:router config)
      (reitit.ring/ring-handler (reitit.ring/create-default-handler) {:middleware [wrap-hidden-method]})
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
