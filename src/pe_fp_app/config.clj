(ns pe-fp-app.config
  (:require [environ.core :refer [env]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Environment-controlled configuration values
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-base-url (env :fp-base-url))
(def fp-nrepl-server-port (env :fp-nrepl-server-port))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Environment-controlled database config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-app-version       (env :fp-app-version))
(def fp-db-name           (env :fp-db-name))
(def fp-db-server-host    (env :fp-db-server-host))
(def fp-db-server-port    (env :fp-db-server-port))
(def fp-db-username       (env :fp-db-username))
(def fp-db-password       (env :fp-db-password))
(def fp-jdbc-driver-class (env :fp-db-driver-class))
(def fp-jdbc-subprotocol  (env :fp-jdbc-subprotocol))

(defn db-spec-fn
  ([]
   (db-spec-fn nil))
  ([db-name]
   (let [subname-prefix (format "//%s:%s/" fp-db-server-host fp-db-server-port)]
     {:classname fp-jdbc-driver-class
      :subprotocol fp-jdbc-subprotocol
      :subname (if db-name
                 (str subname-prefix db-name)
                 subname-prefix)
      :user fp-db-username
      :password fp-db-password})))

(def db-spec-without-db (db-spec-fn nil))
(def db-spec (db-spec-fn fp-db-name))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HTTP-related configuration values
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; URI prefix
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-entity-uri-prefix "/fp/")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 'Authorization' header parts
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-auth-scheme "fp-auth")
(def fp-auth-scheme-param-name "fp-token")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Media sub-type prefix
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-mt-subtype-prefix "vnd.fp.")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Headers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fphdr-establish-session "fp-establish-session")
(def fphdr-auth-token "fp-auth-token")
(def fphdr-error-mask "fp-error-mask")
(def fphdr-if-unmodified-since "fp-if-unmodified-since")
(def fphdr-login-failed-reason "fp-login-failed-reason")
(def fphdr-delete-reason "fp-delete-reason")
