(ns pe-fp-app.core
  (:require [clojure.data.json :as json]
            [liberator.dev :refer [wrap-trace]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [ring.util.codec :refer [url-decode]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes ANY]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.handler :as handler]
            [pe-fp-app.config :as config]
            [pe-user-core.ddl :as userddl]
            [pe-user-core.core :as usercore]
            [pe-user-rest.utils :as userresutils]
            [pe-user-rest.meta :as usermeta]
            [pe-fp-rest.resource.price-stream.price-stream-res :as pricestreamres]
            [pe-fp-rest.resource.price-stream.version.price-stream-res-v001]
            [pe-user-rest.resource.users-res :as usersres]
            [pe-user-rest.resource.version.users-res-v001]
            [pe-user-rest.resource.user-res :as userres]
            [pe-user-rest.resource.version.user-res-v001]
            [pe-user-rest.resource.login-res :as loginres]
            [pe-user-rest.resource.version.login-res-v001]
            [pe-user-rest.resource.logout-res :as logoutres]
            [pe-user-rest.resource.version.logout-res-v001]
            [pe-user-rest.resource.send-verification-email-res :as sendveriemailres]
            [pe-user-rest.resource.version.send-verification-email-res-v001]
            [pe-user-rest.resource.account-verification-res :as verificationres]
            [pe-user-rest.resource.send-password-reset-email-res :as sendpwdresetemailres]
            [pe-user-rest.resource.version.send-password-reset-email-res-v001]
            [pe-user-rest.resource.prepare-password-reset-res :as preparepwdresetres]
            [pe-user-rest.resource.password-reset-res :as pwdresetres]
            [pe-fp-core.ddl :as fpddl]
            [pe-fp-rest.meta :as fpmeta]
            [pe-fp-rest.resource.vehicle.vehicles-res :as vehsres]
            [pe-fp-rest.resource.vehicle.version.vehicles-res-v001]
            [pe-fp-rest.resource.vehicle.vehicle-res :as vehres]
            [pe-fp-rest.resource.vehicle.vehicle-utils :as vehresutils]
            [pe-fp-rest.resource.vehicle.version.vehicle-res-v001]
            [pe-fp-rest.resource.fuelstation.fuelstations-res :as fssres]
            [pe-fp-rest.resource.fuelstation.fuelstation-utils :as fsresutils]
            [pe-fp-rest.resource.fuelstation.version.fuelstations-res-v001]
            [pe-fp-rest.resource.fuelstation.fuelstation-res :as fsres]
            [pe-fp-rest.resource.fuelstation.version.fuelstation-res-v001]
            [pe-fp-rest.resource.fplog.fplogs-res :as fplogsres]
            [pe-fp-rest.resource.fplog.version.fplogs-res-v001]
            [pe-fp-rest.resource.fplog.fplog-res :as fplogres]
            [pe-fp-rest.resource.fplog.fplog-utils :as fplogresutils]
            [pe-fp-rest.resource.fplog.version.fplog-res-v001]
            [pe-fp-rest.resource.envlog.envlogs-res :as envlogsres]
            [pe-fp-rest.resource.envlog.version.envlogs-res-v001]
            [pe-fp-rest.resource.envlog.envlog-res :as envlogres]
            [pe-fp-rest.resource.envlog.envlog-utils :as envlogresutils]
            [pe-fp-rest.resource.envlog.version.envlog-res-v001]
            [pe-fp-rest.meta :as meta]
            [pe-fp-core.core :as fpcore]
            [pe-core-utils.core :as ucore]
            [pe-rest-utils.core :as rucore]
            [pe-rest-utils.meta :as rumeta]
            [pe-rest-utils.changelog.meta :as clmeta]
            [pe-rest-utils.changelog.resource-support :as clres]
            [pe-rest-utils.changelog.version.resource-support-v001]
            [environ.core :refer [env]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; URL templates for routing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def price-stream-uri-template
  (format "%s%s"
          config/fp-entity-uri-prefix
          meta/pathcomp-price-stream))

(def users-uri-template
  (format "%s%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users))

(def verification-uri-template
  (format "%s%s/:email/%s/:verification-token"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-verification))

(def login-uri-template
  (format "%s%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-login))

(def light-login-uri-template
  (format "%s%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-light-login))

(def logout-uri-template
  (format "%s%s/:user-id/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-logout))

(def send-verification-email-uri-template
  (format "%s%s/:user-id/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-send-verification-email))

(def send-password-reset-email-uri-template
  (format "%s%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-send-password-reset-email))

(def prepare-password-reset-uri-template
  (format "%s%s/:email/%s/:password-reset-token"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-prepare-password-reset))

(def password-reset-uri-template
  (format "%s%s/:email/%s/:password-reset-token"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-password-reset))

(def user-uri-template
  (format "%s%s/:user-id"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users))

(def changelog-uri-template
  (format "%s%s/:user-id/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          clmeta/pathcomp-changelog))

(def vehicles-uri-template
  (format "%s%s/:user-id/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-vehicles))

(def vehicle-uri-template
  (format "%s%s/:user-id/%s/:vehicle-id"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-vehicles))

(def fuelstations-uri-template
  (format "%s%s/:user-id/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-fuelstations))

(def fuelstation-uri-template
  (format "%s%s/:user-id/%s/:fuelstation-id"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-fuelstations))

(def envlogs-uri-template
  (format "%s%s/:user-id/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-environment-logs))

(def envlog-uri-template
  (format "%s%s/:user-id/%s/:envlog-id"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-environment-logs))

(def fplogs-uri-template
  (format "%s%s/:user-id/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-fuelpurchase-logs))

(def fplog-uri-template
  (format "%s%s/:user-id/%s/:fplog-id"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-fuelpurchase-logs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Link and Embedded-resources functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn user-links-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   user-id]
  (let [link-fn (fn [rel mt-subtype-fn path-comp]
                  (rucore/make-abs-link version
                                        rel
                                        (mt-subtype-fn config/fp-mt-subtype-prefix)
                                        base-url
                                        (str config/fp-entity-uri-prefix
                                             usermeta/pathcomp-users
                                             "/"
                                             user-id
                                             "/"
                                             path-comp)))]
    (-> {}
        (rucore/assoc-link (link-fn usermeta/logout-relation
                                    usermeta/mt-subtype-user
                                    usermeta/pathcomp-logout))
        (rucore/assoc-link (link-fn usermeta/send-verification-email-relation
                                    usermeta/mt-subtype-user
                                    usermeta/pathcomp-send-verification-email))
        (rucore/assoc-link (link-fn usermeta/send-password-reset-email-relation
                                    usermeta/mt-subtype-user
                                    usermeta/pathcomp-send-password-reset-email))
        (rucore/assoc-link (link-fn fpmeta/fp-vehicles-relation
                                    fpmeta/mt-subtype-vehicle
                                    fpmeta/pathcomp-vehicles))
        (rucore/assoc-link (link-fn fpmeta/fp-fuelstations-relation
                                    fpmeta/mt-subtype-fuelstation
                                    fpmeta/pathcomp-fuelstations))
        (rucore/assoc-link (link-fn fpmeta/fp-envlogs-relation
                                    fpmeta/mt-subtype-envlog
                                    fpmeta/pathcomp-environment-logs))
        (rucore/assoc-link (link-fn fpmeta/fp-fplogs-relation
                                    fpmeta/mt-subtype-fplog
                                    fpmeta/pathcomp-fuelpurchase-logs))
        (rucore/assoc-link (link-fn clmeta/changelog-relation
                                    clmeta/mt-subtype-changelog
                                    clmeta/pathcomp-changelog)))))

(defn make-user-subentity-url
  [user-id pathcomp-subent sub-id]
  (rucore/make-abs-link-href config/fp-base-url
                             (str config/fp-entity-uri-prefix
                                  usermeta/pathcomp-users
                                  "/"
                                  user-id
                                  "/"
                                  pathcomp-subent
                                  "/"
                                  sub-id)))

(defn embedded-user-subentity
  [user-id
   sub-id
   sub-ent-attr
   mt-subtype-fn
   pathcomp-subent
   payload-transform-fn
   conn
   entity
   version
   format-ind]
  {:media-type (rucore/media-type rumeta/mt-type
                                  (mt-subtype-fn config/fp-mt-subtype-prefix)
                                  version
                                  format-ind)
   :location (make-user-subentity-url user-id pathcomp-subent sub-id)
   :payload (-> entity
                (payload-transform-fn))})

(defn embedded-vehicle
  [user-id
   vehicle-id
   conn
   vehicle
   version
   format-ind]
  (embedded-user-subentity user-id
                           vehicle-id
                           :fpvehicle/name
                           fpmeta/mt-subtype-vehicle
                           fpmeta/pathcomp-vehicles
                           #(vehresutils/vehicle-out-transform %)
                           conn
                           vehicle
                           version
                           format-ind))

(defn embedded-fuelstation
  [user-id
   fuelstation-id
   conn
   fuelstation
   version
   format-ind]
  (embedded-user-subentity user-id
                           fuelstation-id
                           :fpfuelstation/name
                           fpmeta/mt-subtype-fuelstation
                           fpmeta/pathcomp-fuelstations
                           #(fsresutils/fuelstation-out-transform %)
                           conn
                           fuelstation
                           version
                           format-ind))

(defn embedded-fplog
  [user-id
   fplog-id
   conn
   fplog
   version
   format-ind]
  (embedded-user-subentity user-id
                           fplog-id
                           :fplog/purchased-at
                           fpmeta/mt-subtype-fplog
                           fpmeta/pathcomp-fuelpurchase-logs
                           #(fplogresutils/fplog-out-transform %
                                                               config/fp-base-url
                                                               config/fp-entity-uri-prefix)
                           conn
                           fplog
                           version
                           format-ind))

(defn embedded-envlog
  [user-id
   envlog-id
   conn
   envlog
   version
   format-ind]
  (embedded-user-subentity user-id
                           envlog-id
                           :fpenvironmentlog/log-date
                           fpmeta/mt-subtype-envlog
                           fpmeta/pathcomp-environment-logs
                           #(envlogresutils/envlog-out-transform %
                                                                 config/fp-base-url
                                                                 config/fp-entity-uri-prefix)
                           conn
                           envlog
                           version
                           format-ind))

(defn fp-entities->vec
  [version
   db-spec
   accept-format-ind
   user-id
   vehicles
   fuelstations
   fplogs
   envlogs]
  (vec (concat (map (fn [[veh-id vehicle]]
                        (embedded-vehicle user-id
                                          veh-id
                                          db-spec
                                          vehicle
                                          version
                                          accept-format-ind))
                      vehicles)
                 (map (fn [[fuelstation-id fuelstation]]
                        (embedded-fuelstation user-id
                                              fuelstation-id
                                              db-spec
                                              fuelstation
                                              version
                                              accept-format-ind))
                      fuelstations)
                 (map (fn [[fplog-id fplog]]
                        (embedded-fplog user-id
                                        fplog-id
                                        db-spec
                                        fplog
                                        version
                                        accept-format-ind))
                      fplogs)
                 (map (fn [[envlog-id envlog]]
                        (embedded-envlog user-id
                                         envlog-id
                                         db-spec
                                         envlog
                                         version
                                         accept-format-ind))
                      envlogs))))

(defn user-embedded-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   db-spec
   accept-format-ind
   user-id]
  (let [vehicles (fpcore/vehicles-for-user db-spec user-id)
        fuelstations (fpcore/fuelstations-for-user db-spec user-id)
        fplogs (fpcore/fplogs-for-user db-spec user-id)
        envlogs (fpcore/envlogs-for-user db-spec user-id)]
    (fp-entities->vec version
                      db-spec
                      accept-format-ind
                      user-id
                      vehicles
                      fuelstations
                      fplogs
                      envlogs)))

(defn changelog-embedded-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   db-spec
   accept-format-ind
   user-id
   modified-since]
  (let [user-result (usercore/load-user-by-id-if-modified-since db-spec user-id modified-since)
        {vehicles :entities} (fpcore/vehicles-modified-since db-spec user-id modified-since)
        {fuelstations :entities} (fpcore/fuelstations-modified-since db-spec user-id modified-since)
        {fplogs :entities} (fpcore/fplogs-modified-since db-spec user-id modified-since)
        {envlogs :entities} (fpcore/envlogs-modified-since db-spec user-id modified-since)
        fp-embedded-entities (fp-entities->vec version
                                               db-spec
                                               accept-format-ind
                                               user-id
                                               vehicles
                                               fuelstations
                                               fplogs
                                               envlogs)]
    (if (not (nil? user-result))
      (let [[_ user] user-result]
        (conj
         fp-embedded-entities
         {:media-type (rucore/media-type rumeta/mt-type
                                         (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                         version
                                         accept-format-ind)
          :location (rucore/make-abs-link-href config/fp-base-url
                                               (str config/fp-entity-uri-prefix
                                                    usermeta/pathcomp-users
                                                    "/"
                                                    user-id))
          :payload (userresutils/user-out-transform user)}))
      fp-embedded-entities)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defroutes fp-routes
  (ANY price-stream-uri-template
       []
       (pricestreamres/price-stream-res (config/pooled-db-spec)
                                        config/fp-mt-subtype-prefix
                                        config/fphdr-auth-token
                                        config/fphdr-error-mask
                                        config/fp-base-url
                                        config/fp-entity-uri-prefix
                                        config/err-notification-mustache-template
                                        config/err-subject
                                        config/err-from-email
                                        config/err-to-email
                                        config/fp-min-distance-diff-fs))
  (ANY users-uri-template
       []
       (usersres/users-res (config/pooled-db-spec)
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           config/fphdr-establish-session
                           nil
                           user-links-fn
                           config/fp-welcome-and-verification-email-mustache-template
                           config/fp-welcome-and-verification-email-subject-line
                           config/fp-support-email-address
                           config/fp-verification-url-maker
                           config/fp-verification-flagged-url-maker
                           config/new-user-notification-mustache-template
                           config/new-user-notification-from-email
                           config/new-user-notification-to-email
                           config/new-user-notification-subject
                           config/err-notification-mustache-template
                           config/err-subject
                           config/err-from-email
                           config/err-to-email))
  (ANY verification-uri-template
       [email
        verification-token]
       (verificationres/account-verification-res (config/pooled-db-spec)
                                                 config/fp-base-url
                                                 config/fp-entity-uri-prefix
                                                 email
                                                 verification-token
                                                 config/fp-verification-success-mustache-template
                                                 config/fp-verification-error-mustache-template
                                                 config/err-notification-mustache-template
                                                 config/err-subject
                                                 config/err-from-email
                                                 config/err-to-email))
  (ANY login-uri-template
       []
       (loginres/login-res (config/pooled-db-spec)
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           user-embedded-fn
                           user-links-fn
                           config/fphdr-login-failed-reason
                           config/err-notification-mustache-template
                           config/err-subject
                           config/err-from-email
                           config/err-to-email))
  (ANY light-login-uri-template
       []
       (loginres/light-login-res (config/pooled-db-spec)
                                 config/fp-mt-subtype-prefix
                                 config/fphdr-auth-token
                                 config/fphdr-error-mask
                                 config/fp-base-url
                                 config/fp-entity-uri-prefix
                                 config/fphdr-login-failed-reason
                                 config/err-notification-mustache-template
                                 config/err-subject
                                 config/err-from-email
                                 config/err-to-email))
  (ANY logout-uri-template
       [user-id]
       (logoutres/logout-res (config/pooled-db-spec)
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             (Long. user-id)
                             config/err-notification-mustache-template
                             config/err-subject
                             config/err-from-email
                             config/err-to-email))
  (ANY send-verification-email-uri-template
       [user-id]
       (sendveriemailres/send-verification-email-res (config/pooled-db-spec)
                                                     config/fp-mt-subtype-prefix
                                                     config/fphdr-auth-token
                                                     config/fphdr-error-mask
                                                     config/fp-auth-scheme
                                                     config/fp-auth-scheme-param-name
                                                     config/fp-base-url
                                                     config/fp-entity-uri-prefix
                                                     (Long. user-id)
                                                     config/fp-verification-email-mustache-template
                                                     config/fp-verification-email-subject-line
                                                     config/fp-support-email-address
                                                     config/fp-verification-url-maker
                                                     config/fp-verification-flagged-url-maker
                                                     config/err-notification-mustache-template
                                                     config/err-subject
                                                     config/err-from-email
                                                     config/err-to-email))
  (ANY send-password-reset-email-uri-template
       []
       (sendpwdresetemailres/send-password-reset-email-res (config/pooled-db-spec)
                                                           config/fp-mt-subtype-prefix
                                                           config/fphdr-error-mask
                                                           config/fp-base-url
                                                           config/fp-entity-uri-prefix
                                                           config/fp-password-reset-email-mustache-template
                                                           config/fp-password-reset-email-subject-line
                                                           config/fp-support-email-address
                                                           config/fp-prepare-password-reset-url-maker
                                                           config/fp-password-reset-flagged-url-maker
                                                           config/err-notification-mustache-template
                                                           config/err-subject
                                                           config/err-from-email
                                                           config/err-to-email))
  (ANY prepare-password-reset-uri-template
       [email
        password-reset-token]
       (preparepwdresetres/prepare-password-reset-res (config/pooled-db-spec)
                                                      config/fp-base-url
                                                      config/fp-entity-uri-prefix
                                                      (url-decode email)
                                                      password-reset-token
                                                      config/fp-password-reset-form-mustache-template
                                                      (config/fp-password-reset-form-action-maker email password-reset-token)
                                                      config/fp-password-reset-error-mustache-template
                                                      config/err-notification-mustache-template
                                                      config/err-subject
                                                      config/err-from-email
                                                      config/err-to-email))
  (ANY password-reset-uri-template
       [email
        password-reset-token]
       (pwdresetres/password-reset-res (config/pooled-db-spec)
                                       config/fp-base-url
                                       config/fp-entity-uri-prefix
                                       (url-decode email)
                                       password-reset-token
                                       config/fp-password-reset-success-mustache-template
                                       config/fp-password-reset-error-mustache-template
                                       config/err-notification-mustache-template
                                       config/err-subject
                                       config/err-from-email
                                       config/err-to-email))
  (ANY user-uri-template
       [user-id]
       (userres/user-res (config/pooled-db-spec)
                         config/fp-mt-subtype-prefix
                         config/fphdr-auth-token
                         config/fphdr-error-mask
                         config/fp-auth-scheme
                         config/fp-auth-scheme-param-name
                         config/fp-base-url
                         config/fp-entity-uri-prefix
                         (Long. user-id)
                         nil ; embedded-resources-fn
                         user-links-fn
                         config/fphdr-if-unmodified-since
                         config/fphdr-if-modified-since
                         config/fphdr-delete-reason
                         config/err-notification-mustache-template
                         config/err-subject
                         config/err-from-email
                         config/err-to-email))

  (ANY changelog-uri-template
       [user-id]
       (let [user-id-l (Long. user-id)]
         (letfn [(mt-fn-maker [mt-subtype-fn]
                   (fn [version accept-format-ind]
                     (rucore/media-type rumeta/mt-type
                                        (mt-subtype-fn config/fp-mt-subtype-prefix)
                                        version
                                        accept-format-ind)))
                 (loc-fn-maker [pathcomp]
                   (fn [id]
                     (make-user-subentity-url user-id pathcomp id)))]
           (clres/changelog-res (config/pooled-db-spec)
                                config/fp-mt-subtype-prefix
                                config/fphdr-auth-token
                                config/fphdr-error-mask
                                config/fp-auth-scheme
                                config/fp-auth-scheme-param-name
                                config/fp-base-url
                                config/fp-entity-uri-prefix
                                user-id-l
                                changelog-embedded-fn
                                nil ; links-fn
                                config/fphdr-if-modified-since
                                (fn [ctx] (userresutils/authorized? ctx
                                                                    (config/pooled-db-spec)
                                                                    user-id-l
                                                                    config/fp-auth-scheme
                                                                    config/fp-auth-scheme-param-name))
                                userresutils/get-plaintext-auth-token
                                [[userddl/tbl-user-account "id" "=" user-id-l "updated_at" "deleted_at"]
                                 [fpddl/tbl-vehicle "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                 [fpddl/tbl-fuelstation "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                 [fpddl/tbl-fplog "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                 [fpddl/tbl-envlog "user_id" "=" user-id-l "updated_at" "deleted_at"]]
                                (fn [exc-and-params]
                                  (usercore/send-email config/err-notification-mustache-template
                                                       exc-and-params
                                                       config/err-subject
                                                       config/err-from-email
                                                       config/err-to-email))))))


  (ANY vehicles-uri-template
       [user-id]
       (vehsres/vehicles-res (config/pooled-db-spec)
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             (Long. user-id)
                             nil ; embedded-resources-fn
                             nil ; links-fn
                             config/err-notification-mustache-template
                             config/err-subject
                             config/err-from-email
                             config/err-to-email))
  (ANY vehicle-uri-template
       [user-id vehicle-id]
       (vehres/vehicle-res (config/pooled-db-spec)
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-auth-scheme
                           config/fp-auth-scheme-param-name
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           (Long. user-id)
                           (Long. vehicle-id)
                           nil ; embedded-resources-fn
                           nil ; links-fn
                           config/fphdr-if-unmodified-since
                           config/fphdr-if-modified-since
                           config/err-notification-mustache-template
                           config/err-subject
                           config/err-from-email
                           config/err-to-email))
  (ANY fuelstations-uri-template
       [user-id]
       (fssres/fuelstations-res (config/pooled-db-spec)
                                config/fp-mt-subtype-prefix
                                config/fphdr-auth-token
                                config/fphdr-error-mask
                                config/fp-auth-scheme
                                config/fp-auth-scheme-param-name
                                config/fp-base-url
                                config/fp-entity-uri-prefix
                                (Long. user-id)
                                nil ; embedded-resources-fn
                                nil ; links-fn
                                config/err-notification-mustache-template
                                config/err-subject
                                config/err-from-email
                                config/err-to-email))
  (ANY fuelstation-uri-template
       [user-id fuelstation-id]
       (fsres/fuelstation-res (config/pooled-db-spec)
                              config/fp-mt-subtype-prefix
                              config/fphdr-auth-token
                              config/fphdr-error-mask
                              config/fp-auth-scheme
                              config/fp-auth-scheme-param-name
                              config/fp-base-url
                              config/fp-entity-uri-prefix
                              (Long. user-id)
                              (Long. fuelstation-id)
                              nil ; embedded-resources-fn
                              nil ; links-fn
                              config/fphdr-if-unmodified-since
                              config/fphdr-if-modified-since
                              config/err-notification-mustache-template
                              config/err-subject
                              config/err-from-email
                              config/err-to-email))
  (ANY envlogs-uri-template
       [user-id]
       (envlogsres/envlogs-res (config/pooled-db-spec)
                               config/fp-mt-subtype-prefix
                               config/fphdr-auth-token
                               config/fphdr-error-mask
                               config/fp-auth-scheme
                               config/fp-auth-scheme-param-name
                               config/fp-base-url
                               config/fp-entity-uri-prefix
                               (Long. user-id)
                               nil ; embedded-resources-fn
                               nil ; links-fn
                               config/err-notification-mustache-template
                               config/err-subject
                               config/err-from-email
                               config/err-to-email))
  (ANY envlog-uri-template
       [user-id envlog-id]
       (envlogres/envlog-res (config/pooled-db-spec)
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             (Long. user-id)
                             (Long. envlog-id)
                             nil ; embedded-resources-fn
                             nil ; links-fn
                             config/fphdr-if-unmodified-since
                             config/fphdr-if-modified-since
                             config/err-notification-mustache-template
                             config/err-subject
                             config/err-from-email
                             config/err-to-email))
  (ANY fplogs-uri-template
       [user-id]
       (fplogsres/fplogs-res (config/pooled-db-spec)
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             (Long. user-id)
                             nil ; embedded-resources-fn
                             nil ; links-fn
                             config/err-notification-mustache-template
                             config/err-subject
                             config/err-from-email
                             config/err-to-email))
  (ANY fplog-uri-template
       [user-id fplog-id]
       (fplogres/fplog-res (config/pooled-db-spec)
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-auth-scheme
                           config/fp-auth-scheme-param-name
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           (Long. user-id)
                           (Long. fplog-id)
                           nil ; embedded-resources-fn
                           nil ; links-fn
                           config/fphdr-if-unmodified-since
                           config/fphdr-if-modified-since
                           config/err-notification-mustache-template
                           config/err-subject
                           config/err-from-email
                           config/err-to-email)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Middleware-decorated app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-app
  (-> fp-routes
      (handler/api)
      ;(wrap-trace :header)
      (wrap-params)
      (wrap-cookies)))
