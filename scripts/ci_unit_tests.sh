#!/usr/bin/env bash

./gradlew coveralls -Dpre-dex=false -Pkotlin.incremental=false --stacktrace --no-daemon --max-workers 2
