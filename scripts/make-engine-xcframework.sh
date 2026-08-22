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
    library="$SLICES_DIR/$slice/libCSNESCore.a"
    headers="$SLICES_DIR/$slice/Headers"
    test -f "$library" || { echo "Missing slice library: $library" >&2; exit 1; }
    test -f "$headers/snes_engine.h" || { echo "Missing headers: $headers" >&2; exit 1; }
    ARGS+=(-library "$library" -headers "$headers")
done

rm -rf "$OUTPUT_DIR/CSNESCore.xcframework" "$OUTPUT_DIR/CSNESCore.xcframework.zip"
xcodebuild -create-xcframework "${ARGS[@]}" -output "$OUTPUT_DIR/CSNESCore.xcframework"

(
    cd "$OUTPUT_DIR"
    ditto -c -k --sequesterRsrc --keepParent CSNESCore.xcframework CSNESCore.xcframework.zip
)

export SNES_BUILD_FROM_SOURCE=1
swift package --package-path "$REPO_ROOT" compute-checksum \
    "$OUTPUT_DIR/CSNESCore.xcframework.zip" > "$OUTPUT_DIR/checksum.txt"

{
    echo "snes prebuilt CSNESCore"
    echo
    echo "wrapper commit:  $(git -C "$REPO_ROOT" rev-parse HEAD)"
    echo "upstream commit: $(git -C "$REPO_ROOT/snes9x" rev-parse HEAD)"
    echo "upstream source: https://github.com/snes9xgit/snes9x"
    echo
    echo "Snes9x uses its non-commercial upstream license; see LICENSE in the source bundle."
    echo "Commercial distribution requires permission from the Snes9x copyright holders."
} > "$OUTPUT_DIR/SOURCES.txt"

echo "Created CSNESCore.xcframework.zip"
echo "Checksum: $(cat "$OUTPUT_DIR/checksum.txt")"
