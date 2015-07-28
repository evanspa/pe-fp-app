#!/bin/bash

readonly PROJECT="pe-fp-app"
readonly VERSION="$1"
readonly TAG_LABEL=${VERSION}

git tag -f -a $TAG_LABEL -m 'version $version'
git push -f --tags
