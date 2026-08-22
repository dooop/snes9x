// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

#ifndef SNES_ENGINE_H
#define SNES_ENGINE_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct SNESEngine SNESEngine;

typedef enum SNESButton {
    SNES_BUTTON_B = 1u << 0,
    SNES_BUTTON_Y = 1u << 1,
    SNES_BUTTON_SELECT = 1u << 2,
    SNES_BUTTON_START = 1u << 3,
    SNES_BUTTON_UP = 1u << 4,
    SNES_BUTTON_DOWN = 1u << 5,
    SNES_BUTTON_LEFT = 1u << 6,
    SNES_BUTTON_RIGHT = 1u << 7,
    SNES_BUTTON_A = 1u << 8,
    SNES_BUTTON_X = 1u << 9,
    SNES_BUTTON_L = 1u << 10,
    SNES_BUTTON_R = 1u << 11,
} SNESButton;

SNESEngine *snes_engine_create(const char *system_directory);
void snes_engine_destroy(SNESEngine *engine);
bool snes_engine_load_rom(SNESEngine *engine, const char *rom_path, const char *battery_path);
void snes_engine_unload_rom(SNESEngine *engine);
bool snes_engine_is_loaded(const SNESEngine *engine);
bool snes_engine_run_frame(SNESEngine *engine);
double snes_engine_frame_duration(const SNESEngine *engine);

const uint32_t *snes_engine_video_buffer(const SNESEngine *engine);
size_t snes_engine_video_pixel_count(const SNESEngine *engine);
uint32_t snes_engine_video_width(const SNESEngine *engine);
uint32_t snes_engine_video_height(const SNESEngine *engine);
const int16_t *snes_engine_audio_buffer(const SNESEngine *engine);
size_t snes_engine_audio_frame_count(const SNESEngine *engine);
uint32_t snes_engine_audio_sample_rate(void);
uint32_t snes_engine_audio_channel_count(void);

void snes_engine_set_button(SNESEngine *engine, unsigned player, SNESButton button, bool pressed);
void snes_engine_reset_inputs(SNESEngine *engine);
void snes_engine_reset(SNESEngine *engine, bool hard_reset);
bool snes_engine_save_state(SNESEngine *engine, const char *path);
bool snes_engine_load_state(SNESEngine *engine, const char *path);
bool snes_engine_add_cheat_code(SNESEngine *engine, const char *code);
void snes_engine_clear_cheats(SNESEngine *engine);
const char *snes_engine_last_error(const SNESEngine *engine);

#ifdef __cplusplus
}
#endif

#endif
