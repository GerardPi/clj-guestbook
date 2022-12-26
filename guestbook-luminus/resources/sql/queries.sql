

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name create-message! :! :n
-- :doc creates a new message using the name and message keys
INSERT INTO guestbook
(id, name, message, timestamp)
VALUES (:id, :name, :message, :timestamp)

-- :name update-message! :! :n
-- :doc updates a message using the name and message keys
UPDATE guestbook
SET name=:name, message=:message, timestamp=:timestamp
WHERE id=:id

-- :name get-messages :? :*
-- :doc selects all available messages
SELECT * from guestbook

-- :name get-message :? :1
-- :doc selects a messages
SELECT * from guestbook where id=:id

-- :name delete-message! :! :n
-- :doc deletes a messages
DELETE from guestbook
WHERE id = :id
