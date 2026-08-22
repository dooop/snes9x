// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package snes9x

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class SNESControllerTheme {
    System,
    SNES,
    SuperFamicom,
}

enum class SNESControllerPresentationMode {
    Automatic,
    Gamepad,
    Overlay,
}

data class SNESControllerColorOverrides(
    val body: Color? = null,
    val directionalPad: Color? = null,
    val actionButtons: Color? = null,
    val utilityButtons: Color? = null,
    val labels: Color? = null,
    val bodyLabel: Color? = null,
)

data class SNESControllerConfiguration(
    val theme: SNESControllerTheme = SNESControllerTheme.System,
    val presentationMode: SNESControllerPresentationMode = SNESControllerPresentationMode.Automatic,
    val colors: SNESControllerColorOverrides = SNESControllerColorOverrides(),
    /** Text on the controller body. Null uses the host app label; an empty string hides it. */
    val controllerLabel: String? = null,
    val hapticsEnabled: Boolean = true,
    val overlayOpacity: Float = 0.72f,
)

@Composable
internal fun rememberControllerLabel(configuration: SNESControllerConfiguration): String {
    val context = LocalContext.current
    val applicationLabel =
        remember(context.applicationContext) {
            context.applicationInfo.loadLabel(context.packageManager).toString()
        }
    return configuration.controllerLabel ?: applicationLabel
}
