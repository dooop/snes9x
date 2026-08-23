// swift-tools-version: 6.0
import Foundation
import PackageDescription

let engineBinaryBaseURL = "https://github.com/dooop/snes9x/releases/download/0.1.0"
let engineChecksum = "3db62675f2da9a7290066fb74c5ad402fd4ee77a90d6ebd2d660c4780c3409d4"
let localEngineArtifactsPath = ProcessInfo.processInfo.environment["SNES9X_ENGINE_ARTIFACTS_DIR"]
let releasedEngineAvailable = engineChecksum != String(repeating: "0", count: 64)
let buildEngineFromSource =
    ProcessInfo.processInfo.environment["SNES9X_BUILD_FROM_SOURCE"] != nil
    || (localEngineArtifactsPath == nil && !releasedEngineAvailable)

let snes9xSources = [
    "apu/apu.cpp",
    "apu/bapu/dsp/sdsp.cpp",
    "apu/bapu/smp/smp.cpp",
    "apu/bapu/smp/smp_state.cpp",
    "bsx.cpp", "c4.cpp", "c4emu.cpp", "cheats.cpp", "cheats2.cpp", "clip.cpp",
    "conffile.cpp", "controls.cpp", "cpu.cpp", "cpuexec.cpp", "cpuops.cpp",
    "crosshairs.cpp", "dma.cpp", "dsp.cpp", "dsp1.cpp", "dsp2.cpp", "dsp3.cpp",
    "dsp4.cpp", "fxinst.cpp", "fxemu.cpp", "gfx.cpp", "globals.cpp", "memmap.cpp",
    "obc1.cpp", "msu1.cpp", "ppu.cpp", "stream.cpp", "sa1.cpp", "sa1cpu.cpp",
    "screenshot.cpp", "sdd1.cpp", "sdd1emu.cpp", "seta.cpp", "seta010.cpp",
    "seta011.cpp", "seta018.cpp", "snapshot.cpp", "snes9x.cpp", "spc7110.cpp",
    "srtc.cpp", "tile.cpp", "tileimpl-n1x1.cpp", "tileimpl-n2x1.cpp",
    "tileimpl-h2x1.cpp", "sha256.cpp", "bml.cpp", "movie.cpp", "fscompat.cpp",
    "filter/snes_ntsc.c", "libretro/libretro.cpp",
].map { "Snes9xCore/\($0)" }

let coreTarget: Target =
    if buildEngineFromSource {
        .target(
            name: "CSnes9xCore",
            path: "swift/Sources",
            exclude: [
                "Snes9x",
                "Snes9xCore/.git",
                "Snes9xCore/data",
                "Snes9xCore/docs",
                "Snes9xCore/external",
                "Snes9xCore/gtk",
                "Snes9xCore/jma",
                "Snes9xCore/libretro/jni",
                "Snes9xCore/libretro/libretro-common",
                "Snes9xCore/libretro/msvc",
                "Snes9xCore/macosx",
                "Snes9xCore/qt",
                "Snes9xCore/unix",
                "Snes9xCore/win32",
            ],
            sources: ["Snes9xCoreBridge"] + snes9xSources,
            publicHeadersPath: "Snes9xCoreBridge/include",
            cSettings: [
                .headerSearchPath("Snes9xCore"),
                .headerSearchPath("Snes9xCore/apu"),
                .headerSearchPath("Snes9xCore/apu/bapu"),
                .headerSearchPath("Snes9xCore/libretro"),
                .headerSearchPath("Snes9xCore/libretro/libretro-common/include"),
                .define("__LIBRETRO__"),
                .define("RIGHTSHIFT_IS_SAR"),
                .define("HAVE_STDINT_H"),
                .define("HAVE_STRINGS_H"),
                .define("ALLOW_CPU_OVERCLOCK"),
            ],
            cxxSettings: [
                .headerSearchPath("Snes9xCore"),
                .headerSearchPath("Snes9xCore/apu"),
                .headerSearchPath("Snes9xCore/apu/bapu"),
                .headerSearchPath("Snes9xCore/libretro"),
                .headerSearchPath("Snes9xCore/libretro/libretro-common/include"),
                .define("__LIBRETRO__"),
                .define("RIGHTSHIFT_IS_SAR"),
                .define("HAVE_STDINT_H"),
                .define("HAVE_STRINGS_H"),
                .define("ALLOW_CPU_OVERCLOCK"),
            ]
        )
    } else if let localEngineArtifactsPath {
        .binaryTarget(
            name: "CSnes9xCore", path: "\(localEngineArtifactsPath)/CSnes9xCore.xcframework")
    } else {
        .binaryTarget(
            name: "CSnes9xCore",
            url: "\(engineBinaryBaseURL)/CSnes9xCore.xcframework.zip",
            checksum: engineChecksum
        )
    }

let package = Package(
    name: "snes9x",
    platforms: [.iOS(.v17), .tvOS(.v17), .macOS(.v15)],
    products: [.library(name: "Snes9x", targets: ["Snes9x"])],
    targets: [
        .target(
            name: "Snes9x",
            dependencies: ["CSnes9xCore"],
            path: "swift/Sources/Snes9x",
            linkerSettings: [.linkedLibrary("c++")]
        ),
        .testTarget(
            name: "Snes9xTests", dependencies: ["Snes9x", "CSnes9xCore"],
            path: "swift/Tests/Snes9xTests"),
        coreTarget,
    ],
    swiftLanguageModes: [.v5],
    cxxLanguageStandard: .cxx17
)
