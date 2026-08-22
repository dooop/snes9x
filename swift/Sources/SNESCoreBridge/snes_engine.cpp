// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

#include "snes_engine.h"

#include "libretro.h"

void S9xReset(void);
void S9xSoftReset(void);

#include <algorithm>
#include <array>
#include <cstdarg>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace {
constexpr std::size_t maxVideoWidth = 512;
constexpr std::size_t maxVideoHeight = 478;
constexpr std::uint32_t audioSampleRate = 32040;
constexpr std::uint32_t audioChannelCount = 2;
constexpr std::size_t maxAudioFrames = 4096;
constexpr unsigned maxPlayers = 2;

std::mutex claimMutex;
bool engineClaimed = false;
SNESEngine *activeEngine = nullptr;

void setError(SNESEngine *engine, const std::string &message);
bool saveBattery(SNESEngine *engine);
bool environmentCallback(unsigned command, void *data);
void videoCallback(const void *data, unsigned width, unsigned height, std::size_t pitch);
void audioSampleCallback(std::int16_t left, std::int16_t right);
std::size_t audioBatchCallback(const std::int16_t *data, std::size_t frames);
void inputPollCallback();
std::int16_t inputStateCallback(unsigned port, unsigned device, unsigned index, unsigned id);
void logCallback(enum retro_log_level level, const char *format, ...);
}

struct SNESEngine {
    std::array<std::uint32_t, maxVideoWidth * maxVideoHeight> videoBuffer{};
    std::vector<std::int16_t> audioBuffer;
    std::array<std::uint32_t, maxPlayers> inputMasks{};
    std::string systemDirectory;
    std::string batteryPath;
    std::string lastError;
    std::uint32_t videoWidth = 256;
    std::uint32_t videoHeight = 224;
    double frameDuration = 1.0 / 60.0988;
    bool loaded = false;

    SNESEngine() { audioBuffer.reserve(maxAudioFrames * audioChannelCount); }
};

namespace {
void setError(SNESEngine *engine, const std::string &message)
{
    if (engine != nullptr) engine->lastError = message;
}

bool environmentCallback(unsigned command, void *data)
{
    switch (command) {
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: {
            if (data == nullptr || activeEngine == nullptr) return false;
            *static_cast<const char **>(data) = activeEngine->systemDirectory.c_str();
            return true;
        }
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            return data != nullptr && *static_cast<retro_pixel_format *>(data) == RETRO_PIXEL_FORMAT_RGB565;
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
            if (data == nullptr) return false;
            *static_cast<int *>(data) = 3;
            return true;
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            if (data == nullptr) return false;
            *static_cast<bool *>(data) = false;
            return true;
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            if (data == nullptr) return false;
            *static_cast<bool *>(data) = false;
            return true;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            if (data == nullptr) return false;
            static_cast<retro_log_callback *>(data)->log = logCallback;
            return true;
        case RETRO_ENVIRONMENT_SET_GEOMETRY:
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
        case RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS:
        case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
            return true;
        default:
            return false;
    }
}

void videoCallback(const void *data, unsigned width, unsigned height, std::size_t pitch)
{
    if (activeEngine == nullptr || data == nullptr || width == 0 || height == 0) return;
    const unsigned copiedWidth = std::min<unsigned>(width, maxVideoWidth);
    const unsigned copiedHeight = std::min<unsigned>(height, maxVideoHeight);
    const auto *bytes = static_cast<const std::uint8_t *>(data);
    for (unsigned y = 0; y < copiedHeight; ++y) {
        const auto *source = reinterpret_cast<const std::uint16_t *>(bytes + y * pitch);
        auto *destination = activeEngine->videoBuffer.data() + y * copiedWidth;
        for (unsigned x = 0; x < copiedWidth; ++x) {
            const std::uint16_t pixel = source[x];
            const std::uint32_t red = ((pixel >> 11) & 0x1f) * 255 / 31;
            const std::uint32_t green = ((pixel >> 5) & 0x3f) * 255 / 63;
            const std::uint32_t blue = (pixel & 0x1f) * 255 / 31;
            destination[x] = red | (green << 8) | (blue << 16) | 0xff000000u;
        }
    }
    activeEngine->videoWidth = copiedWidth;
    activeEngine->videoHeight = copiedHeight;
}

void audioSampleCallback(std::int16_t left, std::int16_t right)
{
    if (activeEngine == nullptr || activeEngine->audioBuffer.size() >= maxAudioFrames * audioChannelCount) return;
    activeEngine->audioBuffer.push_back(left);
    activeEngine->audioBuffer.push_back(right);
}

std::size_t audioBatchCallback(const std::int16_t *data, std::size_t frames)
{
    if (activeEngine == nullptr || data == nullptr) return 0;
    const std::size_t currentFrames = activeEngine->audioBuffer.size() / audioChannelCount;
    const std::size_t acceptedFrames = std::min(frames, maxAudioFrames - currentFrames);
    const std::size_t samples = acceptedFrames * audioChannelCount;
    activeEngine->audioBuffer.insert(activeEngine->audioBuffer.end(), data, data + samples);
    return acceptedFrames;
}

void inputPollCallback() {}

std::int16_t inputStateCallback(unsigned port, unsigned device, unsigned index, unsigned id)
{
    if (activeEngine == nullptr || port >= maxPlayers || device != RETRO_DEVICE_JOYPAD || index != 0 || id >= 32) return 0;
    return (activeEngine->inputMasks[port] & (1u << id)) != 0 ? 1 : 0;
}

void logCallback(enum retro_log_level level, const char *format, ...)
{
    if (activeEngine == nullptr || format == nullptr || level < RETRO_LOG_WARN) return;
    std::array<char, 1024> buffer{};
    va_list arguments;
    va_start(arguments, format);
    std::vsnprintf(buffer.data(), buffer.size(), format, arguments);
    va_end(arguments);
    setError(activeEngine, buffer.data());
}

bool writeAtomically(const char *path, const void *data, std::size_t size)
{
    if (path == nullptr || path[0] == '\0' || data == nullptr) return false;
    const std::string temporaryPath = std::string(path) + ".tmp";
    {
        std::ofstream output(temporaryPath, std::ios::binary | std::ios::trunc);
        if (!output.good()) return false;
        output.write(static_cast<const char *>(data), static_cast<std::streamsize>(size));
        if (!output.good()) return false;
    }
    if (std::rename(temporaryPath.c_str(), path) != 0) {
        std::remove(temporaryPath.c_str());
        return false;
    }
    return true;
}

bool saveBattery(SNESEngine *engine)
{
    if (engine == nullptr || !engine->loaded || engine->batteryPath.empty()) return true;
    const auto saveMemory = [](unsigned type, const std::string &path) {
        const std::size_t size = retro_get_memory_size(type);
        const void *data = retro_get_memory_data(type);
        return size == 0 || (data != nullptr && writeAtomically(path.c_str(), data, size));
    };
    return saveMemory(RETRO_MEMORY_SAVE_RAM, engine->batteryPath)
        && saveMemory(RETRO_MEMORY_RTC, engine->batteryPath + ".rtc");
}

void loadMemory(unsigned type, const std::string &path)
{
    const std::size_t size = retro_get_memory_size(type);
    void *data = retro_get_memory_data(type);
    if (data == nullptr || size == 0) return;
    std::memset(data, 0, size);
    std::ifstream input(path, std::ios::binary);
    if (input.good()) input.read(static_cast<char *>(data), static_cast<std::streamsize>(size));
}
}

SNESEngine *snes_engine_create(const char *systemDirectory)
{
    std::lock_guard<std::mutex> lock(claimMutex);
    if (engineClaimed) return nullptr;
    auto engine = std::make_unique<SNESEngine>();
    engine->systemDirectory = systemDirectory == nullptr || systemDirectory[0] == '\0' ? "." : systemDirectory;
    activeEngine = engine.get();
    engineClaimed = true;
    retro_set_environment(environmentCallback);
    retro_set_video_refresh(videoCallback);
    retro_set_audio_sample(audioSampleCallback);
    retro_set_audio_sample_batch(audioBatchCallback);
    retro_set_input_poll(inputPollCallback);
    retro_set_input_state(inputStateCallback);
    retro_init();
    return engine.release();
}

void snes_engine_destroy(SNESEngine *engine)
{
    if (engine == nullptr) return;
    snes_engine_unload_rom(engine);
    if (activeEngine == engine) {
        retro_deinit();
        activeEngine = nullptr;
    }
    delete engine;
    std::lock_guard<std::mutex> lock(claimMutex);
    engineClaimed = false;
}

bool snes_engine_load_rom(SNESEngine *engine, const char *romPath, const char *batteryPath)
{
    if (engine == nullptr || engine != activeEngine || romPath == nullptr || romPath[0] == '\0') return false;
    if (engine->loaded) snes_engine_unload_rom(engine);
    engine->lastError.clear();
    std::ifstream input(romPath, std::ios::binary | std::ios::ate);
    if (!input.good()) {
        setError(engine, "The ROM could not be opened.");
        return false;
    }
    const std::streamsize length = input.tellg();
    if (length <= 0) {
        setError(engine, "The ROM is empty.");
        return false;
    }
    input.seekg(0);
    const std::size_t romSize = static_cast<std::size_t>(length);
    if (romSize < 0x8000) {
        setError(engine, "The ROM is smaller than the minimum supported SNES cartridge size.");
        return false;
    }
    std::vector<std::uint8_t> rom(romSize);
    if (!input.read(reinterpret_cast<char *>(rom.data()), length)) {
        setError(engine, "The ROM could not be read.");
        return false;
    }
    // The upstream libretro adapter probes both LoROM and HiROM header offsets
    // before its loader mirrors small cartridges. Keep those probes in-bounds
    // without changing the logical ROM size reported to Snes9x.
    if (rom.size() < 0x10000) {
        rom.resize(0x10000);
        for (std::size_t index = romSize; index < rom.size(); ++index) rom[index] = rom[index % romSize];
    }

    retro_game_info game{};
    game.path = romPath;
    game.data = rom.data();
    game.size = romSize;
    if (!retro_load_game(&game)) {
        if (engine->lastError.empty()) setError(engine, "Snes9x rejected the ROM.");
        return false;
    }

    engine->batteryPath = batteryPath == nullptr ? "" : batteryPath;
    engine->loaded = true;
    engine->inputMasks.fill(0);
    if (!engine->batteryPath.empty()) {
        loadMemory(RETRO_MEMORY_SAVE_RAM, engine->batteryPath);
        loadMemory(RETRO_MEMORY_RTC, engine->batteryPath + ".rtc");
    }

    retro_system_av_info info{};
    retro_get_system_av_info(&info);
    engine->frameDuration = info.timing.fps > 0 ? 1.0 / info.timing.fps : 1.0 / 60.0988;
    engine->videoWidth = info.geometry.base_width;
    engine->videoHeight = info.geometry.base_height;
    return true;
}

void snes_engine_unload_rom(SNESEngine *engine)
{
    if (engine == nullptr || !engine->loaded) return;
    if (!saveBattery(engine)) setError(engine, "The battery save could not be written.");
    retro_unload_game();
    engine->loaded = false;
    engine->batteryPath.clear();
    engine->inputMasks.fill(0);
    engine->audioBuffer.clear();
}

bool snes_engine_is_loaded(const SNESEngine *engine) { return engine != nullptr && engine->loaded; }
bool snes_engine_run_frame(SNESEngine *engine)
{
    if (engine == nullptr || engine != activeEngine || !engine->loaded) return false;
    engine->audioBuffer.clear();
    retro_run();
    return true;
}

double snes_engine_frame_duration(const SNESEngine *engine) { return engine == nullptr ? 1.0 / 60.0988 : engine->frameDuration; }
const std::uint32_t *snes_engine_video_buffer(const SNESEngine *engine) { return engine == nullptr ? nullptr : engine->videoBuffer.data(); }
std::size_t snes_engine_video_pixel_count(const SNESEngine *engine) { return engine == nullptr ? 0 : engine->videoWidth * engine->videoHeight; }
std::uint32_t snes_engine_video_width(const SNESEngine *engine) { return engine == nullptr ? 0 : engine->videoWidth; }
std::uint32_t snes_engine_video_height(const SNESEngine *engine) { return engine == nullptr ? 0 : engine->videoHeight; }
const std::int16_t *snes_engine_audio_buffer(const SNESEngine *engine) { return engine == nullptr || engine->audioBuffer.empty() ? nullptr : engine->audioBuffer.data(); }
std::size_t snes_engine_audio_frame_count(const SNESEngine *engine) { return engine == nullptr ? 0 : engine->audioBuffer.size() / audioChannelCount; }
std::uint32_t snes_engine_audio_sample_rate(void) { return audioSampleRate; }
std::uint32_t snes_engine_audio_channel_count(void) { return audioChannelCount; }

void snes_engine_set_button(SNESEngine *engine, unsigned player, SNESButton button, bool pressed)
{
    if (engine == nullptr || player >= maxPlayers) return;
    if (pressed) engine->inputMasks[player] |= static_cast<std::uint32_t>(button);
    else engine->inputMasks[player] &= ~static_cast<std::uint32_t>(button);
}

void snes_engine_reset_inputs(SNESEngine *engine) { if (engine != nullptr) engine->inputMasks.fill(0); }
void snes_engine_reset(SNESEngine *engine, bool hardReset)
{
    if (engine == nullptr || !engine->loaded) return;
    if (hardReset) S9xReset();
    else S9xSoftReset();
}

bool snes_engine_save_state(SNESEngine *engine, const char *path)
{
    if (engine == nullptr || !engine->loaded || path == nullptr) return false;
    const std::size_t size = retro_serialize_size();
    if (size == 0) return false;
    std::vector<std::uint8_t> state(size);
    return retro_serialize(state.data(), state.size()) && writeAtomically(path, state.data(), state.size());
}

bool snes_engine_load_state(SNESEngine *engine, const char *path)
{
    if (engine == nullptr || !engine->loaded || path == nullptr) return false;
    std::ifstream input(path, std::ios::binary | std::ios::ate);
    if (!input.good()) return false;
    const std::streamsize length = input.tellg();
    if (length <= 0) return false;
    input.seekg(0);
    std::vector<std::uint8_t> state(static_cast<std::size_t>(length));
    return input.read(reinterpret_cast<char *>(state.data()), length).good() && retro_unserialize(state.data(), state.size());
}

bool snes_engine_add_cheat_code(SNESEngine *engine, const char *code)
{
    if (engine == nullptr || !engine->loaded || code == nullptr || code[0] == '\0' || std::strlen(code) >= 255) return false;
    retro_cheat_set(0, true, code);
    return true;
}

void snes_engine_clear_cheats(SNESEngine *engine) { if (engine != nullptr && engine->loaded) retro_cheat_reset(); }
const char *snes_engine_last_error(const SNESEngine *engine) { return engine == nullptr ? "The engine is unavailable or already in use." : engine->lastError.c_str(); }
