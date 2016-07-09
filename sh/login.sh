#!/bin/bash

curl -v -H "Content-Type: application/vnd.fp.user-v0.0.1+json;charset=UTF-8" \
     -H "Accept-Language: en-US" \
     -H "Accept: application/vnd.fp.user-v0.0.1+json" \
     -H "fp-desired-embedded-format: id-keyed" \
     -X POST \
     -d '{"user/username-or-email": "evansp2@gmail.com", "user/password": "aardB8rt"}' \
     http://www.jotyourself.com:4040/gasjot/d/login \
     -o tmp.json
