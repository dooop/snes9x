// swift-tools-version: 6.0
import Foundation
import PackageDescription

let engineBinaryBaseURL = "https://github.com/dooop/snes/releases/download/0.0.0"
let engineChecksum = "0000000000000000000000000000000000000000000000000000000000000000"
let localEngineArtifactsPath = ProcessInfo.processInfo.environment["SNES_ENGINE_ARTIFACTS_DIR"]
let releasedEngineAvailable = engineChecksum != String(repeating: "0", count: 64)
let buildEngineFromSource =
    ProcessInfo.processInfo.environment["SNES_BUILD_FROM_SOURCE"] != nil
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
            name: "CSNESCore",
            path: "swift/Sources",
            exclude: [
                "SNES",
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
            sources: ["SNESCoreBridge"] + snes9xSources,
            publicHeadersPath: "SNESCoreBridge/include",
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
        .binaryTarget(name: "CSNESCore", path: "\(localEngineArtifactsPath)/CSNESCore.xcframework")
    } else {
        .binaryTarget(
            name: "CSNESCore",
            url: "\(engineBinaryBaseURL)/CSNESCore.xcframework.zip",
            checksum: engineChecksum
        )
    }

let package = Package(
    name: "snes",
    platforms: [.iOS(.v17), .tvOS(.v17), .macOS(.v15)],
    products: [.library(name: "SNES", targets: ["SNES"])],
    targets: [
        .target(
            name: "SNES",
            dependencies: ["CSNESCore"],
            path: "swift/Sources/SNES",
            linkerSettings: [.linkedLibrary("c++")]
        ),
        .testTarget(name: "SNESTests", dependencies: ["SNES", "CSNESCore"], path: "swift/Tests/SNESTests"),
        coreTarget,
    ],
    swiftLanguageModes: [.v5],
    cxxLanguageStandard: .cxx17
)
