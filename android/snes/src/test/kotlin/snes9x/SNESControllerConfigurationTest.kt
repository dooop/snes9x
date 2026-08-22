// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package snes9x

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SNESControllerConfigurationTest {
    @Test
    fun defaultsToAdaptiveSystemTheme() {
        val configuration = SNESControllerConfiguration()

        assertEquals(SNESControllerTheme.System, configuration.theme)
        assertEquals(SNESControllerPresentationMode.Automatic, configuration.presentationMode)
        assertTrue(configuration.hapticsEnabled)
        assertEquals(0.72f, configuration.overlayOpacity)
    }

    @Test
    fun offersOriginalThemes() {
        assertEquals(
            SNESControllerTheme.SNES,
            SNESControllerConfiguration(theme = SNESControllerTheme.SNES).theme,
        )
        assertEquals(
            SNESControllerTheme.SuperFamicom,
            SNESControllerConfiguration(theme = SNESControllerTheme.SuperFamicom).theme,
        )
    }

    @Test
    fun buttonsMatchTheNativeSNESContract() {
        assertEquals(0x01, SNESButton.B.mask)
        assertEquals(0x02, SNESButton.Y.mask)
        assertEquals(0x100, SNESButton.A.mask)
        assertEquals(0x200, SNESButton.X.mask)
        assertEquals(0x400, SNESButton.L.mask)
        assertEquals(0x800, SNESButton.R.mask)
    }

    @Test
    fun preservesCustomControllerLabel() {
        assertEquals("My App", SNESControllerConfiguration(controllerLabel = "My App").controllerLabel)
        assertEquals("", SNESControllerConfiguration(controllerLabel = "").controllerLabel)
    }
}
