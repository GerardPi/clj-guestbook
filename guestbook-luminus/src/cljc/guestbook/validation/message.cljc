(ns guestbook.validation.message
  (:require [struct.core :as st]))

(def message-schema
  [[:name st/required st/string]
   [:message st/required st/string
    {:message  "message must contain at least 10 character"
     :validate (fn [msg] (>= (count msg) 10))}]])

(defn validate [params]
  (first (st/validate params message-schema)))
