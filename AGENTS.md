# Repository instructions

## Mission

- Provide thin SwiftUI and Android Compose wrappers around upstream Snes9x.
- Reuse one portable C/C++ bridge and one unmodified `snes9x/` git submodule.
- Keep the public Apple and Android APIs behaviorally aligned where the platforms permit it.

## Architecture

- `snes9x/`: read-only upstream git submodule.
- `swift/Sources/Snes9xCoreBridge/`: portable C ABI and C++ integration shared by both platforms.
- `swift/Sources/Snes9x/`: SwiftUI API, Apple lifecycle, video, audio, and controller handling.
- `android/snes9x/`: Android library, Compose UI, lifecycle, JNI, and CMake host.
- `android/app/`: sample-only Android application.
- `swift/Tests/` and Android test source sets: wrapper tests; never put tests in the submodule.

## Non-negotiable boundaries

- Never edit, delete, reformat, patch, or generate files under `snes9x/`.
- Preserve one engine per process. Snes9x callback managers are global; every success, failure, cancellation, and disposal path must release the process claim exactly once.
- Serialize all calls that touch a native engine. Do not destroy an engine while frame, audio, input, state, or file callbacks can still use it.
- Put portable emulator behavior in `swift/Sources/Snes9xCoreBridge/`, not in JNI or Swift.
- Keep platform lifecycle, storage access, rendering, audio output, and controls in their platform layer.
- Do not commit `.build/`, `.gradle/`, `.kotlin/`, `**/.cxx/`, `**/build/`, `DerivedData/`, APKs, AARs, archives, or local SDK configuration.
- Do not add ROMs, BIOS images, firmware, copyrighted game assets, secrets, or machine-local paths.

## Change workflow

- Inspect `git status --short` before editing and preserve unrelated user changes.
- Classify the change before choosing a layer: core behavior, Apple host, Android host, or sample-only.
- When the C ABI changes, update the header and implementation together, then audit both Swift imports and Android JNI/Kotlin bindings.
- When a public capability changes on one platform, explicitly check whether the other platform needs the same behavior or documentation.
- Keep JNI limited to type conversion and buffer transfer. Keep Swift and Kotlin wrappers thin.
- Add focused regression tests for state transitions, lifecycle cleanup, input masks, storage identity, and error paths when those areas change.
- Treat hard-coded user-facing strings in library code as localization debt; keep diagnostic messages consistent across platforms.

## Runtime review rules

- Flag invalid state transitions such as `resume` without a loaded engine or `pause` after failure/stop.
- Flag terminal paths that leak the native handle, audio object, callback registration, executor/timer, security-scoped access, or engine claim.
- Flag per-frame allocation or copying added to the hot path unless it is measured and justified.
- Flag save/battery naming based only on a display filename or unstable URI hash; persistent data needs a stable collision-resistant game identity.
- Validate JNI array lengths and null handles at the native boundary even when the current Kotlin caller supplies fixed-size arrays.
- Keep audio/video timing driven by the emulated machine mode and avoid unbounded audio queue growth.
- Reset pressed inputs on pause, focus loss, controller disconnect, stop, and failed startup where applicable.

## Validation

Run the narrowest relevant checks while iterating, then the full affected platform checks before handoff.

- Manifest: `swift package dump-package > /dev/null`
- Apple host: `swift build`
- Apple tests: `swift test`
- iOS: `xcodebuild -scheme snes9x -destination 'generic/platform=iOS' build`
- Android library: `./gradlew :snes9x:assembleDebug`
- Android sample from source: `./gradlew :app:assembleLocalDebug`
- Android sample from AAR: `./gradlew :app:assembleLocalRelease -Psnes9x.releaseAar=/absolute/path/to/snes9x-release.aar`
- Android lint: `./gradlew :snes9x:lintDebug :app:lintLocalDebug`
- Combined local validation: `./scripts/validate.sh`

After validation, run `git status --short` and ensure only intended source/configuration files changed. Generated output must remain ignored and untracked.

## Licensing

Snes9x uses its own non-commercial license. Keep the `LICENSE` link valid, reproduce the license and copyright notice with every copy or derived work, and do not describe the wrapper or linked binaries as GPL-covered. Commercial distribution requires permission from the Snes9x copyright holders; bundled components may have additional licenses.

## Project skills

- Use `$develop-snes-wrappers` for implementation, refactoring, lifecycle, native bridge, SwiftUI, Compose, or JNI work.
- Use `$validate-snes-wrappers` for build verification, lint triage, release readiness, or license packaging checks.
