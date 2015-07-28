#!/bin/bash

################################################################################
# Locations
################################################################################
readonly FPAPP_UBERJAR_VERSION="0.0.11"
readonly FPAPP_UBERJARS_DIR="/Users/paulevans/Documents/GitHub-repos/pe-fp-app/target"
readonly FPAPP_UBERJAR_NAME="pe-fp-app-${FPAPP_UBERJAR_VERSION}-standalone.jar"

################################################################################
# Config variables
################################################################################
readonly FP_DB_NAME="fp"
readonly FP_DB_SERVER_HOST="localhost"
readonly FP_DB_SERVER_PORT="5432"
readonly FP_DB_USERNAME="postgres"
readonly FP_DB_PASSWORD="postgres"
readonly FP_JDBC_DRIVER_CLASS="org.postgresql.Driver"
readonly FP_JDBC_SUBPROTOCOL="postgresql"

readonly FP_HYPERMEDIA_BASE_URL="http://localhost:4040"

readonly FPAPP_SERVER_JVM_INIT_MEMORY="256m"
readonly FPAPP_SERVER_JVM_MAX_MEMORY="512m"

readonly FPAPP_SERVER_RUNDIR="$HOME/run"
readonly FPAPP_SERVER_LOGSDIR="$HOME/logs"
readonly FPAPP_SERVER_PID="$FPAPP_SERVER_RUNDIR/fpapp-server.pid"
readonly FPAPP_SERVER_OUT="$FPAPP_SERVER_LOGSDIR/fpapp-server.out"

readonly FPAPP_SERVER_DESC="FP App server"

readonly FP_NREPL_SERVER_PORT=7888

################################################################################
# JVM property names
################################################################################
readonly FP_APP_VERSION_LKUP_KEY="fp.app.version"
readonly FP_DB_NAME_LKUP_KEY="fp.db.name"
readonly FP_DB_SERVER_HOST_LKUP_KEY="fp.db.server.host"
readonly FP_DB_SERVER_PORT_LKUP_KEY="fp.db.server.port"
readonly FP_DB_USERNAME_LKUP_KEY="fp.db.username"
readonly FP_DB_PASSWORD_LKUP_KEY="fp.db.password"
readonly FP_JDBC_DRIVER_CLASS_LKUP_KEY="fp.jdbc.driver.class"
readonly FP_JDBC_SUBPROTOCOL_LKUP_KEY="fp.jdbc.subprotocol"
readonly FP_HYPERMEDIA_BASE_URL_LKUP_KEY="fp.base.url"
readonly FP_NREPL_SERVER_PORT_KEY="fp.nrepl.server.port"
readonly FP_LOGBACK_LOGSDIR_KEY="FPAPP_LOGS_DIR"

mkdir -p $FPAPP_SERVER_LOGSDIR

touch "$FPAPP_SERVER_OUT"
java -D${FP_HYPERMEDIA_BASE_URL_LKUP_KEY}=${FP_HYPERMEDIA_BASE_URL} \
-D${FP_APP_VERSION_LKUP_KEY}=${FPAPP_UBERJAR_VERSION} \
-D${FP_DB_NAME_LKUP_KEY}=${FP_DB_NAME} \
-D${FP_DB_SERVER_HOST_LKUP_KEY}=${FP_DB_SERVER_HOST} \
-D${FP_DB_SERVER_PORT_LKUP_KEY}=${FP_DB_SERVER_PORT} \
-D${FP_DB_USERNAME_LKUP_KEY}=${FP_DB_USERNAME} \
-D${FP_DB_PASSWORD_LKUP_KEY}=${FP_DB_PASSWORD} \
-D${FP_JDBC_DRIVER_CLASS_LKUP_KEY}=${FP_JDBC_DRIVER_CLASS} \
-D${FP_JDBC_SUBPROTOCOL_LKUP_KEY}=${FP_JDBC_SUBPROTOCOL} \
-D${FP_NREPL_SERVER_PORT_KEY}=${FP_NREPL_SERVER_PORT} \
-D${FP_LOGBACK_LOGSDIR_KEY}=${FPAPP_SERVER_LOGSDIR} \
-server \
-Xms${FPAPP_SERVER_JVM_INIT_MEMORY} \
-Xmx${FPAPP_SERVER_JVM_MAX_MEMORY} \
-jar \
${FPAPP_UBERJARS_DIR}/${FPAPP_UBERJAR_NAME}
