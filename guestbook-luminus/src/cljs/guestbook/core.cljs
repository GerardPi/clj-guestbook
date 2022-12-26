(ns guestbook.core
  (:require [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [guestbook.validation.message :as message-val]
            [guestbook.messages.forms :as msg-forms]
            [re-frame.core :as rf]
            [reagent.dom :as dom]))

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

(rf/reg-event-db
  :messages/add
  (fn [db [_ message]]
    (update db :messages/list conj message)))

(defn messages-get-success-handler [response]
  (let [messages (:messages response)]
    (.log js/console "received messages" messages)
    (rf/dispatch [:messages/set messages])))

(defn load-messages-list []
  (GET "/api/messages"
       {:headers headers-for-get
        :handler #(messages-get-success-handler %)}))


(defn post-success-handler [response fields errors]
  (let [new-message (-> @fields (assoc :timestamp (js/Date.))
                                (update :name str " [CLIENT]"))]
    (.log js/console (str "post success response:" response " new message: " new-message))
    (rf/dispatch [:messages/add new-message])
    (reset! fields nil)
    (reset! errors nil)))

(defn post-error-handler [error errors]
  (.log js/console (str error))
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
        z-index (.indexOf iso-string \Z)
        date-string (.substring iso-string 0 t-index)
        time-string (.substring iso-string (+ t-index 1) z-index)]
    [:time {:date-time iso-string :title (str "Date/time:" iso-string)} (str date-string " " time-string)]))

(defn message-list [messages]
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}
     [:li (format-date-time timestamp)
      [:p message]
      [:p "@" name]])])

(defn home []
  (let [messages (rf/subscribe [:messages/list])]
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

;;
;; CAUTION: This will (of course :-) ) not work when it says "^:def/after-load"
;;
(defn ^:def/after-load mount-components []
       (rf/clear-subscription-cache!)
       (.log js/console "Mounting components...")
       (dom/render [#'home] (.getElementById js/document "content"))
       (.log js/console "Components Mounted!"))

(defn init! []
  (.log js/console "Initialize App...")
  (rf/dispatch [:app/initialize])
  (load-messages-list)
  (mount-components))

(.log js/console "guestbook.core enabled!")