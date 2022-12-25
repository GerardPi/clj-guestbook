(ns guestbook.routes.home
  (:require
    [guestbook.layout :as layout]
    [guestbook.middleware :as middleware]
    [guestbook.db.core :as db]
    [guestbook.validation.message :as message-val]
    [ring.util.http-response :as response]
    [guestbook.messages :as msg]
    [ring.util.response])
  (:import (java.time LocalDateTime)
           (java.util UUID)))

(defn home-page [request]
  (layout/render request "home.html"))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])

