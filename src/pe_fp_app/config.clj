(ns pe-fp-app.config
  (:require [environ.core :refer [env]]
            [pe-user-core.core :as usercore]
            [pe-user-rest.meta :as usermeta]
            [clojurewerkz.mailer.core :refer [delivery-mode!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; URI prefix
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-entity-uri-prefix (env :fp-uri-prefix))

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
;; Header names
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fphdr-establish-session "fp-establish-session")
(def fphdr-auth-token "fp-auth-token")
(def fphdr-error-mask "fp-error-mask")
(def fphdr-if-unmodified-since "fp-if-unmodified-since")
(def fphdr-if-modified-since "fp-if-modified-since")
(def fphdr-login-failed-reason "fp-login-failed-reason")
(def fphdr-delete-reason "fp-delete-reason")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; REPL port number
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-nrepl-server-port (env :fp-nrepl-server-port))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Application version and config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-base-url    (env :fp-base-url))
(def fp-app-version (env :fp-app-version))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Database config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-db-name           (env :fp-db-name))
(def fp-db-server-host    (env :fp-db-server-host))
(def fp-db-server-port    (env :fp-db-server-port))
(def fp-db-username       (env :fp-db-username))
(def fp-db-password       (env :fp-db-password))
(def fp-jdbc-driver-class (env :fp-db-driver-class))
(def fp-jdbc-subprotocol  (env :fp-jdbc-subprotocol))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Email config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-smtp-host (env :fp-smtp-host))
(alter-var-root (var usercore/*smtp-server-host*) (fn [_] fp-smtp-host))
(def fp-verification-email-subject-line "Welcome to Gas Jot! (please verify your account)")
(def fp-verification-email-from "Gas Jot <support@jotyourself.com>")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mustache templates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-verification-email-mustache-template "email/templates/welcome-and-verify.html.mustache")
(def fp-verified-mustache-template "web/templates/account-verified.html.mustache")
(def fp-error-mustache-template "web/templates/error.html.mustache")

(defn fp-verification-url-maker
  [user-id verification-token]
  (str fp-base-url
       fp-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       user-id
       "/"
       usermeta/pathcomp-verification
       "/"
       verification-token))

(defn fp-flagged-url-maker
  [user-id verification-token]
  (str fp-base-url
       fp-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       user-id
       "/"
       usermeta/pathcomp-flagged
       "/"
       verification-token))

(delivery-mode! :smtp)

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
