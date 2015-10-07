(defproject pe-fp-app "0.0.23"
  :description "The fuel purchase application REST API endpoint."
  :url "https://github.com/evanspa/pe-fp-app"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-pprint "1.1.2"]
            [codox "0.8.10"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [clj-time "0.8.0"]
                 [org.clojure/tools.nrepl "0.2.7"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.postgresql/postgresql "9.3-1102-jdbc41"]
                 [ring-server "0.3.1"]
                 [compojure "1.2.1"]
                 [liberator "0.12.2"]
                 [environ "1.0.0"]
                 [clojurewerkz/mailer "1.2.0"]
                 [javax.mail/mail "1.4.7"]
                 [pe-core-utils "0.0.11"]
                 [pe-jdbc-utils "0.0.16"]
                 [pe-rest-utils "0.0.30"]
                 [pe-user-core "0.1.32"]
                 [pe-user-rest "0.0.42"]
                 [pe-fp-core "0.0.19"]
                 [pe-fp-rest "0.0.26"]]
  :resource-paths ["resources"]
  :ring {:handler pe-fp-app.core/fp-app
         :init pe-fp-app.lifecycle/init
         :destroy pe-fp-app.lifecycle/stop}
  :profiles {:dev {:source-paths ["dev"]  ;ensures 'user.clj' gets auto-loaded
                   :env {:fp-app-version "0.0.23"
                         :fp-uri-prefix "/gasjot/d/"
                         :fp-db-name "fp"
                         :fp-db-server-host "localhost"
                         :fp-db-server-port 5432
                         :fp-db-username "postgres"
                         :fp-jdbc-driver-class "org.postgresql.Driver"
                         :fp-jdbc-subprotocol "postgresql"
                         :fp-base-url "http://localhost:4040"
                         :fp-smtp-host "localhost"
                         :fp-nrepl-server-port 7888}
                   :plugins [[cider/cider-nrepl "0.10.0-SNAPSHOT"]
                             [lein-environ "1.0.0"]
                             [lein-ring "0.8.13"]]
                   :resource-paths ["test-resources"]
                   :dependencies [[org.clojure/tools.namespace "0.2.7"]
                                  [org.clojure/java.classpath "0.2.2"]
                                  [org.clojure/tools.nrepl "0.2.7"]
                                  [pe-rest-testutils "0.0.7"]
                                  [ring-mock "0.1.5"]]}}
  :jvm-opts ["-Xmx1g" "-DFPAPP_LOGS_DIR=logs"]
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]])
