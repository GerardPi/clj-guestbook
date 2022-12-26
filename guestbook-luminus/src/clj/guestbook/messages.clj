(ns guestbook.messages
  (:require [guestbook.db.core :as db]
            [clojure.tools.logging :as log]
            [guestbook.validation.message :as message-val])
  (:import (java.time LocalDateTime)
           (java.util UUID)))

(defn message-list []
  (db/get-messages))

(defn one-message [id]
  (db/get-message {:id id}))

(defn create-message! [^UUID id ^LocalDateTime timestamp message]
  (if-let [errors (message-val/validate message)]
    (throw (ex-info "Message is invalid" {:guestbook/error-id :validation :errors errors}))
    (let [message-complete (assoc message :id id :timestamp timestamp)]
      (log/info "create message" message-complete)
      (db/create-message! message-complete))))

(defn update-message! [^UUID id ^LocalDateTime timestamp message]
  (if-let [errors (message-val/validate message)]
    (throw (ex-info "Message is invalid" {:guestbook/error-id :validation :errors errors}))
    (let [message-complete (assoc message :id id :timestamp timestamp)]
      (log/info "update message" message-complete)
      (db/update-message! message-complete))))

(defn delete-message! [^UUID id]
  (db/delete-message! {:id id}))
