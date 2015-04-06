(ns pe-fp-app.config
  (:require [environ.core :refer [env]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Environment-controlled configuration values
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-datomic-url (env :fp-datomic-url))

(def fp-base-url (env :fp-base-url))

(def fp-nrepl-server-port (env :fp-nrepl-server-port))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Datomic-related configuration values
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-app-schema-sets
  [
   ["apptxn-logging-schema-updates-0.0.1.dtm" ; initial release of application schema set
    "user-schema-updates-0.0.1.dtm"
    "fp-schema-updates-0.0.1.dtm"]
   ])

(def fp-partition :fp)

(def fp-apptxn-partition :fp-apptxn)

(def fp-app-schemaset-version-attr :fp-app-schemaset-version)

(def conn
  "Datomic connection object."
  (atom nil))

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

(def fphdr-apptxn-id "fp-apptxn-id")

(def fphdr-useragent-device-make "fp-useragent-device-make")

(def fphdr-useragent-device-os "fp-useragent-device-os")

(def fphdr-useragent-device-os-version "fp-useragent-device-os-version")
