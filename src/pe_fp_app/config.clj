(ns pe-fp-app.config
  (:require [environ.core :refer [env]]
            [ring.util.codec :refer [url-encode]]
            [clojure.java.jdbc :as j]
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
(def fp-support-email-address "Gas Jot <support@jotyourself.com>")
(delivery-mode! :smtp)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Account verification related
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-welcome-and-verification-email-mustache-template "email/templates/welcome-and-account-verification.html.mustache")
(def fp-verification-email-mustache-template             "email/templates/account-verification.html.mustache")
(def fp-welcome-and-verification-email-subject-line "Welcome to Gas Jot! [please verify your account]")
(def fp-verification-email-subject-line "Gas Jot [account verification]")

(def err-notification-mustache-template "email/templates/err-notification.html.mustache")
(def err-subject    (env :fp-err-notification-subject))
(def err-from-email (env :fp-err-notification-from-email))
(def err-to-email   (env :fp-err-notification-to-email))

(def new-user-notification-mustache-template "email/templates/new-signup-notification.html.mustache")
(def new-user-notification-from-email (env :fp-new-user-notification-from-email))
(def new-user-notification-to-email   (env :fp-new-user-notification-to-email))
(def new-user-notification-subject    (env :fp-new-user-notification-subject))

(defn fp-verification-url-maker
  [email verification-token]
  (str fp-base-url
       fp-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-verification
       "/"
       verification-token))

(defn fp-verification-flagged-url-maker
  [email verification-token]
  (str fp-base-url
       fp-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-verification-flagged
       "/"
       verification-token))

(def fp-verification-success-mustache-template "web/templates/verification-success.html.mustache")
(def fp-verification-error-mustache-template   "web/templates/verification-error.html.mustache")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Password reset related
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-password-reset-email-subject-line "Gas Jot [password reset]")
(def fp-password-reset-email-mustache-template "email/templates/password-reset.html.mustache")

(defn fp-prepare-password-reset-url-maker
  [email password-reset-token]
  (str fp-base-url
       fp-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-prepare-password-reset
       "/"
       password-reset-token))

(defn fp-password-reset-form-action-maker
  [email password-reset-token]
  (str fp-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-password-reset
       "/"
       password-reset-token))

(defn fp-password-reset-flagged-url-maker
  [email password-reset-token]
  (str fp-base-url
       fp-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-password-reset-flagged
       "/"
       password-reset-token))

(def fp-password-reset-form-mustache-template    "web/templates/password-reset-form.html.mustache")
(def fp-password-reset-success-mustache-template "web/templates/password-reset-success.html.mustache")
(def fp-password-reset-error-mustache-template   "web/templates/password-reset-error.html.mustache")

(defn db-spec-fn
  ([]
   (db-spec-fn nil))
  ([db-name]
   (let [subname-prefix (format "//%s:%s/" fp-db-server-host fp-db-server-port)
         db-spec {:classname fp-jdbc-driver-class
                  :subprotocol fp-jdbc-subprotocol
                  :subname (if db-name
                             (str subname-prefix db-name)
                             subname-prefix)
                  :user fp-db-username
                  :password fp-db-password}]
     (j/with-db-connection [con-db db-spec]
       (let [jdbc-conn (:connection con-db)]
         (.addDataType jdbc-conn "geometry" org.postgis.PGgeometry)))
     db-spec)))

(def db-spec-without-db (db-spec-fn nil))
(def db-spec (db-spec-fn fp-db-name))
