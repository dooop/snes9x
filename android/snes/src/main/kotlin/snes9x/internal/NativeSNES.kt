// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package snes9x.internal

internal object NativeSNES {
    init {
        System.loadLibrary("snes")
    }

    external fun create(systemDirectory: String): Long

    external fun destroy(handle: Long)

    external fun loadROM(
        handle: Long,
        romPath: String,
        savePath: String,
    ): Boolean

    external fun runFrame(handle: Long): Boolean

    external fun frameDuration(handle: Long): Double

    external fun videoWidth(handle: Long): Int

    external fun videoHeight(handle: Long): Int

    external fun copyVideo(
        handle: Long,
        destination: IntArray,
    ): Int

    external fun copyAudio(
        handle: Long,
        destination: ShortArray,
    ): Int

    external fun setButton(
        handle: Long,
        player: Int,
        button: Int,
        pressed: Boolean,
    )

    external fun resetInputs(handle: Long)

    external fun reset(
        handle: Long,
        hardReset: Boolean,
    )

    external fun saveState(
        handle: Long,
        path: String,
    ): Boolean

    external fun loadState(
        handle: Long,
        path: String,
    ): Boolean

    external fun addCheat(
        handle: Long,
        code: String,
    ): Boolean

    external fun clearCheats(handle: Long)

    external fun lastError(handle: Long): String
}
