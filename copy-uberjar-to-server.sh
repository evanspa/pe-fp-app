#!/bin/bash

readonly VERSION=$1
readonly SERVER_USERNAME="fprest"
readonly SERVER_HOST="gasjot"

scp target/pe-fp-app-${VERSION}-standalone.jar ${SERVER_USERNAME}@${SERVER_HOST}:/home/${SERVER_USERNAME}/documents/pe-fp-app/fpapp-uberjars
