(ns pe-fp-app.lifecycle
  (:require [datomic.api :refer [q db] :as d]
            [pe-fp-app.config :as config]
            [clojure.tools.logging :as log]
            [pe-datomic-utils.core :as ducore]
            [clojure.tools.nrepl.server :as nrepl]
            [environ.core :refer [env]]))

(def nrepl-server)

(defn init-database
  []
  (log/info (format "Proceeding to create database at uri: [%s]" config/fp-datomic-url))
  (let [db-created (d/create-database config/fp-datomic-url)]
    (reset! config/conn (d/connect config/fp-datomic-url))
    (letfn [(transact-user-schema-version []
              (ducore/transact-user-schema-version @config/conn
                                                   config/fp-partition
                                                   config/fp-app-schemaset-version-attr
                                                   (dec (count config/fp-app-schema-sets))))]
      (if db-created
        (do
          (log/info (format "Database at uri: [%s] created." config/fp-datomic-url))
          (log/info "Proceeding to install target schema sets.")
          (ducore/transact-user-schema-attribute @config/conn config/fp-app-schemaset-version-attr)
          (ducore/transact-schema-files @config/conn
                                        (flatten config/fp-app-schema-sets))
          (ducore/transact-partition @config/conn
                                     config/fp-partition)
          (ducore/transact-partition @config/conn
                                     config/fp-apptxn-partition)
          (transact-user-schema-version)
          (log/info "Target schema sets installed."))
        (let [[_ current-ver] (ducore/get-user-schema-version @config/conn
                                                              config/fp-app-schemaset-version-attr)]
          (log/info (format "Database at uri: [%s] already exists." config/fp-datomic-url))
          (log/info (format "Current schema version (schema set count) installed: [%s]." current-ver))
          (let [target-ver (dec (count config/fp-app-schema-sets))]
            (log/info (format "Target schema version (schema set count): [%s]." target-ver))
            (when (> target-ver current-ver)
              (log/info "Proceeding to install target schema sets.")
              (ducore/transact-schema-files @config/conn
                                            (flatten (subvec config/fp-app-schema-sets (inc current-ver))))
              (transact-user-schema-version)
              (log/info "Target schema sets installed."))))))))

(defn init []
  (log/info "Proceeding to start FP App server.")
  (init-database)
  (log/info (format "Proceeding to start nrepl-server at port: [%s]" config/fp-nrepl-server-port))
  (defonce nrepl-server
    (nrepl/start-server :port (Integer/valueOf config/fp-nrepl-server-port))))

(defn stop []
  (log/info "Proceeding to stop FP App server.")
  (nrepl/stop-server nrepl-server))
