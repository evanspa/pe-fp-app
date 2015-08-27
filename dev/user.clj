(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.stacktrace :refer (e)]
            [clj-time.core :as t]
            [clojure.test :as test]
            [clojure.java.io :refer [resource]]
            [ring.server.standalone :refer (serve)]
            [pe-user-core.core :as usercore]
            [pe-fp-core.core :as fpcore]
            [pe-fp-app.config :as config]
            [pe-fp-app.lifecycle :as lifecycle]
            [pe-fp-app.core :as core]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [pe-jdbc-utils.core :as jcore]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))

(def server (atom nil))

(defn- create-and-start-server
  []
  (serve core/fp-app {:port 4040 :open-browser? false :auto-reload? true}))

(defn- go-with-db-refresh []
  (println "Proceeding to refresh the database")
  (jcore/drop-database config/db-spec-without-db config/fp-db-name)
  (jcore/create-database config/db-spec-without-db config/fp-db-name)
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
