# snes9x

A 16-bit cartridge-console engine for Apple and Android, backed by the unmodified official [Snes9x](https://github.com/snes9xgit/snes9x) core.

```text
snes9x/                    upstream git submodule; read-only
swift/
  Sources/Snes9xCore      read-only symlink into the submodule
  Sources/Snes9xCoreBridge  portable C ABI over Snes9x's official libretro port
  Sources/Snes9x            SwiftUI library for iOS, tvOS, and macOS
android/
  snes9x                  Compose AAR + thin JNI/CMake host
  app                     phone/tablet/TV sample
scripts/                   build, packaging, and validation entry points
```

The shared bridge implements ROM loading, automatic PAL/NTSC timing, dynamic 256/512-pixel video, 32,040 Hz stereo PCM audio, two controllers, SRAM, save states, reset, and Snes9x cheat codes. It also enforces Snes9x's process-global callback constraint by allowing only one native engine per process. ROM content is hashed once at startup so Apple and Android use stable, collision-resistant SRAM identities.

## Apple

Requirements: Xcode 16+, Swift 6 toolchain. Supported deployment targets are iOS 17+, tvOS 17+, and macOS 15+.

```swift
import Snes9x
import SwiftUI

struct GameScreen: View {
    let romURL: URL

    var body: some View {
        Snes9x(rom: romURL)
    }
}
```

```sh
git submodule update --init
SNES9X_BUILD_FROM_SOURCE=1 swift build
SNES9X_BUILD_FROM_SOURCE=1 xcodebuild -scheme snes9x -destination 'generic/platform=iOS' build
```

The public `Snes9x` target is always built from source. `CSnes9xCore` supports two package modes:

- Source mode compiles the bridge and pinned `snes9x/` submodule when `SNES9X_BUILD_FROM_SOURCE=1` is set. It is also the fallback until `Package.swift` contains a real binary checksum.
- Binary mode downloads the pinned framework-based `CSnes9xCore.xcframework.zip`, or consumes a local artifact from `SNES9X_ENGINE_ARTIFACTS_DIR`. Its module map is namespaced inside `CSnes9xCore.framework/Modules`, so it can coexist with other static binary packages in one app.

`Snes9x` starts on appearance and stops on disappearance. `Snes9xView(engine:)` and `Snes9xEngine` provide explicit lifecycle, save-state, reset, cheat, and custom-control access. Apple game controllers and keyboards map D-pad, A/B/X/Y, L/R, Start, and Select for up to two players. Connecting an external controller hides touch controls; tvOS requests a controller instead of showing touch input.

The on-screen controller offers the default adaptive `system` theme plus `snes9x` with the original two-tone-purple palette and `superFamicom` with the original four-color palette. Every on-screen button provides tactile press feedback by default; set `hapticsEnabled` to `false` to disable it. The controller body uses the host app name by default; `controllerLabel` can replace it or hide it with an empty string:

```swift
Snes9x(
    rom: romURL,
    controllerConfiguration: Snes9xControllerConfiguration(
        theme: .superFamicom,
        presentationMode: .automatic,
        controllerLabel: "My App",
        colors: Snes9xControllerColorOverrides(actionButtons: .purple)
    )
)
```

## Android

Requirements: JDK 17+, Android SDK 37, CMake 3.22.1, and NDK 29. The default ABIs are `arm64-v8a,x86_64`; override them with `-Psnes9x.abis=...`.

```kotlin
Snes9x(
    configuration = Snes9xConfiguration(romUri = documentUri),
    modifier = Modifier.fillMaxSize(),
)
```

```sh
./gradlew :snes9x:assembleDebug
./gradlew :app:assembleLocalDebug
```

The library namespace and public Kotlin package are `com.snes9x`; the sample application id is `com.snes9x`. Compose, keyboard, analog stick, D-pad, and physical gamepad input map to the same native masks as Apple. Android TV uses the same controller-only presentation behavior.

Source checkouts use `implementation(project(":snes9x"))`. Release AARs are configured as `io.github.dooop:snes9x:<version>` from GitHub Packages. The sample's `localDebug` variant uses source, `localRelease` uses `-Psnes9x.releaseAar=/absolute/path/to/snes9x-release.aar`, and `maven` variants use the published coordinate. The AAR and sample APK contain all applicable license texts under `assets/licenses/`; Maven releases additionally publish a `complete-source` archive. Apple XCFramework archives embed the license texts and source provenance at their root.

```kotlin
Snes9x(
    configuration = Snes9xConfiguration(romUri = documentUri),
    controllerConfiguration = Snes9xControllerConfiguration(
        theme = Snes9xControllerTheme.Snes9x,
        presentationMode = Snes9xControllerPresentationMode.Automatic,
        controllerLabel = "My App",
        colors = Snes9xControllerColorOverrides(actionButtons = Color(0xFF6850A4)),
    ),
    modifier = Modifier.fillMaxSize(),
)
```

## ROMs and firmware

No ROMs, firmware, BIOS files, or copyrighted game assets are included. Supply legally obtained `.sfc` or `.smc` cartridge images. The embedded upstream core contains its standard enhancement-chip support; special multi-cart and broadcast formats that require external BIOS or companion files are not configured by the initial public wrapper.

## License

This wrapper uses the same non-commercial [Snes9x License](LICENSE) as the core. Its license information and copyright notice must accompany every copy and derived work; commercial users must obtain permission from the Snes9x copyright holders. The compiled NTSC filter's separate terms are copied to [LICENSES/snes_ntsc-license.txt](LICENSES/snes_ntsc-license.txt). This is a technical summary, not legal advice.
