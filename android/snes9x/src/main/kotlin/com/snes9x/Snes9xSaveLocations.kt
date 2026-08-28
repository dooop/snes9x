// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

import java.io.File

internal const val BATTERY_SAVE_EXTENSION = "srm"

internal const val AUTOSAVE_EXTENSION = "state"

internal const val DEFAULT_SAVE_DIRECTORY_NAME = "Saves"

internal const val DEFAULT_AUTOSAVE_DIRECTORY_NAME = "Autosaves"

/** Battery-save directory for [configured], falling back to the default below [runtimeDirectory]. */
internal fun resolveSaveDirectory(
    configured: File?,
    runtimeDirectory: File,
): File = configured ?: File(runtimeDirectory, DEFAULT_SAVE_DIRECTORY_NAME)

/** Autosave directory for [configured], or `null` when autosaving is disabled. */
internal fun resolveAutosaveDirectory(
    enabled: Boolean,
    configured: File?,
    runtimeDirectory: File,
): File? = if (enabled) configured ?: File(runtimeDirectory, DEFAULT_AUTOSAVE_DIRECTORY_NAME) else null

internal fun Snes9xConfiguration.resolveSaveDirectory(runtimeDirectory: File): File =
    resolveSaveDirectory(saveDirectory, runtimeDirectory)

internal fun Snes9xConfiguration.resolveAutosaveDirectory(runtimeDirectory: File): File? =
    resolveAutosaveDirectory(autosaveEnabled, autosaveDirectory, runtimeDirectory)

internal fun Snes9xConfiguration.resolveSaveFile(
    runtimeDirectory: File,
    digest: String,
): File = File(resolveSaveDirectory(runtimeDirectory), "$digest.$BATTERY_SAVE_EXTENSION")

internal fun Snes9xConfiguration.resolveAutosaveFile(
    runtimeDirectory: File,
    digest: String,
): File? = resolveAutosaveDirectory(runtimeDirectory)?.let { File(it, "$digest.$AUTOSAVE_EXTENSION") }
