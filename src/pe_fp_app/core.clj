(ns pe-fp-app.core
  (:require [clojure.data.json :as json]
            [liberator.dev :refer [wrap-trace]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes ANY]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.handler :as handler]
            [pe-fp-app.config :as config]
            [pe-user-rest.utils :as userresutils]
            [pe-user-rest.meta :as usermeta]
            [pe-user-rest.resource.users-res :as userres]
            [pe-user-rest.resource.version.users-res-v001]
            [pe-user-rest.resource.login-res :as loginres]
            [pe-user-rest.resource.version.login-res-v001]
            [pe-fp-rest.meta :as fpmeta]
            [pe-fp-rest.resource.vehicle.vehicles-res :as vehsres]
            [pe-fp-rest.resource.vehicle.version.vehicles-res-v001]
            [pe-fp-rest.resource.vehicle.vehicle-res :as vehres]
            [pe-fp-rest.resource.vehicle.version.vehicle-res-v001]
            [pe-fp-rest.resource.fuelstation.fuelstations-res :as fssres]
            [pe-fp-rest.resource.fuelstation.version.fuelstations-res-v001]
            [pe-fp-rest.resource.fuelstation.fuelstation-res :as fsres]
            [pe-fp-rest.resource.fuelstation.version.fuelstation-res-v001]
            [pe-fp-rest.resource.fplog.fplogs-res :as fplogsres]
            [pe-fp-rest.resource.fplog.version.fplogs-res-v001]
            [pe-fp-rest.resource.fplog.fplog-res :as fplogres]
            [pe-fp-rest.resource.fplog.version.fplog-res-v001]
            [pe-fp-rest.resource.envlog.envlogs-res :as envlogsres]
            [pe-fp-rest.resource.envlog.version.envlogs-res-v001]
            [pe-fp-rest.resource.envlog.envlog-res :as envlogres]
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
(def users-uri-template
  (format "%s%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users))

(def login-uri-template
  (format "%s%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-login))

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
                           #(-> % (dissoc :fpvehicle/user))
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
                           #(-> % (dissoc :fpfuelstation/user))
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
                           (fn [fplog]
                             (let [vehicle-id (:db/id (:fplog/vehicle fplog))
                                   fuelstation-id (:db/id (:fplog/fuelstation fplog))]
                               (-> fplog
                                   (dissoc :fplog/user)
                                   (assoc :fplog/vehicle (make-user-subentity-url user-id
                                                                                  fpmeta/pathcomp-vehicles
                                                                                  vehicle-id))
                                   (assoc :fplog/fuelstation (make-user-subentity-url user-id
                                                                                      fpmeta/pathcomp-fuelstations
                                                                                      fuelstation-id)))))
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
                           (fn [envlog]
                             (let [vehicle-id (:db/id (:fpenvironmentlog/vehicle envlog))]
                               (-> envlog
                                   (dissoc :fpenvironmentlog/user)
                                   (assoc :fpenvironmentlog/vehicle (make-user-subentity-url user-id
                                                                                             fpmeta/pathcomp-vehicles
                                                                                             vehicle-id)))))
                           conn
                           envlog
                           version
                           format-ind))

(defn user-embedded-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   conn
   accept-format-ind
   user-id]
  (let [vehicles (fpcore/vehicles-for-user conn user-id)
        fuelstations (fpcore/fuelstations-for-user conn user-id)
        fplogs (fpcore/fplogs-for-user conn user-id)
        envlogs (fpcore/envlogs-for-user conn user-id)]
    (vec (concat (map (fn [[veh-id vehicle]]
                        (embedded-vehicle user-id
                                          veh-id
                                          conn
                                          vehicle
                                          version
                                          accept-format-ind))
                      vehicles)
                 (map (fn [[fuelstation-id fuelstation]]
                        (embedded-fuelstation user-id
                                              fuelstation-id
                                              conn
                                              fuelstation
                                              version
                                              accept-format-ind))
                      fuelstations)
                 (map (fn [[fplog-id fplog]]
                        (embedded-fplog user-id
                                        fplog-id
                                        conn
                                        fplog
                                        version
                                        accept-format-ind))
                      fplogs)
                 (map (fn [[envlog-id envlog]]
                        (embedded-envlog user-id
                                         envlog-id
                                         conn
                                         envlog
                                         version
                                         accept-format-ind))
                      envlogs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defroutes fp-routes
  (ANY users-uri-template
       []
       (userres/users-res config/db-spec
                          config/fp-mt-subtype-prefix
                          config/fphdr-auth-token
                          config/fphdr-error-mask
                          config/fp-base-url
                          config/fp-entity-uri-prefix
                          config/fphdr-establish-session
                          nil
                          user-links-fn))
  (ANY login-uri-template
       []
       (loginres/login-res config/db-spec
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           user-embedded-fn
                           user-links-fn))
  #_(ANY changelog-uri-template
       [user-id]
       (letfn [(mt-fn-maker [mt-subtype-fn]
                 (fn [version accept-format-ind]
                   (rucore/media-type rumeta/mt-type
                                      (mt-subtype-fn config/fp-mt-subtype-prefix)
                                      version
                                      accept-format-ind)))
               (loc-fn-maker [pathcomp]
                 (fn [id]
                   (make-user-subentity-url user-id pathcomp id)))]
         (let [user-id-l (Long. user-id)]
           (clres/changelog-res config/db-spec
                                config/fp-mt-subtype-prefix
                                config/fphdr-auth-token
                                config/fphdr-error-mask
                                config/fp-base-url
                                config/fp-entity-uri-prefix
                                (fn [ctx] (userresutils/authorized? ctx
                                                                    config/db-spec
                                                                    user-id-l
                                                                    config/fp-auth-scheme
                                                                    config/fp-auth-scheme-param-name))
                                user-id-l
                                [[user-id-l
                                  :user/hashed-password
                                  (mt-fn-maker usermeta/mt-subtype-user)
                                  (fn [user-id] (rucore/make-abs-link-href config/fp-base-url
                                                                              (str config/fp-entity-uri-prefix
                                                                                   usermeta/pathcomp-users
                                                                                   "/"
                                                                                   user-id)))
                                  user-links-fn
                                  [:user/auth-token
                                   :user/hashed-password
                                   :user/password]]
                                 [:fpvehicle/user
                                  user-id-l
                                  (mt-fn-maker fpmeta/mt-subtype-vehicle)
                                  (loc-fn-maker fpmeta/pathcomp-vehicles)
                                  nil
                                  [:fpvehicle/user]]
                                 [:fpfuelstation/user
                                  user-id-l
                                  (mt-fn-maker fpmeta/mt-subtype-fuelstation)
                                  (loc-fn-maker fpmeta/pathcomp-fuelstations)
                                  nil
                                  [:fpfuelstation/user]]
                                 [:fplog/user
                                  user-id-l
                                  (mt-fn-maker fpmeta/mt-subtype-fplog)
                                  (loc-fn-maker fpmeta/pathcomp-fuelpurchase-logs)
                                  nil
                                  [:fplog/user]]
                                 [:fpenvironmentlog/user
                                  user-id-l
                                  (mt-fn-maker fpmeta/mt-subtype-envlog)
                                  (loc-fn-maker fpmeta/pathcomp-environment-logs)
                                  nil
                                  [:fpenvironmentlog/user]]]
                                fpapptxn/fpapptxn-changelog-fetch
                                fpapptxn/fpapptxnlog-fetchclsince-remote-proc-started
                                fpapptxn/fpapptxnlog-fetchclsince-remote-proc-done-success
                                fpapptxn/fpapptxnlog-fetchclsince-remote-proc-done-err-occurred
                                apptxnres/apptxn-async-logger
                                apptxnres/make-apptxn))))
  (ANY vehicles-uri-template
       [user-id]
       (vehsres/vehicles-res config/db-spec
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             (Long. user-id)
                             nil
                             nil))
  (ANY vehicle-uri-template
       [user-id vehicle-id]
       (vehres/vehicle-res config/db-spec
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-auth-scheme
                           config/fp-auth-scheme-param-name
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           (Long. user-id)
                           (Long. vehicle-id)
                           nil
                           nil))
  (ANY fuelstations-uri-template
       [user-id]
       (fssres/fuelstations-res config/db-spec
                                config/fp-mt-subtype-prefix
                                config/fphdr-auth-token
                                config/fphdr-error-mask
                                config/fp-auth-scheme
                                config/fp-auth-scheme-param-name
                                config/fp-base-url
                                config/fp-entity-uri-prefix
                                (Long. user-id)
                                nil
                                nil))
  (ANY fuelstation-uri-template
       [user-id fuelstation-id]
       (fsres/fuelstation-res config/db-spec
                              config/fp-mt-subtype-prefix
                              config/fphdr-auth-token
                              config/fphdr-error-mask
                              config/fp-auth-scheme
                              config/fp-auth-scheme-param-name
                              config/fp-base-url
                              config/fp-entity-uri-prefix
                              (Long. user-id)
                              (Long. fuelstation-id)
                              nil
                              nil))
  (ANY envlogs-uri-template
       [user-id]
       (envlogsres/envlogs-res config/db-spec
                               config/fp-mt-subtype-prefix
                               config/fphdr-auth-token
                               config/fphdr-error-mask
                               config/fp-auth-scheme
                               config/fp-auth-scheme-param-name
                               config/fp-base-url
                               config/fp-entity-uri-prefix
                               (Long. user-id)
                               nil
                               nil))
  (ANY envlog-uri-template
       [user-id envlog-id]
       (envlogres/envlog-res config/db-spec
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             (Long. user-id)
                             (Long. envlog-id)
                             nil
                             nil))
  (ANY fplogs-uri-template
       [user-id]
       (fplogsres/fplogs-res config/db-spec
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             (Long. user-id)
                             nil
                             nil))
  (ANY fplog-uri-template
       [user-id fplog-id]
       (fplogres/fplog-res config/db-spec
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-auth-scheme
                           config/fp-auth-scheme-param-name
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           (Long. user-id)
                           (Long. fplog-id)
                           nil
                           nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Middleware-decorated app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-app
  (-> fp-routes
      (handler/api)
      (wrap-trace :header)
      (wrap-params)
      (wrap-cookies)))
