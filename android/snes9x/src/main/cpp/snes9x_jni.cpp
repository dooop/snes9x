// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

#include <jni.h>

#include <cstdint>
#include <string>

#include "snes9x_engine.h"

namespace {
Snes9xEngine *fromHandle(jlong handle) { return reinterpret_cast<Snes9xEngine *>(handle); }

std::string toString(JNIEnv *env, jstring value)
{
    if (value == nullptr) return {};
    const char *characters = env->GetStringUTFChars(value, nullptr);
    std::string result(characters == nullptr ? "" : characters);
    if (characters != nullptr) env->ReleaseStringUTFChars(value, characters);
    return result;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_snes9x_internal_NativeSnes9x_create(JNIEnv *env, jobject, jstring systemDirectory)
{
    const std::string directory = toString(env, systemDirectory);
    return reinterpret_cast<jlong>(snes9x_engine_create(directory.c_str()));
}

extern "C" JNIEXPORT void JNICALL
Java_com_snes9x_internal_NativeSnes9x_destroy(JNIEnv *, jobject, jlong handle)
{
    if (handle != 0) snes9x_engine_destroy(fromHandle(handle));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_snes9x_internal_NativeSnes9x_loadROM(JNIEnv *env, jobject, jlong handle, jstring romPath, jstring savePath)
{
    if (handle == 0) return false;
    const std::string rom = toString(env, romPath);
    const std::string save = toString(env, savePath);
    return snes9x_engine_load_rom(fromHandle(handle), rom.c_str(), save.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_snes9x_internal_NativeSnes9x_runFrame(JNIEnv *, jobject, jlong handle)
{
    return handle != 0 && snes9x_engine_run_frame(fromHandle(handle));
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_snes9x_internal_NativeSnes9x_frameDuration(JNIEnv *, jobject, jlong handle)
{
    return handle == 0 ? 0.0 : snes9x_engine_frame_duration(fromHandle(handle));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_snes9x_internal_NativeSnes9x_videoWidth(JNIEnv *, jobject, jlong handle)
{
    return handle == 0 ? 0 : static_cast<jint>(snes9x_engine_video_width(fromHandle(handle)));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_snes9x_internal_NativeSnes9x_videoHeight(JNIEnv *, jobject, jlong handle)
{
    return handle == 0 ? 0 : static_cast<jint>(snes9x_engine_video_height(fromHandle(handle)));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_snes9x_internal_NativeSnes9x_copyVideo(JNIEnv *env, jobject, jlong handle, jintArray destination)
{
    if (handle == 0 || destination == nullptr) return 0;
    const uint32_t *source = snes9x_engine_video_buffer(fromHandle(handle));
    if (source == nullptr) return 0;
    const jsize requested = static_cast<jsize>(snes9x_engine_video_pixel_count(fromHandle(handle)));
    const jsize capacity = env->GetArrayLength(destination);
    if (capacity < requested) return 0;
    jint *pixels = env->GetIntArrayElements(destination, nullptr);
    if (pixels == nullptr) return 0;
    for (jsize index = 0; index < requested; ++index) {
        const uint32_t rgba = source[index];
        const uint32_t red = rgba & 0xff;
        const uint32_t green = (rgba >> 8) & 0xff;
        const uint32_t blue = (rgba >> 16) & 0xff;
        pixels[index] = static_cast<jint>(0xff000000u | (red << 16) | (green << 8) | blue);
    }
    env->ReleaseIntArrayElements(destination, pixels, 0);
    return requested;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_snes9x_internal_NativeSnes9x_copyAudio(JNIEnv *env, jobject, jlong handle, jshortArray destination)
{
    if (handle == 0 || destination == nullptr) return 0;
    const int16_t *source = snes9x_engine_audio_buffer(fromHandle(handle));
    const jsize requested = static_cast<jsize>(snes9x_engine_audio_frame_count(fromHandle(handle)) * 2);
    const jsize capacity = env->GetArrayLength(destination);
    if (source == nullptr || capacity < requested) return 0;
    env->SetShortArrayRegion(destination, 0, requested, reinterpret_cast<const jshort *>(source));
    return requested;
}

extern "C" JNIEXPORT void JNICALL
Java_com_snes9x_internal_NativeSnes9x_setButton(JNIEnv *, jobject, jlong handle, jint player, jint button, jboolean pressed)
{
    if (handle != 0 && player >= 0 && player < 2) {
        snes9x_engine_set_button(fromHandle(handle), static_cast<unsigned>(player), static_cast<Snes9xButton>(button), pressed);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_snes9x_internal_NativeSnes9x_resetInputs(JNIEnv *, jobject, jlong handle)
{
    if (handle != 0) snes9x_engine_reset_inputs(fromHandle(handle));
}

extern "C" JNIEXPORT void JNICALL
Java_com_snes9x_internal_NativeSnes9x_reset(JNIEnv *, jobject, jlong handle, jboolean hardReset)
{
    if (handle != 0) snes9x_engine_reset(fromHandle(handle), hardReset);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_snes9x_internal_NativeSnes9x_saveState(JNIEnv *env, jobject, jlong handle, jstring path)
{
    const std::string value = toString(env, path);
    return handle != 0 && snes9x_engine_save_state(fromHandle(handle), value.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_snes9x_internal_NativeSnes9x_loadState(JNIEnv *env, jobject, jlong handle, jstring path)
{
    const std::string value = toString(env, path);
    return handle != 0 && snes9x_engine_load_state(fromHandle(handle), value.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_snes9x_internal_NativeSnes9x_addCheat(JNIEnv *env, jobject, jlong handle, jstring code)
{
    const std::string value = toString(env, code);
    return handle != 0 && snes9x_engine_add_cheat_code(fromHandle(handle), value.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_snes9x_internal_NativeSnes9x_clearCheats(JNIEnv *, jobject, jlong handle)
{
    if (handle != 0) snes9x_engine_clear_cheats(fromHandle(handle));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_snes9x_internal_NativeSnes9x_lastError(JNIEnv *env, jobject, jlong handle)
{
    return env->NewStringUTF(snes9x_engine_last_error(fromHandle(handle)));
}
