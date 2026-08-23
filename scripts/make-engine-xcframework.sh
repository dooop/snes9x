#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <slices-directory> <output-directory>" >&2
    exit 2
fi

SLICES_DIR="$(cd "$1" && pwd)"
OUTPUT_DIR="$2"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

case "$OUTPUT_DIR" in
    ""|"/"|"."|"$REPO_ROOT"|"$REPO_ROOT/")
        echo "Refusing unsafe output directory: $OUTPUT_DIR" >&2
        exit 2
        ;;
esac

mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

ARGS=()
for slice in \
    ios-arm64 \
    ios-arm64_x86_64-simulator \
    tvos-arm64 \
    tvos-arm64_x86_64-simulator \
    macos-arm64_x86_64; do
    framework="$SLICES_DIR/$slice/CSnes9xCore.framework"
    test -f "$framework/CSnes9xCore" || { echo "Missing slice framework: $framework" >&2; exit 1; }
    test -f "$framework/Headers/snes9x_engine.h" || { echo "Missing framework headers: $framework" >&2; exit 1; }
    test -f "$framework/Modules/module.modulemap" || { echo "Missing framework module map: $framework" >&2; exit 1; }
    ARGS+=(-framework "$framework")
done

rm -rf "$OUTPUT_DIR/CSnes9xCore.xcframework" "$OUTPUT_DIR/CSnes9xCore.xcframework.zip"
xcodebuild -create-xcframework "${ARGS[@]}" -output "$OUTPUT_DIR/CSnes9xCore.xcframework"

if find "$OUTPUT_DIR/CSnes9xCore.xcframework" -path '*/Headers/module.modulemap' -print -quit | grep -q .; then
    echo "Framework module maps must not be placed in Headers; that collides with other binary packages." >&2
    exit 1
fi
MODULE_MAP_COUNT="$(find "$OUTPUT_DIR/CSnes9xCore.xcframework" -path '*/CSnes9xCore.framework/Modules/module.modulemap' | wc -l | tr -d ' ')"
test "$MODULE_MAP_COUNT" -eq 5 || {
    echo "Expected 5 namespaced framework module maps, found $MODULE_MAP_COUNT." >&2
    exit 1
}

{
    echo "snes9x prebuilt CSnes9xCore"
    echo
    echo "wrapper commit:  $(git -C "$REPO_ROOT" rev-parse HEAD)"
    echo "upstream commit: $(git -C "$REPO_ROOT/snes9x" rev-parse HEAD)"
    echo "upstream source: https://github.com/snes9xgit/snes9x"
    echo
    echo "Snes9x uses its non-commercial upstream license; see LICENSE in the source bundle."
    echo "Commercial distribution requires permission from the Snes9x copyright holders."
} > "$OUTPUT_DIR/SOURCES.txt"

mkdir -p "$OUTPUT_DIR/CSnes9xCore.xcframework/LICENSES"
cp "$REPO_ROOT/LICENSE" "$OUTPUT_DIR/CSnes9xCore.xcframework/LICENSE"
cp "$REPO_ROOT/LICENSES/snes_ntsc-license.txt" \
    "$OUTPUT_DIR/CSnes9xCore.xcframework/LICENSES/snes_ntsc-license.txt"
cp "$OUTPUT_DIR/SOURCES.txt" "$OUTPUT_DIR/CSnes9xCore.xcframework/SOURCES.txt"

(
    cd "$OUTPUT_DIR"
    ditto -c -k --sequesterRsrc --keepParent CSnes9xCore.xcframework CSnes9xCore.xcframework.zip
)
unzip -p "$OUTPUT_DIR/CSnes9xCore.xcframework.zip" CSnes9xCore.xcframework/LICENSE | cmp "$REPO_ROOT/LICENSE" -
unzip -p "$OUTPUT_DIR/CSnes9xCore.xcframework.zip" \
    CSnes9xCore.xcframework/LICENSES/snes_ntsc-license.txt | cmp "$REPO_ROOT/LICENSES/snes_ntsc-license.txt" -

export SNES9X_BUILD_FROM_SOURCE=1
swift package --package-path "$REPO_ROOT" compute-checksum \
    "$OUTPUT_DIR/CSnes9xCore.xcframework.zip" > "$OUTPUT_DIR/checksum.txt"

echo "Created CSnes9xCore.xcframework.zip"
echo "Checksum: $(cat "$OUTPUT_DIR/checksum.txt")"
