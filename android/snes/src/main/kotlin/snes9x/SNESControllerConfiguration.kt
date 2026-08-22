package snes9x

import androidx.compose.ui.graphics.Color

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
    val overlayOpacity: Float = 0.72f,
)
