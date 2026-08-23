#!/bin/sh
set -eu

cmp LICENSE snes9x/LICENSE
cmp LICENSES/snes_ntsc-license.txt snes9x/filter/snes_ntsc-license.txt
cmp LICENSE android/snes9x/src/main/assets/licenses/Snes9x-License.txt
cmp LICENSES/snes_ntsc-license.txt android/snes9x/src/main/assets/licenses/snes_ntsc-license.txt

SNES9X_BUILD_FROM_SOURCE=1 swift package dump-package >/dev/null
SNES9X_BUILD_FROM_SOURCE=1 swift build
SNES9X_BUILD_FROM_SOURCE=1 swift test

if [ "${ANDROID_SDK_ROOT:-}" != "" ] || [ -f local.properties ]; then
    ./gradlew :snes9x:testDebugUnitTest :snes9x:assembleDebug :app:assembleLocalDebug :snes9x:lintDebug :app:lintLocalDebug
else
    echo "Android SDK not configured; skipped Gradle validation."
fi
