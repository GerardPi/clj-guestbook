(ns guestbook.db.core-test
  (:require
    [guestbook.db.core :refer [*db*] :as db]
    [java-time.pre-java8]
    [luminus-migrations.core :as migrations]
    [clojure.test :refer :all]
    [next.jdbc :as jdbc]
    [guestbook.config :refer [env]]
    [mount.core :as mount])
  (:import (java.time LocalDateTime)
           (java.util UUID)))

(def id (UUID/fromString "00000000-1111-2222-3333-444444444444"))
(def timestamp (LocalDateTime/now))

(def db-success-return-value 1)

  (use-fixtures
  :once
  (fn [f]
    (mount/start
     #'guestbook.config/env
     #'guestbook.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-users
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (is (= db-success-return-value (db/create-user!
              t-conn
              {:id         id
               :first_name "Sam"
               :last_name  "Smith"
               :email      "sam.smith@example.com"
               :pass       "pass"}
              {})))
    (is (= {:id         id
            :first_name "Sam"
            :last_name  "Smith"
            :email      "sam.smith@example.com"
            :pass       "pass"
            :admin      nil
            :last_login nil
            :is_active  nil}
           (db/get-user t-conn {:id id} {})))))

(deftest test-messages
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
   (is (= db-success-return-value (db/create-message!
          t-conn
          {:id      id
           :name    "Bab"
           :message "Hello wereld!"
           :timestamp timestamp})))
   (is (= {:id id
           :name "Bab"
           :message "Hello wereld!"}
          (-> (db/get-messages t-conn {})
              (first)
              (select-keys [:id :name :message]))))
   (is (= db-success-return-value (db/delete-message!
              t-conn
              {:id id}
              {})))))

