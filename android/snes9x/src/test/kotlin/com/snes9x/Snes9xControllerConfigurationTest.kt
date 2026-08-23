// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Snes9xControllerConfigurationTest {
    @Test
    fun defaultsToAdaptiveSystemTheme() {
        val configuration = Snes9xControllerConfiguration()

        assertEquals(Snes9xControllerTheme.System, configuration.theme)
        assertEquals(Snes9xControllerPresentationMode.Automatic, configuration.presentationMode)
        assertTrue(configuration.hapticsEnabled)
        assertEquals(0.72f, configuration.overlayOpacity)
    }

    @Test
    fun offersOriginalThemes() {
        assertEquals(
            Snes9xControllerTheme.Snes9x,
            Snes9xControllerConfiguration(theme = Snes9xControllerTheme.Snes9x).theme,
        )
        assertEquals(
            Snes9xControllerTheme.SuperFamicom,
            Snes9xControllerConfiguration(theme = Snes9xControllerTheme.SuperFamicom).theme,
        )
    }

    @Test
    fun buttonsMatchTheNativeSnes9xContract() {
        assertEquals(0x01, Snes9xButton.B.mask)
        assertEquals(0x02, Snes9xButton.Y.mask)
        assertEquals(0x100, Snes9xButton.A.mask)
        assertEquals(0x200, Snes9xButton.X.mask)
        assertEquals(0x400, Snes9xButton.L.mask)
        assertEquals(0x800, Snes9xButton.R.mask)
    }

    @Test
    fun preservesCustomControllerLabel() {
        assertEquals("My App", Snes9xControllerConfiguration(controllerLabel = "My App").controllerLabel)
        assertEquals("", Snes9xControllerConfiguration(controllerLabel = "").controllerLabel)
    }
}
