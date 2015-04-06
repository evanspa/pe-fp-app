(ns pe-fp-app.core
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer (pprint)]
            [datomic.api :refer [q db] :as d]
            [compojure.core :refer [defroutes ANY]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [compojure.handler :as handler]
            [pe-fp-app.config :as config]
            [pe-datomic-utils.core :as ducore]
            [pe-apptxn-core.core :as apptxncore]
            [pe-apptxn-restsupport.resource-support :as apptxnres]
            [pe-apptxn-restsupport.meta :as apptxnmeta]
            [pe-apptxn-restsupport.version.resource-support-v001]
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

(def apptxnset-uri-template
  (format "%s%s/:user-entid/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          apptxnmeta/pathcomp-apptxnset))

(def vehicles-uri-template
  (format "%s%s/:user-entid/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-vehicles))

(def vehicle-uri-template
  (format "%s%s/:user-entid/%s/:vehicle-entid"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-vehicles))

(def fuelstations-uri-template
  (format "%s%s/:user-entid/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-fuelstations))

(def fuelstation-uri-template
  (format "%s%s/:user-entid/%s/:fuelstation-entid"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-fuelstations))

(def envlogs-uri-template
  (format "%s%s/:user-entid/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-environment-logs))

(def envlog-uri-template
  (format "%s%s/:user-entid/%s/:envlog-entid"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-environment-logs))

(def fplogs-uri-template
  (format "%s%s/:user-entid/%s"
          config/fp-entity-uri-prefix
          usermeta/pathcomp-users
          meta/pathcomp-fuelpurchase-logs))

(def fplog-uri-template
  (format "%s%s/:user-entid/%s/:fplog-entid"
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
   user-entid]
  (let [link-fn (fn [rel mt-subtype-fn path-comp]
                  (rucore/make-abs-link version
                                        rel
                                        (mt-subtype-fn config/fp-mt-subtype-prefix)
                                        base-url
                                        (str config/fp-entity-uri-prefix
                                             usermeta/pathcomp-users
                                             "/"
                                             user-entid
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
        (rucore/assoc-link (link-fn apptxnmeta/apptxnset-relation
                                    apptxnmeta/mt-subtype-apptxnset
                                    apptxnmeta/pathcomp-apptxnset)))))

(defn make-user-subentity-url
  [user-entid pathcomp-subent sub-entid]
  (rucore/make-abs-link-href config/fp-base-url
                             (str config/fp-entity-uri-prefix
                                  usermeta/pathcomp-users
                                  "/"
                                  user-entid
                                  "/"
                                  pathcomp-subent
                                  "/"
                                  sub-entid)))

(defn embedded-user-subentity
  [user-entid
   sub-entid
   sub-ent-attr
   mt-subtype-fn
   pathcomp-subent
   payload-transform-fn
   conn
   entity
   version
   format-ind]
  (let [subent-txn-time (ducore/txn-time conn sub-entid sub-ent-attr)
        subent-txn-time-str (ucore/instant->rfc7231str subent-txn-time)]
    {:media-type (rucore/media-type rumeta/mt-type
                                    (mt-subtype-fn config/fp-mt-subtype-prefix)
                                    version
                                    format-ind)
     :location (make-user-subentity-url user-entid pathcomp-subent sub-entid)
     :last-modified subent-txn-time-str
     :payload (payload-transform-fn entity)}))

(defn embedded-vehicle
  [user-entid
   vehicle-entid
   conn
   vehicle
   version
   format-ind]
  (embedded-user-subentity user-entid
                           vehicle-entid
                           :fpvehicle/name
                           fpmeta/mt-subtype-vehicle
                           fpmeta/pathcomp-vehicles
                           #(-> % (dissoc :fpvehicle/user))
                           conn
                           vehicle
                           version
                           format-ind))

(defn embedded-fuelstation
  [user-entid
   fuelstation-entid
   conn
   fuelstation
   version
   format-ind]
  (embedded-user-subentity user-entid
                           fuelstation-entid
                           :fpfuelstation/name
                           fpmeta/mt-subtype-fuelstation
                           fpmeta/pathcomp-fuelstations
                           #(-> % (dissoc :fpfuelstation/user))
                           conn
                           fuelstation
                           version
                           format-ind))

(defn embedded-fplog
  [user-entid
   fplog-entid
   conn
   fplog
   version
   format-ind]
  (embedded-user-subentity user-entid
                           fplog-entid
                           :fpfuelpurchaselog/purchase-date
                           fpmeta/mt-subtype-fplog
                           fpmeta/pathcomp-fuelpurchase-logs
                           (fn [fplog]
                             (let [vehicle-entid (:db/id (:fpfuelpurchaselog/vehicle fplog))
                                   fuelstation-entid (:db/id (:fpfuelpurchaselog/fuelstation fplog))]
                               (-> fplog
                                   (dissoc :fpfuelpurchaselog/user)
                                   (assoc :fpfuelpurchaselog/vehicle (make-user-subentity-url user-entid
                                                                                              fpmeta/pathcomp-vehicles
                                                                                              vehicle-entid))
                                   (assoc :fpfuelpurchaselog/fuelstation (make-user-subentity-url user-entid
                                                                                                  fpmeta/pathcomp-fuelstations
                                                                                                  fuelstation-entid)))))
                           conn
                           fplog
                           version
                           format-ind))

(defn embedded-envlog
  [user-entid
   envlog-entid
   conn
   envlog
   version
   format-ind]
  (embedded-user-subentity user-entid
                           envlog-entid
                           :fpenvironmentlog/log-date
                           fpmeta/mt-subtype-envlog
                           fpmeta/pathcomp-environment-logs
                           (fn [envlog]
                             (let [vehicle-entid (:db/id (:fpenvironmentlog/vehicle envlog))]
                               (-> envlog
                                   (dissoc :fpenvironmentlog/user)
                                   (assoc :fpenvironmentlog/vehicle (make-user-subentity-url user-entid
                                                                                             fpmeta/pathcomp-vehicles
                                                                                             vehicle-entid)))))
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
   user-entid]
  (let [vehicles (fpcore/vehicles-for-user conn user-entid)
        fuelstations (fpcore/fuelstations-for-user conn user-entid)
        fplogs (fpcore/fplogs-for-user conn user-entid)
        envlogs (fpcore/envlogs-for-user conn user-entid)]
    (vec (concat (map (fn [[veh-entid vehicle]]
                        (embedded-vehicle user-entid
                                          veh-entid
                                          conn
                                          vehicle
                                          version
                                          accept-format-ind))
                      vehicles)
                 (map (fn [[fuelstation-entid fuelstation]]
                        (embedded-fuelstation user-entid
                                              fuelstation-entid
                                              conn
                                              fuelstation
                                              version
                                              accept-format-ind))
                      fuelstations)
                 (map (fn [[fplog-entid fplog]]
                        (embedded-fplog user-entid
                                        fplog-entid
                                        conn
                                        fplog
                                        version
                                        accept-format-ind))
                      fplogs)
                 (map (fn [[envlog-entid envlog]]
                        (embedded-envlog user-entid
                                         envlog-entid
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
       (userres/users-res @config/conn
                          config/fp-partition
                          config/fp-apptxn-partition
                          config/fp-mt-subtype-prefix
                          config/fphdr-auth-token
                          config/fphdr-error-mask
                          config/fp-base-url
                          config/fp-entity-uri-prefix
                          config/fphdr-apptxn-id
                          config/fphdr-useragent-device-make
                          config/fphdr-useragent-device-os
                          config/fphdr-useragent-device-os-version
                          config/fphdr-establish-session
                          nil
                          user-links-fn))
  (ANY login-uri-template
       []
       (loginres/login-res @config/conn
                           config/fp-partition
                           config/fp-apptxn-partition
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           config/fphdr-apptxn-id
                           config/fphdr-useragent-device-make
                           config/fphdr-useragent-device-os
                           config/fphdr-useragent-device-os-version
                           user-embedded-fn
                           user-links-fn))
  (ANY apptxnset-uri-template
       [user-entid]
       (apptxnres/apptxnset-res @config/conn
                                config/fp-apptxn-partition
                                config/fp-mt-subtype-prefix
                                config/fphdr-auth-token
                                config/fphdr-error-mask
                                config/fp-base-url
                                config/fp-entity-uri-prefix
                                config/fphdr-apptxn-id
                                config/fphdr-useragent-device-make
                                config/fphdr-useragent-device-os
                                config/fphdr-useragent-device-os-version
                                (fn [ctx] (userresutils/authorized? ctx
                                                                    @config/conn
                                                                    (Long. user-entid)
                                                                    config/fp-auth-scheme
                                                                    config/fp-auth-scheme-param-name))))
  (ANY vehicles-uri-template
       [user-entid]
       (vehsres/vehicles-res @config/conn
                             config/fp-partition
                             config/fp-apptxn-partition
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             config/fphdr-apptxn-id
                             config/fphdr-useragent-device-make
                             config/fphdr-useragent-device-os
                             config/fphdr-useragent-device-os-version
                             (Long. user-entid)
                             nil
                             nil))
  (ANY vehicle-uri-template
       [user-entid vehicle-entid]
       (vehres/vehicle-res @config/conn
                           config/fp-partition
                           config/fp-apptxn-partition
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-auth-scheme
                           config/fp-auth-scheme-param-name
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           config/fphdr-apptxn-id
                           config/fphdr-useragent-device-make
                           config/fphdr-useragent-device-os
                           config/fphdr-useragent-device-os-version
                           (Long. user-entid)
                           (Long. vehicle-entid)
                           nil
                           nil))
  (ANY fuelstations-uri-template
       [user-entid]
       (fssres/fuelstations-res @config/conn
                                config/fp-partition
                                config/fp-apptxn-partition
                                config/fp-mt-subtype-prefix
                                config/fphdr-auth-token
                                config/fphdr-error-mask
                                config/fp-auth-scheme
                                config/fp-auth-scheme-param-name
                                config/fp-base-url
                                config/fp-entity-uri-prefix
                                config/fphdr-apptxn-id
                                config/fphdr-useragent-device-make
                                config/fphdr-useragent-device-os
                                config/fphdr-useragent-device-os-version
                                (Long. user-entid)
                                nil
                                nil))
  (ANY fuelstation-uri-template
       [user-entid fuelstation-entid]
       (fsres/fuelstation-res @config/conn
                              config/fp-partition
                              config/fp-apptxn-partition
                              config/fp-mt-subtype-prefix
                              config/fphdr-auth-token
                              config/fphdr-error-mask
                              config/fp-auth-scheme
                              config/fp-auth-scheme-param-name
                              config/fp-base-url
                              config/fp-entity-uri-prefix
                              config/fphdr-apptxn-id
                              config/fphdr-useragent-device-make
                              config/fphdr-useragent-device-os
                              config/fphdr-useragent-device-os-version
                              (Long. user-entid)
                              (Long. fuelstation-entid)
                              nil
                              nil))
  (ANY envlogs-uri-template
       [user-entid]
       (envlogsres/envlogs-res @config/conn
                               config/fp-partition
                               config/fp-apptxn-partition
                               config/fp-mt-subtype-prefix
                               config/fphdr-auth-token
                               config/fphdr-error-mask
                               config/fp-auth-scheme
                               config/fp-auth-scheme-param-name
                               config/fp-base-url
                               config/fp-entity-uri-prefix
                               config/fphdr-apptxn-id
                               config/fphdr-useragent-device-make
                               config/fphdr-useragent-device-os
                               config/fphdr-useragent-device-os-version
                               (Long. user-entid)
                               nil
                               nil))
  (ANY envlog-uri-template
       [user-entid envlog-entid]
       (envlogres/envlog-res @config/conn
                             config/fp-partition
                             config/fp-apptxn-partition
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             config/fphdr-apptxn-id
                             config/fphdr-useragent-device-make
                             config/fphdr-useragent-device-os
                             config/fphdr-useragent-device-os-version
                             (Long. user-entid)
                             (Long. envlog-entid)
                             nil
                             nil))
  (ANY fplogs-uri-template
       [user-entid]
       (fplogsres/fplogs-res @config/conn
                             config/fp-partition
                             config/fp-apptxn-partition
                             config/fp-mt-subtype-prefix
                             config/fphdr-auth-token
                             config/fphdr-error-mask
                             config/fp-auth-scheme
                             config/fp-auth-scheme-param-name
                             config/fp-base-url
                             config/fp-entity-uri-prefix
                             config/fphdr-apptxn-id
                             config/fphdr-useragent-device-make
                             config/fphdr-useragent-device-os
                             config/fphdr-useragent-device-os-version
                             (Long. user-entid)
                             nil
                             nil))
  (ANY fplog-uri-template
       [user-entid fplog-entid]
       (fplogres/fplog-res @config/conn
                           config/fp-partition
                           config/fp-apptxn-partition
                           config/fp-mt-subtype-prefix
                           config/fphdr-auth-token
                           config/fphdr-error-mask
                           config/fp-auth-scheme
                           config/fp-auth-scheme-param-name
                           config/fp-base-url
                           config/fp-entity-uri-prefix
                           config/fphdr-apptxn-id
                           config/fphdr-useragent-device-make
                           config/fphdr-useragent-device-os
                           config/fphdr-useragent-device-os-version
                           (Long. user-entid)
                           (Long. fplog-entid)
                           nil
                           nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Middleware-decorated app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-app
  (-> fp-routes
      (handler/api)
      (wrap-cookies)))
