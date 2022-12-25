(ns guestbook.messages.forms
  (:require [reagent.core :as r]
            [re-frame.core :as rf]))

(defn set-new-field-value [key element fields]
  (swap! fields assoc key (-> element .-target .-value)))

(defn creation-form [errors-component submit-message-fn]
  (let [fields (r/atom {})
        errors (r/atom nil)]
    (fn []
      [:div
       [errors-component errors :server-error]
       [:div.field
        [:label.label {:for :name} "Name"]
        [errors-component errors :name]
        [:input.input
         {:type      :text
          :name      :name
          :on-change #(set-new-field-value :name % fields)
          :value     (:name @fields)}]]
       [:div.field
        [:label.label {:for :message} "Message"]
        [errors-component errors :message]
        [:textarea.textarea
         {:name      :message
          :value     (:message @fields)
          :on-change #(set-new-field-value :message % fields)}]]
       [:input.button.is-primary
        {:type     :submit
         :on-click #(submit-message-fn fields errors)
         :value    "comment"}]
       [:div.field
        [:label.label "What was entered above:"]
        [:p "Name : '" (:name @fields) "'"]
        [:p "Message: '" (:message @fields) "'"]]])))
