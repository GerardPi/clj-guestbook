(ns guestbook.handler
  (:require
    [guestbook.middleware :as middleware]
    [guestbook.layout :refer [error-page]]
    [guestbook.routes.home :refer [home-routes]]
    [guestbook.routes.services :refer [service-routes]]
    [reitit.ring :as ring]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.webjars :refer [wrap-webjars]]
    [guestbook.env :refer [defaults]]
    [reitit.ring.middleware.dev :as mw-dev]
    [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(defn- async-aware-default-handler
  ([_] nil)
  ([_ respond _] (respond nil)))


(defn create-default-handler []
  (ring/create-default-handler
    {:not-found
     (constantly (error-page {:status 404, :title "404 - Page not found"}))
     :method-not-allowed
     (constantly (error-page {:status 405, :title "405 - Not allowed"}))
     :not-acceptable
     (constantly (error-page {:status 406, :title "406 - Not acceptable"}))}))

(def debug-enabled false)

(defn router-options []
  (when debug-enabled
    {:reitit.middleware/transform mw-dev/print-request-diffs}))

(mount/defstate app-routes
  :start
  (ring/ring-handler
    (ring/router
      [(home-routes) (service-routes)]
      (router-options))
    (ring/routes
      (ring/create-resource-handler
        {:path "/"})
      (wrap-content-type
        (wrap-webjars async-aware-default-handler))
      (create-default-handler))))

(defn app []
  (middleware/wrap-base #'app-routes))
