// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

internal fun Snes9xState.afterPauseRequest(): Snes9xState =
    when (this) {
        Snes9xState.Running -> Snes9xState.Paused
        else -> this
    }

internal fun Snes9xState.afterResumeRequest(): Snes9xState =
    when (this) {
        Snes9xState.Paused -> Snes9xState.Running
        else -> this
    }
