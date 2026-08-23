// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class Snes9xControllerTheme {
    System,
    Snes9x,
    SuperFamicom,
}

enum class Snes9xControllerPresentationMode {
    Automatic,
    Gamepad,
    Overlay,
}

data class Snes9xControllerColorOverrides(
    val body: Color? = null,
    val directionalPad: Color? = null,
    val actionButtons: Color? = null,
    val utilityButtons: Color? = null,
    val labels: Color? = null,
    val bodyLabel: Color? = null,
)

data class Snes9xControllerConfiguration(
    val theme: Snes9xControllerTheme = Snes9xControllerTheme.System,
    val presentationMode: Snes9xControllerPresentationMode = Snes9xControllerPresentationMode.Automatic,
    val colors: Snes9xControllerColorOverrides = Snes9xControllerColorOverrides(),
    /** Text on the controller body. Null uses the host app label; an empty string hides it. */
    val controllerLabel: String? = null,
    val hapticsEnabled: Boolean = true,
    val overlayOpacity: Float = 0.72f,
)

@Composable
internal fun rememberControllerLabel(configuration: Snes9xControllerConfiguration): String {
    val context = LocalContext.current
    val applicationLabel =
        remember(context.applicationContext) {
            context.applicationInfo.loadLabel(context.packageManager).toString()
        }
    return configuration.controllerLabel ?: applicationLabel
}
