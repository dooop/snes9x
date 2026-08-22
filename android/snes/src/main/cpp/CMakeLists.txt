cmake_minimum_required(VERSION 3.22.1)
project(snes LANGUAGES C CXX)

get_filename_component(PROJECT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../.." ABSOLUTE)
set(SNES9X_CORE "${PROJECT_ROOT}/snes9x")
set(COMMON_BRIDGE "${PROJECT_ROOT}/swift/Sources/SNESCoreBridge")

set(
    SNES9X_SOURCES
    apu/apu.cpp apu/bapu/dsp/sdsp.cpp apu/bapu/smp/smp.cpp apu/bapu/smp/smp_state.cpp
    bsx.cpp c4.cpp c4emu.cpp cheats.cpp cheats2.cpp clip.cpp conffile.cpp controls.cpp
    cpu.cpp cpuexec.cpp cpuops.cpp crosshairs.cpp dma.cpp dsp.cpp dsp1.cpp dsp2.cpp dsp3.cpp dsp4.cpp
    fxinst.cpp fxemu.cpp gfx.cpp globals.cpp memmap.cpp obc1.cpp msu1.cpp ppu.cpp stream.cpp
    sa1.cpp sa1cpu.cpp screenshot.cpp sdd1.cpp sdd1emu.cpp seta.cpp seta010.cpp seta011.cpp
    seta018.cpp snapshot.cpp snes9x.cpp spc7110.cpp srtc.cpp tile.cpp tileimpl-n1x1.cpp
    tileimpl-n2x1.cpp tileimpl-h2x1.cpp sha256.cpp bml.cpp movie.cpp fscompat.cpp
    filter/snes_ntsc.c libretro/libretro.cpp
)
list(TRANSFORM SNES9X_SOURCES PREPEND "${SNES9X_CORE}/")

add_library(snes SHARED snes_jni.cpp "${COMMON_BRIDGE}/snes_engine.cpp" ${SNES9X_SOURCES})
target_include_directories(
    snes
    PRIVATE
    "${COMMON_BRIDGE}/include"
    "${SNES9X_CORE}"
    "${SNES9X_CORE}/apu"
    "${SNES9X_CORE}/apu/bapu"
    "${SNES9X_CORE}/libretro"
    "${SNES9X_CORE}/libretro/libretro-common/include"
)
target_compile_definitions(
    snes
    PRIVATE
    __LIBRETRO__
    RIGHTSHIFT_IS_SAR
    HAVE_STDINT_H
    HAVE_STRINGS_H
    ALLOW_CPU_OVERCLOCK
)
target_compile_options(snes PRIVATE -Wno-deprecated-declarations -Wno-unused-parameter)
target_link_libraries(snes PRIVATE log)
