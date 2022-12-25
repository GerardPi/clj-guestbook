(ns guestbook.core
  (:require [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [guestbook.validation.message :as message-val]
            [guestbook.messages.forms :as msg-forms]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(defn log [message]
  (.log js/console message))
(defn fetch-x-csrf-token-value-from-document []
  (.-value (.getElementById js/document "token")))

(def accept-encoding-transit-json "application/transit+json")
(def accept-encoding-json "application/json")

(def accept-encoding accept-encoding-transit-json)

(def headers-for-post {"Accept" accept-encoding "x-csrf-token" (fetch-x-csrf-token-value-from-document)})
(def headers-for-get {"Accept" accept-encoding})

(rf/reg-event-fx
  :app/initialize
  (fn [_ _] {:db {:messages/loading? true}}))

(rf/reg-sub
  :messages/loading?
  (fn [db _] (:messages/loading? db)))

(rf/reg-event-db
  :messages/set
  (fn [db [_ messages]]
    (-> db (assoc :messages/loading? false
                  :messages/list messages))))

(rf/reg-sub
  :messages/list
  (fn [db _]
    (:messages/list db [])))

(defn messages-get-success-handler [response]
  (log (str "received response >>" response "<<"))
  (let [messages (:messages response)]
    (log (str "received messages" messages))
    (rf/dispatch [:messages/set messages])))
(defn load-messages-list []
  (GET "/api/messages"
       {:headers headers-for-get
        :handler #(messages-get-success-handler %)}))

(rf/reg-event-db
  :message/add
  (fn [db [_ message]]
    (update db :messages/list conj message)))

(defn post-success-handler [response fields errors]
  (let [new-message (assoc @fields :timestamp (js/Date.))]
    (log (str "post success response:" response " new message: " new-message))
    (rf/dispatch [:message/add new-message])
    (reset! fields nil)
    (reset! errors nil)))

(defn post-error-handler [error errors]
  (log (str error))
  (reset! errors (-> error :response :errors)))

(defn submit-message! [fields errors]
  (if-let [validation-errors (message-val/validate @fields)]
    (reset! errors validation-errors)
    (POST "/api/messages"
          {:format        :json
           :headers       headers-for-post
           :params        @fields
           :handler       #(post-success-handler %1 fields errors)
           :error-handler #(post-error-handler %1 errors)})))

(defn errors-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))

(defn format-date-time [timestamp]
  (let [iso-string (.toISOString timestamp)
        t-index (.indexOf iso-string \T)
        date-string (.substring iso-string 0 t-index)
        time-string (.substring iso-string (+ t-index 1) (- (count iso-string) - 1))]
    [:time {:date-time iso-string :title iso-string} (str date-string " " time-string)]))

(defn message-list [messages]
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}
     [:li (format-date-time timestamp)
      [:p message]
      [:p " - " name]])])

(defn home []
  (let [messages (rf/subscribe [:messages/list])]
    (rf/dispatch [:app/initialize])
    (load-messages-list)
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       (if @(rf/subscribe [:messages/loading?])
         [:h3 "Loading messages..."]
         [:div
          [:div.columns>div.column
           [:h3 "Messages"]
           [message-list messages]]
          [:div.columns>div.column
           [msg-forms/creation-form errors-component submit-message!]]])])))

(dom/render
  [home]
  (.getElementById js/document "content"))
