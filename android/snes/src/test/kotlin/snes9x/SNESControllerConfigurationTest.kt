package snes9x

import org.junit.Assert.assertEquals
import org.junit.Test

class SNESControllerConfigurationTest {
    @Test
    fun defaultsToAdaptiveSystemTheme() {
        val configuration = SNESControllerConfiguration()

        assertEquals(SNESControllerTheme.System, configuration.theme)
        assertEquals(SNESControllerPresentationMode.Automatic, configuration.presentationMode)
        assertEquals(0.72f, configuration.overlayOpacity)
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
}
