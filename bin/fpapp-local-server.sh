#!/bin/bash

set -x

################################################################################
# Locations
################################################################################
readonly FPAPP_HOME="$HOME/Documents/BitBucket-repos/pe-fp-app"
readonly FPAPP_UBERJARS_DIR="$FPAPP_HOME/target"
readonly FPAPP_UBERJAR_NAME="pe-fp-app-0.0.1-SNAPSHOT-standalone.jar"

################################################################################
# Config variables
################################################################################
readonly POSTGRESQL_USER="datomic"
readonly POSTGRESQL_PASSWORD="datomic"
readonly POSTGRESQL_DBNAME="datomic"
readonly POSTGRESQL_SERVER="localhost"
readonly POSTGRESQL_PORT="5432"

readonly FP_DATOMIC_DATABASE_NAME="fuelpurchase"
readonly FP_DATABASE_URI="datomic:sql://${FP_DATOMIC_DATABASE_NAME}?jdbc:postgresql://${POSTGRESQL_SERVER}:${POSTGRESQL_PORT}/${POSTGRESQL_DBNAME}?user=${POSTGRESQL_USER}&password=${POSTGRESQL_PASSWORD}"
readonly FP_HYPERMEDIA_BASE_URL="http://localhost:4040"

readonly FPAPP_SERVER_JVM_INIT_MEMORY="1024m"
readonly FPAPP_SERVER_JVM_MAX_MEMORY="1024m"
readonly FP_NREPL_SERVER_PORT=7888

################################################################################
# JVM property names
################################################################################
readonly FP_DATABASE_URI_LOOKUP_KEY="fp.datomic.url"
readonly FP_HYPERMEDIA_BASE_URL_LOOKUP_KEY="fp.base.url"
readonly FP_NREPL_SERVER_PORT_KEY="fp.nrepl.server.port"
readonly FP_LOGBACK_LOGSDIR_KEY="FPAPP_LOGS_DIR"

exec java -D${FP_HYPERMEDIA_BASE_URL_LOOKUP_KEY}=${FP_HYPERMEDIA_BASE_URL} \
-D${FP_DATABASE_URI_LOOKUP_KEY}=${FP_DATABASE_URI} \
-D${FP_NREPL_SERVER_PORT_KEY}=${FP_NREPL_SERVER_PORT} \
-D${FP_LOGBACK_LOGSDIR_KEY}=${FPAPP_HOME} \
-server \
-Xms${FPAPP_SERVER_JVM_INIT_MEMORY} \
-Xmx${FPAPP_SERVER_JVM_MAX_MEMORY} \
-jar \
${FPAPP_UBERJARS_DIR}/${FPAPP_UBERJAR_NAME}
