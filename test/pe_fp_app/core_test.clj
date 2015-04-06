(ns pe-fp-app.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer (pprint)]
            [datomic.api :refer [q db] :as d]
            [pe-fp-core.core :as fpcore]
            [pe-user-core.core :as usercore]
            [pe-fp-app.core :as core]
            [pe-fp-app.lifecycle :as lifecycle]
            [pe-fp-app.config :as config]
            [pe-rest-testutils.core :as rtucore]
            [ring.mock.request :as mock]
            [pe-core-utils.core :as ucore]
            [pe-user-rest.meta :as usermeta]
            [pe-rest-utils.core :as rucore]
            [pe-fp-rest.meta :as fpmeta]
            [pe-rest-utils.meta :as rumeta]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(use-fixtures :each (fn [f]
                      (d/delete-database config/fp-datomic-url)
                      (lifecycle/init-database)
                      (f)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest integration-test-1
  (testing "Multiple Logins of user and subsequent authenticated request."
    (is (nil? (usercore/load-user-by-email @config/conn "smithka@testing.com")))
    (is (nil? (usercore/load-user-by-username @config/conn "smithk")))
    (let [user {:user/name "Karen Smith"
                :user/email "smithka@testing.com"
                :user/password "insecure"}]
      @(d/transact @config/conn [(usercore/save-new-user-txnmap config/fp-partition user)])
      (is (not (nil? (usercore/load-user-by-email @config/conn "smithka@testing.com")))))
    ;; 1st Login
    (let [user {"user/username-or-email" "smithka@testing.com"
                "user/password" "insecure"}
          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                          (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                          usermeta/v001
                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                          "json"
                                          "en-US"
                                          :post
                                          core/login-uri-template
                                          config/fphdr-apptxn-id
                                          config/fphdr-useragent-device-make
                                          config/fphdr-useragent-device-os
                                          config/fphdr-useragent-device-os-version)
                  (mock/body (json/write-str user))
                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                          (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                                          usermeta/v001
                                                          "json"
                                                          "UTF-8")))
          resp (core/fp-app req)]
      (testing "status code" (is (= 200 (:status resp)))))

    ;; Login again (creating a 2nd auth token in the database)
    (let [user {"user/username-or-email" "smithka@testing.com"
                "user/password" "insecure"}
          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                          (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                          usermeta/v001
                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                          "json"
                                          "en-US"
                                          :post
                                          core/login-uri-template
                                          config/fphdr-apptxn-id
                                          config/fphdr-useragent-device-make
                                          config/fphdr-useragent-device-os
                                          config/fphdr-useragent-device-os-version)
                  (mock/body (json/write-str user))
                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                          (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                                          usermeta/v001
                                                          "json"
                                                          "UTF-8")))
          resp (core/fp-app req)]
      (testing "status code" (is (= 200 (:status resp))))
      (let [hdrs (:headers resp)
            resp-body-stream (:body resp)
            user-location-str (get hdrs "location")
            user-last-modified-str (get hdrs "last-modified")
            last-modified (ucore/rfc7231str->instant user-last-modified-str)
            resp-user-entid-str (rtucore/last-url-part user-location-str)
            pct (rucore/parse-media-type (get hdrs "Content-Type"))
            charset (get rumeta/char-sets (:charset pct))
            resp-user (rucore/read-res pct resp-body-stream charset)
            auth-token (get hdrs config/fphdr-auth-token)
            [loaded-user-entid loaded-user-ent] (usercore/load-user-by-authtoken @config/conn
                                                                                 (Long. resp-user-entid-str)
                                                                                 auth-token)]
        (is (not (nil? loaded-user-ent)))
        ;; Create 1st vehicle
        (is (empty? (fpcore/vehicles-for-user @config/conn loaded-user-entid)))
        (let [vehicle {"fpvehicle/name" "300Z"
                       "fpvehicle/fuel-capacity" 19.0
                       "fpvehicle/min-reqd-octane" 93}
              vehicles-uri (str config/fp-base-url
                                config/fp-entity-uri-prefix
                                usermeta/pathcomp-users
                                "/"
                                resp-user-entid-str
                                "/"
                                fpmeta/pathcomp-vehicles)
              req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                              (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                              fpmeta/v001
                                              "UTF-8;q=1,ISO-8859-1;q=0"
                                              "json"
                                              "en-US"
                                              :post
                                              vehicles-uri
                                              config/fphdr-apptxn-id
                                              config/fphdr-useragent-device-make
                                              config/fphdr-useragent-device-os
                                              config/fphdr-useragent-device-os-version)
                      (mock/body (json/write-str vehicle))
                      (mock/content-type (rucore/content-type rumeta/mt-type
                                                              (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                              fpmeta/v001
                                                              "json"
                                                              "UTF-8"))
                      (rtucore/header "Authorization" (rtucore/authorization-req-hdr-val config/fp-auth-scheme
                                                                                         config/fp-auth-scheme-param-name
                                                                                         auth-token)))
              resp (core/fp-app req)]
          (testing "status code" (is (= 201 (:status resp))))
          (testing "headers and body of created 300Z vehicle"
            (let [hdrs (:headers resp)
                  resp-body-stream (:body resp)
                  veh-location-str (get hdrs "location")
                  veh-last-modified-str (get hdrs "last-modified")]
              (is (= "Accept, Accept-Charset, Accept-Language" (get hdrs "Vary")))
              (is (not (nil? resp-body-stream)))
              (is (not (nil? veh-location-str)))
              (is (not (nil? veh-last-modified-str)))
              (let [last-modified (ucore/rfc7231str->instant veh-last-modified-str)
                    resp-veh-entid-str (rtucore/last-url-part veh-location-str)
                    pct (rucore/parse-media-type (get hdrs "Content-Type"))
                    charset (get rumeta/char-sets (:charset pct))
                    resp-veh (rucore/read-res pct resp-body-stream charset)]
                (is (not (nil? last-modified)))
                (is (not (nil? resp-veh-entid-str)))
                (is (not (nil? resp-veh)))
                (is (= "300Z" (get resp-veh "fpvehicle/name")))
                (is (= 19.0 (get resp-veh "fpvehicle/fuel-capacity")))
                (is (= 93 (get resp-veh "fpvehicle/min-reqd-octane")))
                (let [loaded-vehicles (fpcore/vehicles-for-user @config/conn loaded-user-entid)]
                  (is (= 1 (count loaded-vehicles)))
                  (let [[[loaded-veh-300z-entid loaded-veh-300z]] loaded-vehicles]
                    (is (= (Long/parseLong resp-veh-entid-str) loaded-veh-300z-entid))
                    (is (= "300Z" (:fpvehicle/name loaded-veh-300z)))
                    (is (= 19.0 (:fpvehicle/fuel-capacity loaded-veh-300z)))
                    (is (= 93 (:fpvehicle/min-reqd-octane loaded-veh-300z)))

                    ;; Create 2nd vehicle
                    (let [vehicle {"fpvehicle/name" "Mazda CX-9"
                                   "fpvehicle/fuel-capacity" 24.5
                                   "fpvehicle/min-reqd-octane" 87}
                          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                                          (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                          fpmeta/v001
                                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                                          "json"
                                                          "en-US"
                                                          :post
                                                          (str config/fp-base-url
                                                               config/fp-entity-uri-prefix
                                                               usermeta/pathcomp-users
                                                               "/"
                                                               resp-user-entid-str
                                                               "/"
                                                               fpmeta/pathcomp-vehicles)
                                                          config/fphdr-apptxn-id
                                                          config/fphdr-useragent-device-make
                                                          config/fphdr-useragent-device-os
                                                          config/fphdr-useragent-device-os-version)
                                  (mock/body (json/write-str vehicle))
                                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                                          (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                                          fpmeta/v001
                                                                          "json"
                                                                          "UTF-8"))
                                  (rtucore/header "Authorization" (rtucore/authorization-req-hdr-val config/fp-auth-scheme
                                                                                                     config/fp-auth-scheme-param-name
                                                                                                     auth-token)))
                          resp (core/fp-app req)]
                      (testing "status code" (is (= 201 (:status resp))))
                      (testing "headers and body of created mazda vehicle"
                        (let [hdrs (:headers resp)
                              resp-body-stream (:body resp)
                              veh-location-str (get hdrs "location")
                              veh-last-modified-str (get hdrs "last-modified")]
                          (is (= "Accept, Accept-Charset, Accept-Language" (get hdrs "Vary")))
                          (is (not (nil? resp-body-stream)))
                          (is (not (nil? veh-location-str)))
                          (is (not (nil? veh-last-modified-str)))
                          (let [last-modified (ucore/rfc7231str->instant veh-last-modified-str)
                                resp-veh-entid-str (rtucore/last-url-part veh-location-str)
                                pct (rucore/parse-media-type (get hdrs "Content-Type"))
                                charset (get rumeta/char-sets (:charset pct))
                                resp-veh (rucore/read-res pct resp-body-stream charset)]
                            (is (not (nil? last-modified)))
                            (is (not (nil? resp-veh-entid-str)))
                            (is (not (nil? resp-veh)))
                            (is (= "Mazda CX-9" (get resp-veh "fpvehicle/name")))
                            (is (= 24.5 (get resp-veh "fpvehicle/fuel-capacity")))
                            (is (= 87 (get resp-veh "fpvehicle/min-reqd-octane")))
                            (let [loaded-vehicles (fpcore/vehicles-for-user @config/conn loaded-user-entid)]
                              (is (= 2 (count loaded-vehicles)))
                              (let [[[loaded-veh-mazda-entid loaded-veh-mazda] _] loaded-vehicles]
                                (is (= (Long/parseLong resp-veh-entid-str) loaded-veh-mazda-entid))
                                (is (= "Mazda CX-9" (:fpvehicle/name loaded-veh-mazda)))
                                (is (= 24.5 (:fpvehicle/fuel-capacity loaded-veh-mazda)))
                                (is (= 87 (:fpvehicle/min-reqd-octane loaded-veh-mazda)))))))))))))))))))

(deftest integration-test-2
  (testing "Login of user and subsequent authenticated request."
    (is (nil? (usercore/load-user-by-email @config/conn "smithka@testing.com")))
    (is (nil? (usercore/load-user-by-username @config/conn "smithk")))
    (let [user {:user/name "Karen Smith"
                :user/email "smithka@testing.com"
                :user/password "insecure"}]
      @(d/transact @config/conn [(usercore/save-new-user-txnmap config/fp-partition user)])
      (is (not (nil? (usercore/load-user-by-email @config/conn "smithka@testing.com")))))
    (let [user {"user/username-or-email" "smithka@testing.com"
                "user/password" "insecure"}
          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                          (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                          usermeta/v001
                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                          "json"
                                          "en-US"
                                          :post
                                          core/login-uri-template
                                          config/fphdr-apptxn-id
                                          config/fphdr-useragent-device-make
                                          config/fphdr-useragent-device-os
                                          config/fphdr-useragent-device-os-version)
                  (mock/body (json/write-str user))
                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                          (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                                          usermeta/v001
                                                          "json"
                                                          "UTF-8")))
          resp (core/fp-app req)]
      (testing "status code" (is (= 200 (:status resp))))
      (let [hdrs (:headers resp)
            resp-body-stream (:body resp)
            user-location-str (get hdrs "location")
            user-last-modified-str (get hdrs "last-modified")
            last-modified (ucore/rfc7231str->instant user-last-modified-str)
            resp-user-entid-str (rtucore/last-url-part user-location-str)
            pct (rucore/parse-media-type (get hdrs "Content-Type"))
            charset (get rumeta/char-sets (:charset pct))
            resp-user (rucore/read-res pct resp-body-stream charset)
            auth-token (get hdrs config/fphdr-auth-token)
            [loaded-user-entid loaded-user-ent] (usercore/load-user-by-authtoken @config/conn
                                                                                 (Long. resp-user-entid-str)
                                                                                 auth-token)]
        (is (not (nil? loaded-user-ent)))
        ;; Create 1st vehicle
        (is (empty? (fpcore/vehicles-for-user @config/conn loaded-user-entid)))
        (let [vehicle {"fpvehicle/name" "300Z"
                       "fpvehicle/fuel-capacity" 19.0
                       "fpvehicle/min-reqd-octane" 93}
              vehicles-uri (str config/fp-base-url
                                config/fp-entity-uri-prefix
                                usermeta/pathcomp-users
                                "/"
                                resp-user-entid-str
                                "/"
                                fpmeta/pathcomp-vehicles)
              req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                              (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                              fpmeta/v001
                                              "UTF-8;q=1,ISO-8859-1;q=0"
                                              "json"
                                              "en-US"
                                              :post
                                              vehicles-uri
                                              config/fphdr-apptxn-id
                                              config/fphdr-useragent-device-make
                                              config/fphdr-useragent-device-os
                                              config/fphdr-useragent-device-os-version)
                      (mock/body (json/write-str vehicle))
                      (mock/content-type (rucore/content-type rumeta/mt-type
                                                              (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                              fpmeta/v001
                                                              "json"
                                                              "UTF-8"))
                      (rtucore/header "Authorization" (rtucore/authorization-req-hdr-val config/fp-auth-scheme
                                                                                         config/fp-auth-scheme-param-name
                                                                                         auth-token)))
              resp (core/fp-app req)]
          (testing "status code" (is (= 201 (:status resp))))
          (testing "headers and body of created 300Z vehicle"
            (let [hdrs (:headers resp)
                  resp-body-stream (:body resp)
                  veh-location-str (get hdrs "location")
                  veh-last-modified-str (get hdrs "last-modified")]
              (is (= "Accept, Accept-Charset, Accept-Language" (get hdrs "Vary")))
              (is (not (nil? resp-body-stream)))
              (is (not (nil? veh-location-str)))
              (is (not (nil? veh-last-modified-str)))
              (let [last-modified (ucore/rfc7231str->instant veh-last-modified-str)
                    resp-veh-entid-str (rtucore/last-url-part veh-location-str)
                    pct (rucore/parse-media-type (get hdrs "Content-Type"))
                    charset (get rumeta/char-sets (:charset pct))
                    resp-veh (rucore/read-res pct resp-body-stream charset)]
                (is (not (nil? last-modified)))
                (is (not (nil? resp-veh-entid-str)))
                (is (not (nil? resp-veh)))
                (is (= "300Z" (get resp-veh "fpvehicle/name")))
                (is (= 19.0 (get resp-veh "fpvehicle/fuel-capacity")))
                (is (= 93 (get resp-veh "fpvehicle/min-reqd-octane")))
                (let [loaded-vehicles (fpcore/vehicles-for-user @config/conn loaded-user-entid)]
                  (is (= 1 (count loaded-vehicles)))
                  (let [[[loaded-veh-300z-entid loaded-veh-300z]] loaded-vehicles]
                    (is (= (Long/parseLong resp-veh-entid-str) loaded-veh-300z-entid))
                    (is (= "300Z" (:fpvehicle/name loaded-veh-300z)))
                    (is (= 19.0 (:fpvehicle/fuel-capacity loaded-veh-300z)))
                    (is (= 93 (:fpvehicle/min-reqd-octane loaded-veh-300z)))

                    ;; Create 2nd vehicle
                    (let [vehicle {"fpvehicle/name" "Mazda CX-9"
                                   "fpvehicle/fuel-capacity" 24.5
                                   "fpvehicle/min-reqd-octane" 87}
                          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                                          (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                          fpmeta/v001
                                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                                          "json"
                                                          "en-US"
                                                          :post
                                                          (str config/fp-base-url
                                                               config/fp-entity-uri-prefix
                                                               usermeta/pathcomp-users
                                                               "/"
                                                               resp-user-entid-str
                                                               "/"
                                                               fpmeta/pathcomp-vehicles)
                                                          config/fphdr-apptxn-id
                                                          config/fphdr-useragent-device-make
                                                          config/fphdr-useragent-device-os
                                                          config/fphdr-useragent-device-os-version)
                                  (mock/body (json/write-str vehicle))
                                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                                          (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                                          fpmeta/v001
                                                                          "json"
                                                                          "UTF-8"))
                                  (rtucore/header "Authorization" (rtucore/authorization-req-hdr-val config/fp-auth-scheme
                                                                                                     config/fp-auth-scheme-param-name
                                                                                                     auth-token)))
                          resp (core/fp-app req)]
                      (testing "status code" (is (= 201 (:status resp))))
                      (testing "headers and body of created mazda vehicle"
                        (let [hdrs (:headers resp)
                              resp-body-stream (:body resp)
                              veh-location-str (get hdrs "location")
                              veh-last-modified-str (get hdrs "last-modified")]
                          (is (= "Accept, Accept-Charset, Accept-Language" (get hdrs "Vary")))
                          (is (not (nil? resp-body-stream)))
                          (is (not (nil? veh-location-str)))
                          (is (not (nil? veh-last-modified-str)))
                          (let [last-modified (ucore/rfc7231str->instant veh-last-modified-str)
                                resp-veh-entid-str (rtucore/last-url-part veh-location-str)
                                pct (rucore/parse-media-type (get hdrs "Content-Type"))
                                charset (get rumeta/char-sets (:charset pct))
                                resp-veh (rucore/read-res pct resp-body-stream charset)]
                            (is (not (nil? last-modified)))
                            (is (not (nil? resp-veh-entid-str)))
                            (is (not (nil? resp-veh)))
                            (is (= "Mazda CX-9" (get resp-veh "fpvehicle/name")))
                            (is (= 24.5 (get resp-veh "fpvehicle/fuel-capacity")))
                            (is (= 87 (get resp-veh "fpvehicle/min-reqd-octane")))
                            (let [loaded-vehicles (fpcore/vehicles-for-user @config/conn loaded-user-entid)]
                              (is (= 2 (count loaded-vehicles)))
                              (let [[[loaded-veh-mazda-entid loaded-veh-mazda] _] loaded-vehicles]
                                (is (= (Long/parseLong resp-veh-entid-str) loaded-veh-mazda-entid))
                                (is (= "Mazda CX-9" (:fpvehicle/name loaded-veh-mazda)))
                                (is (= 24.5 (:fpvehicle/fuel-capacity loaded-veh-mazda)))
                                (is (= 87 (:fpvehicle/min-reqd-octane loaded-veh-mazda)))))))))))))))))))

(deftest integration-test-3
  (testing "Successful creation of user, app txn logs and vehicles."
    (is (nil? (usercore/load-user-by-email @config/conn "smithka@testing.com")))
    (is (nil? (usercore/load-user-by-username @config/conn "smithk")))
    (let [user {"user/name" "Karen Smith"
                "user/email" "smithka@testing.com"
                "user/username" "smithk"
                "user/password" "insecure"}
          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                          (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                          usermeta/v001
                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                          "json"
                                          "en-US"
                                          :post
                                          core/users-uri-template
                                          config/fphdr-apptxn-id
                                          config/fphdr-useragent-device-make
                                          config/fphdr-useragent-device-os
                                          config/fphdr-useragent-device-os-version)
                  (rtucore/header config/fphdr-establish-session "true")
                  (mock/body (json/write-str user))
                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                          (usermeta/mt-subtype-user config/fp-mt-subtype-prefix)
                                                          usermeta/v001
                                                          "json"
                                                          "UTF-8")))
          resp (core/fp-app req)]
      (let [hdrs (:headers resp)
            resp-body-stream (:body resp)
            user-location-str (get hdrs "location")
            user-last-modified-str (get hdrs "last-modified")
            last-modified (ucore/rfc7231str->instant user-last-modified-str)
            resp-user-entid-str (rtucore/last-url-part user-location-str)
            pct (rucore/parse-media-type (get hdrs "Content-Type"))
            charset (get rumeta/char-sets (:charset pct))
            resp-user (rucore/read-res pct resp-body-stream charset)
            auth-token (get hdrs config/fphdr-auth-token)
            [loaded-user-entid loaded-user-ent] (usercore/load-user-by-authtoken @config/conn
                                                                                 (Long. resp-user-entid-str)
                                                                                 auth-token)]
        ;; Create 1st vehicle
        (is (empty? (fpcore/vehicles-for-user @config/conn loaded-user-entid)))
        (let [vehicle {"fpvehicle/name" "300Z"
                       "fpvehicle/fuel-capacity" 19.0
                       "fpvehicle/min-reqd-octane" 93}
              vehicles-uri (str config/fp-base-url
                                config/fp-entity-uri-prefix
                                usermeta/pathcomp-users
                                "/"
                                resp-user-entid-str
                                "/"
                                fpmeta/pathcomp-vehicles)
              req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                              (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                              fpmeta/v001
                                              "UTF-8;q=1,ISO-8859-1;q=0"
                                              "json"
                                              "en-US"
                                              :post
                                              vehicles-uri
                                              config/fphdr-apptxn-id
                                              config/fphdr-useragent-device-make
                                              config/fphdr-useragent-device-os
                                              config/fphdr-useragent-device-os-version)
                      (mock/body (json/write-str vehicle))
                      (mock/content-type (rucore/content-type rumeta/mt-type
                                                              (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                              fpmeta/v001
                                                              "json"
                                                              "UTF-8"))
                      (rtucore/header "Authorization" (rtucore/authorization-req-hdr-val config/fp-auth-scheme
                                                                                         config/fp-auth-scheme-param-name
                                                                                         auth-token)))
              resp (core/fp-app req)]
          (testing "status code" (is (= 201 (:status resp))))
          (testing "headers and body of created 300Z vehicle"
            (let [hdrs (:headers resp)
                  resp-body-stream (:body resp)
                  veh-location-str (get hdrs "location")
                  veh-last-modified-str (get hdrs "last-modified")]
              (is (= "Accept, Accept-Charset, Accept-Language" (get hdrs "Vary")))
              (is (not (nil? resp-body-stream)))
              (is (not (nil? veh-location-str)))
              (is (not (nil? veh-last-modified-str)))
              (let [last-modified (ucore/rfc7231str->instant veh-last-modified-str)
                    resp-veh-entid-str (rtucore/last-url-part veh-location-str)
                    pct (rucore/parse-media-type (get hdrs "Content-Type"))
                    charset (get rumeta/char-sets (:charset pct))
                    resp-veh (rucore/read-res pct resp-body-stream charset)]
                (is (not (nil? last-modified)))
                (is (not (nil? resp-veh-entid-str)))
                (is (not (nil? resp-veh)))
                (is (= "300Z" (get resp-veh "fpvehicle/name")))
                (is (= 19.0 (get resp-veh "fpvehicle/fuel-capacity")))
                (is (= 93 (get resp-veh "fpvehicle/min-reqd-octane")))
                (let [loaded-vehicles (fpcore/vehicles-for-user @config/conn loaded-user-entid)]
                  (is (= 1 (count loaded-vehicles)))
                  (let [[[loaded-veh-300z-entid loaded-veh-300z]] loaded-vehicles]
                    (is (= (Long/parseLong resp-veh-entid-str) loaded-veh-300z-entid))
                    (is (= "300Z" (:fpvehicle/name loaded-veh-300z)))
                    (is (= 19.0 (:fpvehicle/fuel-capacity loaded-veh-300z)))
                    (is (= 93 (:fpvehicle/min-reqd-octane loaded-veh-300z)))

                    ;; Create 2nd vehicle
                    (let [vehicle {"fpvehicle/name" "Mazda CX-9"
                                   "fpvehicle/fuel-capacity" 24.5
                                   "fpvehicle/min-reqd-octane" 87}
                          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                                          (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                          fpmeta/v001
                                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                                          "json"
                                                          "en-US"
                                                          :post
                                                          (str config/fp-base-url
                                                               config/fp-entity-uri-prefix
                                                               usermeta/pathcomp-users
                                                               "/"
                                                               resp-user-entid-str
                                                               "/"
                                                               fpmeta/pathcomp-vehicles)
                                                          config/fphdr-apptxn-id
                                                          config/fphdr-useragent-device-make
                                                          config/fphdr-useragent-device-os
                                                          config/fphdr-useragent-device-os-version)
                                  (mock/body (json/write-str vehicle))
                                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                                          (fpmeta/mt-subtype-vehicle config/fp-mt-subtype-prefix)
                                                                          fpmeta/v001
                                                                          "json"
                                                                          "UTF-8"))
                                  (rtucore/header "Authorization" (rtucore/authorization-req-hdr-val config/fp-auth-scheme
                                                                                                     config/fp-auth-scheme-param-name
                                                                                                     auth-token)))
                          resp (core/fp-app req)]
                      (testing "status code" (is (= 201 (:status resp))))
                      (testing "headers and body of created mazda vehicle"
                        (let [hdrs (:headers resp)
                              resp-body-stream (:body resp)
                              veh-location-str (get hdrs "location")
                              veh-last-modified-str (get hdrs "last-modified")]
                          (is (= "Accept, Accept-Charset, Accept-Language" (get hdrs "Vary")))
                          (is (not (nil? resp-body-stream)))
                          (is (not (nil? veh-location-str)))
                          (is (not (nil? veh-last-modified-str)))
                          (let [last-modified (ucore/rfc7231str->instant veh-last-modified-str)
                                resp-veh-entid-str (rtucore/last-url-part veh-location-str)
                                pct (rucore/parse-media-type (get hdrs "Content-Type"))
                                charset (get rumeta/char-sets (:charset pct))
                                resp-veh (rucore/read-res pct resp-body-stream charset)]
                            (is (not (nil? last-modified)))
                            (is (not (nil? resp-veh-entid-str)))
                            (is (not (nil? resp-veh)))
                            (is (= "Mazda CX-9" (get resp-veh "fpvehicle/name")))
                            (is (= 24.5 (get resp-veh "fpvehicle/fuel-capacity")))
                            (is (= 87 (get resp-veh "fpvehicle/min-reqd-octane")))
                            (let [loaded-vehicles (fpcore/vehicles-for-user @config/conn loaded-user-entid)]
                              (is (= 2 (count loaded-vehicles)))
                              (let [[[loaded-veh-mazda-entid loaded-veh-mazda] _] loaded-vehicles]
                                (is (= (Long/parseLong resp-veh-entid-str) loaded-veh-mazda-entid))
                                (is (= "Mazda CX-9" (:fpvehicle/name loaded-veh-mazda)))
                                (is (= 24.5 (:fpvehicle/fuel-capacity loaded-veh-mazda)))
                                (is (= 87 (:fpvehicle/min-reqd-octane loaded-veh-mazda)))))))))))))))))))
