(ns activities.system
  (:require [activities.handlers :as handlers]
            [aero.core :as aero]
            [clojure.java.io :as io]
            [integrant.core :as integrant]
            [org.httpkit.server :as httpkit]
            [reitit.ring :as reitit]
            [crux.api :as crux]
            [ring.middleware.defaults]
            [ring.middleware.resource]
            [ring.middleware.content-type]
            [ring.middleware.not-modified]
            [prone.middleware :as prone]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.auth.backends :as backends]))

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
  [["/"
    {:name ::index
     :get  #'handlers/redirect-to-activities}]
   ["/activities" {:name ::activities}
    ["" {:get  #'handlers/list-activities
         :post #'handlers/create-activity}]
    ["/new" {:name ::new-activity
             :get  #'handlers/new-activity-form}]]
   ["/activity" {}
    ["/:id" {}
     ["" {:name   ::activity
          :get    #'handlers/get-activity
          :post   #'handlers/update-activity
          :delete #'handlers/delete-activity}]
     ["/edit" {}
      ["" {:name ::edit-activity
           :get  #'handlers/edit-activity}]]
     ["/join" {}
      ["" {:name   ::join
           :post   #'handlers/join-activity
           :delete #'handlers/leave-activity}]]]]
   ["/login" {}
    ["" {:name ::login-form
         :get  #'handlers/login-form
         :post #'handlers/login-submission}]]
   ["/register" {}
    ["" {:name ::register
         :get  #'handlers/register-form
         :post #'handlers/register-submission}]]
   ["/logout" {}
    ["" {:name ::logout
         :get  #'handlers/logout}]]])

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
             (if-let [fm (and (= :post (:request-method request)) ;; if this is a :post request
                              (hidden-method request))] ;; and there is a "_method" field
               (handler (assoc request :request-method fm))
               (handler request))))})

(defn wrap-inject-crux [handler crux]
  (fn [req]
    (handler (assoc req :crux crux))))

(defmethod integrant/init-key :ring-handler [_ config] ;; {:router reitit-router}
  (let [wrap-req-res (or (requiring-resolve 'user/wrap-capture-request-response) identity)]
    (-> (:router config)
        ;; wrap-hidden-method must be applied before requests are matched with handlers
        ;; https://cljdoc.org/d/metosin/reitit-ring/0.3.9/doc/ring/restful-form-methods
        (reitit.ring/ring-handler (reitit.ring/create-default-handler) {:middleware [wrap-hidden-method]})
        wrap-req-res
        (wrap-inject-crux (:crux config))
        (wrap-authentication (backends/session)) ;FIX
        (ring.middleware.defaults/wrap-defaults ring-config)
        (prone/wrap-exceptions))))

(defmethod integrant/init-key :http-kit [_ config] ;; {:port 5387 :handler ...ring-handler...}
  (assoc config
         :stop-server
         (httpkit/run-server (:handler config) config)))

(defmethod integrant/halt-key! :http-kit [_ {stop-server :stop-server}]
  (stop-server))

(defmethod integrant/init-key :crux [_ opts]
  (crux/start-node opts))

(defmethod integrant/halt-key! :crux [_ node]
  (.close node))
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
