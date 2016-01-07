(ns pe-fp-app.lifecycle
  (:require [pe-fp-app.config :as config]
            [clojure.tools.logging :as log]
            [pe-jdbc-utils.core :as jcore]
            [clojure.java.jdbc :as j]
            [pe-user-core.ddl :as uddl]
            [pe-fp-core.migration :as fpmig]
            [pe-fp-core.data-loads :as fpdataloads]
            [pe-fp-core.ddl :as fpddl]
            [pe-user-core.core :as usercore]
            [clojure.tools.nrepl.server :as nrepl]
            [environ.core :refer [env]]))

(def nrepl-server)

(def target-schema-version 10)

(def ddl-operations
  {0 (fn []
       ;; User / auth-token setup
       (j/db-do-commands config/db-spec
                         true
                         uddl/v0-create-user-account-ddl
                         uddl/v0-add-unique-constraint-user-account-email
                         uddl/v0-add-unique-constraint-user-account-username
                         uddl/v0-create-authentication-token-ddl)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v0-create-updated-count-inc-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v0-create-user-account-updated-count-trigger-fn config/db-spec))
       ;; Vehicle setup
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v0-create-vehicle-ddl
                         fpddl/v0-add-unique-constraint-vehicle-name)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-vehicle-updated-count-inc-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-vehicle-updated-count-trigger-fn config/db-spec))
       ;; Fuelstation setup
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v0-create-fuelstation-ddl
                         fpddl/v0-create-index-on-fuelstation-name)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-fuelstation-updated-count-inc-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-fuelstation-updated-count-trigger-fn config/db-spec))
       ;; Fuel purchase log setup
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v0-create-fplog-ddl)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-fplog-updated-count-inc-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-fplog-updated-count-trigger-fn config/db-spec))
       ;; Environment log setup
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v0-create-envlog-ddl)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-envlog-updated-count-inc-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-envlog-updated-count-trigger-fn config/db-spec)))
   1 (fn []
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v1-vehicle-add-fuel-capacity-col))
   2 (fn []
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v2-vehicle-drop-erroneous-unique-name-constraint
                         fpddl/v2-vehicle-add-proper-unique-name-constraint))
   3 (fn []
       (j/db-do-commands config/db-spec
                         true
                         uddl/v1-user-add-deleted-reason-col
                         uddl/v1-user-add-suspended-at-col
                         uddl/v1-user-add-suspended-reason-col
                         uddl/v1-user-add-suspended-count-col)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v1-create-suspended-count-inc-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v1-create-user-account-suspended-count-trigger-fn config/db-spec)))
   4 (fn []
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v3-vehicle-drop-erroneous-unique-name-constraint-again
                         fpddl/v3-vehicle-add-proper-unique-name-constraint-take-2))
   5 (fn []
       (j/db-do-commands config/db-spec
                         true
                         uddl/v2-create-email-verification-token-ddl))
   6 (fn []
       (j/db-do-commands config/db-spec
                         true
                         uddl/v3-create-password-reset-token-ddl))
   7 (fn []
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v4-fplog-add-odometer-col)
       (fpmig/v4-migrations config/db-spec))
   8 (fn []
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v5-vehicle-add-diesel-col
                         fpddl/v5-vehicle-add-has-dte-readout-col
                         fpddl/v5-vehicle-add-has-mpg-readout-col
                         fpddl/v5-vehicle-add-has-mph-readout-col
                         fpddl/v5-vehicle-add-has-outside-temp-readout-col
                         fpddl/v5-vehicle-add-vin-col
                         fpddl/v5-vehicle-add-plate-col
                         fpddl/v5-fplog-add-diesel-col))
   9 (fn []
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v6-create-fuelstation-type-ddl
                         fpddl/v6-fuelstation-add-fstype-col))
   10 (fn []
        (fpddl/v6-fuelstation-add-location-col-sql config/db-spec)
        (fpdataloads/v6-data-loads config/db-spec))})

(defn init-database
  []
  ;; Database setup
  (log/info (format "Proceeding to setup database (app version=[%s])" config/fp-app-version))

  ;; Create schema version table
  (j/db-do-commands config/db-spec true uddl/schema-version-ddl)

  ;; Apply DDL operations
  (let [current-schema-version (usercore/get-schema-version config/db-spec)]
    (if (nil? current-schema-version)
      (let [do-upper-bound (inc target-schema-version)]
        (log/info (format "Current schema version installed is nil.  Proceeding to apply DDL operations through target schema version: [%d]"
                          target-schema-version))
        (dotimes [version do-upper-bound]
          (let [ddl-fn (get ddl-operations version)]
            (log/info (format "Proceeding to apply version [%d] DDL updates." version))
            (ddl-fn))))
      (let [do-upper-bound (- target-schema-version current-schema-version)]
        (log/info (format "Current schema version installed: [%d].  Proceeding to apply DDL operations through target schema version: [%d]."
                          current-schema-version
                          target-schema-version))
        (dotimes [version do-upper-bound]
          (let [version-key (+ version (inc current-schema-version))
                ddl-fn (get ddl-operations version-key)]
            (log/info (format "Proceeding to apply version [%d] DDL updates." version-key))
            (ddl-fn)
            (log/info (format  "Version [%d] DDL updates applied." version-key))))))
    (usercore/set-schema-version config/db-spec target-schema-version)
    (log/info (format  "Schema version table updated to value: [%d]." target-schema-version))))

(defn init []
  (log/info (format "Proceeding to start FP App server (version=[%s])." config/fp-app-version))
  (init-database)
  (log/info (format "Proceeding to start nrepl-server at port: [%s]" config/fp-nrepl-server-port))
  (defonce nrepl-server
    (nrepl/start-server :port (Integer/valueOf config/fp-nrepl-server-port))))

(defn stop []
  (log/info (format "Proceeding to stop FP App server (version=[%s])." config/fp-app-version))
  (nrepl/stop-server nrepl-server))
