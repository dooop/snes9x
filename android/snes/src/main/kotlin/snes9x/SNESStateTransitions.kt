// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package snes9x

internal fun SNESState.afterPauseRequest(): SNESState =
    when (this) {
        SNESState.Running -> SNESState.Paused
        else -> this
    }

internal fun SNESState.afterResumeRequest(): SNESState =
    when (this) {
        SNESState.Paused -> SNESState.Running
        else -> this
    }
