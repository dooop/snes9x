#!/bin/sh
set -eu

SNES_BUILD_FROM_SOURCE=1 swift package dump-package >/dev/null
SNES_BUILD_FROM_SOURCE=1 swift build
SNES_BUILD_FROM_SOURCE=1 swift test

if [ "${ANDROID_SDK_ROOT:-}" != "" ] || [ -f local.properties ]; then
    ./gradlew :snes:testDebugUnitTest :snes:assembleDebug :app:assembleLocalDebug :snes:lintDebug :app:lintLocalDebug
else
    echo "Android SDK not configured; skipped Gradle validation."
fi
