(ns guestbook.db.core
  (:require
    [java-time.api :as java-time-api]
    [next.jdbc.date-time]
    [next.jdbc.result-set]
    [conman.core :as conman]
    [mount.core :refer [defstate]]
    [guestbook.config :refer [env]])
  (:import (java.sql Timestamp Date Time)
           (java.time ZoneId)))

(defstate ^:dynamic *db*
          :start (conman/connect! {:jdbc-url (env :database-url)})
          :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn sql-timestamp->inst [^Timestamp t]
      (-> t (.toLocalDateTime)
          (.atZone (ZoneId/systemDefault))
          (java-time-api/java-date)))


(extend-protocol next.jdbc.result-set/ReadableColumn
  Timestamp
  (read-column-by-label [^Timestamp v _]
    (sql-timestamp->inst v))
  (read-column-by-index [^Timestamp v _2 _3]
    (sql-timestamp->inst v))
  Date
  (read-column-by-label [^Date v _]
    (.toLocalDate v))
  (read-column-by-index [^Date v _2 _3]
    (.toLocalDate v))
  Time
  (read-column-by-label [^Time v _]
    (.toLocalTime v))
  (read-column-by-index [^Time v _2 _3]
    (.toLocalTime v)))
