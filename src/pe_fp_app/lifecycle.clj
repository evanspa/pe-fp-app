(ns pe-fp-app.lifecycle
  (:require [pe-fp-app.config :as config]
            [clojure.tools.logging :as log]
            [pe-jdbc-utils.core :as jcore]
            [clojure.java.jdbc :as j]
            [pe-user-core.ddl :as uddl]
            [pe-fp-core.ddl :as fpddl]
            [pe-user-core.core :as usercore]
            [clojure.tools.nrepl.server :as nrepl]
            [environ.core :refer [env]]))

(def nrepl-server)

(def target-schema-version 0)

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
         (uddl/v0-create-updated-count-inc-trigger-function-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v0-create-user-account-updated-count-trigger-fn config/db-spec))
       ;; Vehicle setup
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v0-create-vehicle-ddl
                         fpddl/v0-add-unique-constraint-vehicle-name)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-vehicle-updated-count-inc-trigger-function-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-vehicle-updated-count-trigger-fn config/db-spec))
       ;; Fuelstation setup
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v0-create-fuelstation-ddl
                         fpddl/v0-create-index-on-fuelstation-name)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-fuelstation-updated-count-inc-trigger-function-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-fuelstation-updated-count-trigger-fn config/db-spec))
       ;; Fuel purchase log setup
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v0-create-fplog-ddl)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-fplog-updated-count-inc-trigger-function-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-fplog-updated-count-trigger-fn config/db-spec))
       ;; Environment log setup
       (j/db-do-commands config/db-spec
                         true
                         fpddl/v0-create-envlog-ddl)
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-envlog-updated-count-inc-trigger-function-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (fpddl/v0-create-envlog-updated-count-trigger-fn config/db-spec)))})

(defn init-database
  []
  ;; Database setup
  (log/info "Proceeding to setup database")

  ;; Create schema version table
  (j/db-do-commands config/db-spec true uddl/schema-version-ddl)

  ;; Apply DDL operations
  (let [current-schema-version (usercore/get-schema-version config/db-spec)]
    (if (nil? current-schema-version)
      (let [do-upper-bound (inc target-schema-version)]
        (log/info "Current schema version installed is nil.  Proceeding to apply DDL operations through target schema version: "
                  target-schema-version)
        (dotimes [version do-upper-bound]
          (let [ddl-fn (get ddl-operations version)]
            (ddl-fn))))
      (let [do-upper-bound (- target-schema-version current-schema-version)]
        (log/info "Current schema version installed: "
                  current-schema-version
                  ".  Proceeding to apply DDL operations through target schema version: "
                  target-schema-version)
        (dotimes [version do-upper-bound]
          (let [ddl-fn (get ddl-operations (+ version (inc current-schema-version)))]
            (ddl-fn)))))
    (usercore/set-schema-version config/db-spec target-schema-version)))

(defn init []
  (log/info "Proceeding to start FP App server.")
  (init-database)
  (log/info (format "Proceeding to start nrepl-server at port: [%s]" config/fp-nrepl-server-port))
  (defonce nrepl-server
    (nrepl/start-server :port (Integer/valueOf config/fp-nrepl-server-port))))

(defn stop []
  (log/info "Proceeding to stop FP App server.")
  (nrepl/stop-server nrepl-server))
