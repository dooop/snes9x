// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package snes9x

import android.view.InputDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SNESControllerPresentationTest {
    @Test
    fun externalControllerHidesOnScreenControls() {
        assertTrue(shouldShowOnScreenControls(requested = true, isTelevision = false, hasExternalController = false))
        assertFalse(shouldShowOnScreenControls(requested = true, isTelevision = false, hasExternalController = true))
    }

    @Test
    fun televisionNeverShowsControlsAndPromptsWithoutController() {
        assertFalse(shouldShowOnScreenControls(requested = true, isTelevision = true, hasExternalController = false))
        assertTrue(shouldShowControllerConnectionPrompt(isTelevision = true, hasExternalController = false))
        assertFalse(shouldShowControllerConnectionPrompt(isTelevision = true, hasExternalController = true))
    }

    @Test
    fun onlyPhysicalGameControllerSourcesCountAsExternalControllers() {
        assertTrue(isExternalGameControllerSource(InputDevice.SOURCE_GAMEPAD, isVirtual = false))
        assertTrue(isExternalGameControllerSource(InputDevice.SOURCE_JOYSTICK, isVirtual = false))
        assertFalse(isExternalGameControllerSource(InputDevice.SOURCE_KEYBOARD, isVirtual = false))
        assertFalse(isExternalGameControllerSource(InputDevice.SOURCE_GAMEPAD, isVirtual = true))
    }
}
