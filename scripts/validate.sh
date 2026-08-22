#!/bin/sh
set -eu

cmp LICENSE snes9x/LICENSE
cmp LICENSES/snes_ntsc-license.txt snes9x/filter/snes_ntsc-license.txt
cmp LICENSE android/snes/src/main/assets/licenses/Snes9x-License.txt
cmp LICENSES/snes_ntsc-license.txt android/snes/src/main/assets/licenses/snes_ntsc-license.txt

SNES_BUILD_FROM_SOURCE=1 swift package dump-package >/dev/null
SNES_BUILD_FROM_SOURCE=1 swift build
SNES_BUILD_FROM_SOURCE=1 swift test

if [ "${ANDROID_SDK_ROOT:-}" != "" ] || [ -f local.properties ]; then
    ./gradlew :snes:testDebugUnitTest :snes:assembleDebug :app:assembleLocalDebug :snes:lintDebug :app:lintLocalDebug
else
    echo "Android SDK not configured; skipped Gradle validation."
fi
