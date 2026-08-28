// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

import android.net.Uri
import java.io.File

data class Snes9xConfiguration(
    val romUri: Uri,
    /** Directory for battery saves; `null` uses `filesDir/Snes9x/Saves`. */
    val saveDirectory: File? = null,
    /** Writes and restores an automatic save state for the loaded ROM. */
    val autosaveEnabled: Boolean = true,
    /** Directory for automatic save states; `null` uses `filesDir/Snes9x/Autosaves`. */
    val autosaveDirectory: File? = null,
    /** Seconds between automatic save states while the engine is running. */
    val autosaveIntervalSeconds: Long = 30,
    val automaticallyStarts: Boolean = true,
    val showsTouchControls: Boolean = true,
)

sealed interface Snes9xState {
    data object Idle : Snes9xState

    data object Loading : Snes9xState

    data object Running : Snes9xState

    data object Paused : Snes9xState

    data object Stopped : Snes9xState

    data class Failed(
        val message: String,
    ) : Snes9xState
}

enum class Snes9xButton(
    val mask: Int,
) {
    B(0x01),
    Y(0x02),
    Select(0x04),
    Start(0x08),
    Up(0x10),
    Down(0x20),
    Left(0x40),
    Right(0x80),
    A(0x100),
    X(0x200),
    L(0x400),
    R(0x800),
}
