(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.stacktrace :refer (e)]
            [clj-time.core :as t]
            [clojure.test :as test]
            [datomic.api :as d]
            [clojure.java.io :refer [resource]]
            [ring.server.standalone :refer (serve)]
            [pe-datomic-utils.core :as ducore]
            [pe-fp-app.config :as config]
            [pe-fp-app.lifecycle :as lifecycle]
            [pe-fp-app.core :as core]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))

(def server (atom nil))

(defn create-and-start-server []
  (serve core/fp-app {:port 4040 :open-browser? false :auto-reload? true}))

(defn reset
  ([] (reset nil))
  ([refresh-db]
   (let [go-fn-name (if (nil? refresh-db) 'user/go 'user/go-with-db-refresh)]
     (when (not (nil? @server))
       (println "Proceeding to stop server")
       (.stop @server))
     (refresh-all :after go-fn-name))))

(defn- go-with-db-refresh []
  (println "Proceeding to refresh the database")
  (d/delete-database config/fp-datomic-url)
  (lifecycle/init-database)
  (reset! server (create-and-start-server))
  (println "Jetty server restarted."))

(defn- go []
  (reset! server (create-and-start-server))
  (println "Jetty server restarted."))
