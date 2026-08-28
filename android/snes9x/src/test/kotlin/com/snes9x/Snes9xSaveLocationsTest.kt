// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class Snes9xSaveLocationsTest {
    private val runtimeDirectory = File("/data/user/0/com.snes9x/files/Snes9x")
    private val digest = "a".repeat(64)

    @Test
    fun unconfiguredDirectoriesLiveBesideEachOtherInTheRuntimeDirectory() {
        assertEquals(
            File(runtimeDirectory, "Saves"),
            resolveSaveDirectory(configured = null, runtimeDirectory = runtimeDirectory),
        )
        assertEquals(
            File(runtimeDirectory, "Autosaves"),
            resolveAutosaveDirectory(enabled = true, configured = null, runtimeDirectory = runtimeDirectory),
        )
    }

    @Test
    fun configuredDirectoriesReplaceTheDefaults() {
        val saves = File("/sdcard/Snes9xSaves")
        val autosaves = File("/sdcard/Snes9xAutosaves")

        assertEquals(saves, resolveSaveDirectory(configured = saves, runtimeDirectory = runtimeDirectory))
        assertEquals(
            autosaves,
            resolveAutosaveDirectory(enabled = true, configured = autosaves, runtimeDirectory = runtimeDirectory),
        )
    }

    @Test
    fun disabledAutosaveResolvesNoDirectoryEvenWhenOneIsConfigured() {
        assertNull(
            resolveAutosaveDirectory(
                enabled = false,
                configured = File("/sdcard/Snes9xAutosaves"),
                runtimeDirectory = runtimeDirectory,
            ),
        )
    }

    @Test
    fun saveAndAutosaveShareTheGameDigestButNotTheExtension() {
        val saveDirectory = resolveSaveDirectory(configured = null, runtimeDirectory = runtimeDirectory)
        val autosaveDirectory =
            requireNotNull(
                resolveAutosaveDirectory(
                    enabled = true,
                    configured = null,
                    runtimeDirectory = runtimeDirectory,
                ),
            )

        assertEquals("$digest.$BATTERY_SAVE_EXTENSION", File(saveDirectory, "$digest.$BATTERY_SAVE_EXTENSION").name)
        assertEquals("$digest.$AUTOSAVE_EXTENSION", File(autosaveDirectory, "$digest.$AUTOSAVE_EXTENSION").name)
        assertEquals(runtimeDirectory, saveDirectory.parentFile)
        assertEquals(runtimeDirectory, autosaveDirectory.parentFile)
    }
}
