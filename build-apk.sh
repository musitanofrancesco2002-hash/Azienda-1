#!/bin/sh
set -e
cd "$(dirname "$0")"
exec sh ./gradlew :app:assembleDebug
