(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.stacktrace :refer (e)]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.test :as test]
            [clojure.java.io :refer [resource]]
            [ring.server.standalone :refer (serve)]
            [pe-user-core.core :as usercore]
            [pe-fp-core.core :as fpcore]
            [pe-fp-core.admin :as fpadmin]
            [pe-fp-app.config :as config]
            [pe-fp-app.lifecycle :as lifecycle]
            [pe-fp-app.core :as core]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [pe-jdbc-utils.core :as jcore]
            [clojure.java.jdbc :as j]
            [pe-fp-core.ddl :as fpddl]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [pe-rest-utils.core :refer [*retry-after*]]))

(def server (atom nil))

(defn- create-and-start-server
  []
  (serve core/fp-app {:port 4040 :open-browser? false :auto-reload? true}))

(defn- go-with-db-refresh []
  (println "Proceeding to refresh the database")
  ;(jcore/drop-database config/db-spec-without-db config/fp-db-name)
  ;(jcore/create-database config/db-spec-without-db config/fp-db-name)
  (try
    (j/db-do-commands (config/db-spec)
                      true
                      fpddl/v6-create-postgis-extension)
    (catch Exception e
      (log/debug "Exception caught executing 'fpddl/v6-create-postgis-extension'.  This is okay.")))
  (lifecycle/init-database)
  (reset! server (create-and-start-server))
  (println "Jetty server restarted."))

(defn- go []
  (reset! server (create-and-start-server))
  (println "Jetty server restarted."))

(defn reset
  ([] (reset nil))
  ([refresh-db]
   (let [go-fn-name (if (nil? refresh-db) 'user/go 'user/go-with-db-refresh)]
     (when (not (nil? @server))
       (println "Proceeding to stop server")
       (.stop @server))
     (refresh-all :after go-fn-name))))

(defn busy []
  (alter-var-root (var *retry-after*) (fn [_] (t/now))))

(defn not-busy []
  (alter-var-root (var *retry-after*) (fn [_] nil)))

(defn start []
  (.start @server))

(defn stop []
  (.stop @server))
