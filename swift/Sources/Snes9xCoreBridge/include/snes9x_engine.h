// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

#ifndef SNES9X_ENGINE_H
#define SNES9X_ENGINE_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct Snes9xEngine Snes9xEngine;

typedef enum Snes9xButton {
    SNES9X_BUTTON_B = 1u << 0,
    SNES9X_BUTTON_Y = 1u << 1,
    SNES9X_BUTTON_SELECT = 1u << 2,
    SNES9X_BUTTON_START = 1u << 3,
    SNES9X_BUTTON_UP = 1u << 4,
    SNES9X_BUTTON_DOWN = 1u << 5,
    SNES9X_BUTTON_LEFT = 1u << 6,
    SNES9X_BUTTON_RIGHT = 1u << 7,
    SNES9X_BUTTON_A = 1u << 8,
    SNES9X_BUTTON_X = 1u << 9,
    SNES9X_BUTTON_L = 1u << 10,
    SNES9X_BUTTON_R = 1u << 11,
} Snes9xButton;

Snes9xEngine *snes9x_engine_create(const char *system_directory);
void snes9x_engine_destroy(Snes9xEngine *engine);
bool snes9x_engine_load_rom(Snes9xEngine *engine, const char *rom_path, const char *battery_path);
void snes9x_engine_unload_rom(Snes9xEngine *engine);
bool snes9x_engine_is_loaded(const Snes9xEngine *engine);
bool snes9x_engine_run_frame(Snes9xEngine *engine);
double snes9x_engine_frame_duration(const Snes9xEngine *engine);

const uint32_t *snes9x_engine_video_buffer(const Snes9xEngine *engine);
size_t snes9x_engine_video_pixel_count(const Snes9xEngine *engine);
uint32_t snes9x_engine_video_width(const Snes9xEngine *engine);
uint32_t snes9x_engine_video_height(const Snes9xEngine *engine);
const int16_t *snes9x_engine_audio_buffer(const Snes9xEngine *engine);
size_t snes9x_engine_audio_frame_count(const Snes9xEngine *engine);
uint32_t snes9x_engine_audio_sample_rate(void);
uint32_t snes9x_engine_audio_channel_count(void);

void snes9x_engine_set_button(Snes9xEngine *engine, unsigned player, Snes9xButton button, bool pressed);
void snes9x_engine_reset_inputs(Snes9xEngine *engine);
void snes9x_engine_reset(Snes9xEngine *engine, bool hard_reset);
bool snes9x_engine_save_state(Snes9xEngine *engine, const char *path);
bool snes9x_engine_load_state(Snes9xEngine *engine, const char *path);
bool snes9x_engine_add_cheat_code(Snes9xEngine *engine, const char *code);
void snes9x_engine_clear_cheats(Snes9xEngine *engine);
const char *snes9x_engine_last_error(const Snes9xEngine *engine);

#ifdef __cplusplus
}
#endif

#endif
