#!/bin/bash

readonly VERSION=$1
readonly SSH_PORT=$2
readonly SERVER_USERNAME=$3
readonly SERVER_HOST=$4

scp -P ${SSH_PORT} target/pe-fp-app-${VERSION}-standalone.jar ${SERVER_USERNAME}@${SERVER_HOST}:/home/${SERVER_USERNAME}/documents/pe-fp-app/fpapp-uberjars
