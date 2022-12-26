(ns guestbook.routes.services
  (:require
    [clojure.tools.logging :as log]
    [guestbook.messages :as msg]
    [guestbook.middleware.formats :as formats]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.coercion :as ring-coercion]
    [reitit.ring.middleware.exception :as mw-exception]
    [reitit.ring.middleware.multipart :as mw-multipart]
    [reitit.ring.middleware.muuntaja :as mw-muuntaja]
    [reitit.ring.middleware.parameters :as mw-parameters]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [ring.util.http-response :as response]
    [ring.util.response])
  (:import (java.time LocalDateTime)
           (java.util UUID)))

(def message-creation-request-spec {:body {:name string? :message string?}})

(def message-update-request-spec {:path {:id string?}
                                  :body {:name string? :message string?}})

(def message-list-response-spec
  {:this-must-be-present-no-idea-why
   {200 {:body {:messages [{:id uuid? :name string? :message string? :timestamp inst?}]}}}})

(def one-message-response-spec
  {:this-must-be-present-no-idea-why
   {200 {:body {:id uuid? :name string? :message string? :timestamp inst?}}
    400 {:body map?}}})

(def message-delete-response-specs
  {200 {:body map?}
   404 {:body map?}
   400 {:body map?}})

(def message-creation-response-specs
  {201 {:body map?}
   400 {:body map?}
   500 {:errors map?}})

(def message-update-response-specs
  {201 {:body map?}
   400 {:body map?}
   500 {:errors map?}})

(defn message-list []
  (fn [_]
    (try
      (let [messages (msg/message-list)]
        (println "### messages " messages "##")
        (response/ok {:messages (vec messages)}))
      (catch Exception e
        (let [message (str "Failed to get messages" (.getMessage e))]
          (log/warn message)
          (response/internal-server-error {:errors {:server-error [message]}}))))))

(defn body-problem-message-not-found [message-id]
  {:type :message-not-found :message "Could not find a message with the id given"
   :data {:id message-id}})


(defn one-message []
  (fn [request]
    (log/info "request [" (-> request :parameters :path) "]")
    (try
      (let [message-id-str (-> request :parameters :path :id)
            message-id (UUID/fromString message-id-str)]
        (log/info "id: " message-id)
        (try
          (if-let [message (msg/one-message message-id)]
            (response/ok message)
            (do
              (log/warn "not found: " message-id)
              (response/not-found (body-problem-message-not-found message-id))))
          (catch Exception e
            (let [message (str "Failed to get one message with ID '" message-id "'. " (.getMessage e))]
              (log/warn message)
              (response/internal-server-error {:errors {:server-error [message]}})))))
      (catch Exception e
        (let [{id :guestbook/error-id errors :errors} (ex-data e)
              message (str "Failed to convert message ID!" (.getMessage e))]
          (log/warn message errors)
          (response/bad-request {:errors errors}))))))


(defn delete-message! []
  (fn [request]
    (log/info "delete-message! req [" (-> request :parameters :path) "]")
    (try
      (let [message-id-str (-> request :parameters :path :id)
            message-id (UUID/fromString message-id-str)
            body {:status :ok}]
        (log/info "delete-message! id [" message-id "]")
        (try
          (if-let [message (msg/one-message message-id)]
            (do
              (msg/delete-message! message-id)
              (response/ok body))
            (do
              (log/warn "Attempt to delete non-existing message with id" message-id)
              (response/not-found (body-problem-message-not-found message-id))))
          (catch Exception e
            (response/internal-server-error {:errors {:server-error [(str "Failed to delete message with ID '" message-id "'. " (.getMessage e))]}}))))
      (catch Exception e
        (let [message (str "Failed to convert message ID!" (.getMessage e))]
          (log/warn message)
          (response/bad-request {:errors {:server-error [message]}}))))))

(defn create-message! []
  (fn [request]
    (try
      (log/info "create-message! parameters [" (-> request :parameters) "]")
      (let [params (-> request :parameters :body)
            id (UUID/randomUUID)
            timestamp (LocalDateTime/now)]
        (msg/create-message! id timestamp params)
        (let [url (str "/messages/" id)
              body {:status :ok}]
          (response/created url body)))
      (catch Exception e
        (let [{id :guestbook/error-id errors :errors} (ex-data e)
              errorMessage (.getMessage e)]
          (log/warn "id" id "errors" errors "errorMessage" errorMessage)
          (case id
            :validation
            (response/bad-request {:errors errors})
            (response/internal-server-error {:errors {:server-error [(str "Failed to create message" (.getMessage e))]}})))))))

(defn update-message! []
  (fn [request]
    (log/info "update-message! request [" (-> request :parameters :path) "]")
    (try
      (let [message-id-str (-> request :parameters :path :id)
            params (-> request :parameters :body)
            message-id (UUID/fromString message-id-str)
            timestamp (LocalDateTime/now)
            body {:status :ok}]
        (try
          (if-let [message (msg/one-message message-id)]
            (do
              (msg/update-message! message-id timestamp params)
              (response/ok body))
            (do
              (log/warn "Attempt to delete non-existing message with id" message-id)
              (response/not-found (body-problem-message-not-found message-id))))
          (catch Exception e
            (let [{id :guestbook/error-id errors :errors} (ex-data e)
                  errorMessage (.getMessage e)]
              (log/warn "id" id "errors" errors "errorMessage" errorMessage)
              (case id
                :validation
                (response/bad-request {:errors errors})
                (response/internal-server-error {:errors {:server-error [(str "Failed to update message" (.getMessage e))]}}))))))
      (catch Exception e
        (let [{id :guestbook/error-id errors :errors} (ex-data e)
              message (str "Failed to convert message ID!" (.getMessage e))]
          (log/warn message errors)
          (response/bad-request {:errors errors}))))))

(defn middleware-spec []
  [
   ;; query-params & form-params
   mw-parameters/parameters-middleware
   ;; content-negotiation
   mw-muuntaja/format-negotiate-middleware
   ;; encoding response body
   mw-muuntaja/format-response-middleware
   ;; exception handling
   mw-exception/exception-middleware
   ;; decoding request body
   mw-muuntaja/format-request-middleware
   ;; coercing response bodys
   ring-coercion/coerce-response-middleware
   ;; coercing request parameters
   ring-coercion/coerce-request-middleware
   ;; multipart params
   mw-multipart/multipart-middleware])


(defn service-routes []
  ["/api"
   {:middleware (middleware-spec)
    :muuntaja   formats/instance
    :coercion   spec-coercion/coercion
    :swagger    {:id ::api}
    }
   ["" {:no-doc true}
    ["/swagger.json" {:get (swagger/create-swagger-handler)}]
    ["/swagger-ui*" {:get (swagger-ui/create-swagger-ui-handler {:url "/api/swagger.json"})}]]
   ["/messages"
    [""
     {:get  {
             :responses message-list-response-spec
             :handler   (message-list)}
      :post {
             :parameters message-creation-request-spec
             :handler    (create-message!)
             :responses  message-creation-response-specs}
      }]
    ["/:id"
     {:get    {
               :parameters {:path {:id string?}}
               :responses  one-message-response-spec
               :handler    (one-message)}
      :put    {
               :parameters message-update-request-spec
               :responses  message-update-response-specs
               :handler    (update-message!)}
      :delete {
               :parameters {:path {:id string?}}
               :responses  message-delete-response-specs
               :handler    (delete-message!)}
      }]
    ]
   ])
