#!/bin/bash

curl -H "Content-Type: application/vnd.fp.user-v0.0.1+json;charset=UTF-8" \
     -H "Accept-Language: en-US" \
     -H "Accept: application/vnd.fp.user-v0.0.1+json" \
     -X POST \
     -d '{"user/username-or-email": "evansp2@gmail.com", "user/password": "aardB8rt"}' \
     http://www.gasjot.com:4040/gasjot/d/login \
     -o login-response2.json
