package snes9x

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SNESControls(
    engine: SNESEngine,
    modifier: Modifier = Modifier,
    configuration: SNESControllerConfiguration = SNESControllerConfiguration(),
) {
    BoxWithConstraints(modifier = modifier) {
        val mode =
            when (configuration.presentationMode) {
                SNESControllerPresentationMode.Automatic ->
                    if (maxWidth > maxHeight || maxHeight < 300.dp) {
                        SNESControllerPresentationMode.Overlay
                    } else {
                        SNESControllerPresentationMode.Gamepad
                    }
                else -> configuration.presentationMode
            }
        val metrics = ControllerMetrics(compact = maxWidth < 500.dp)
        val palette = controllerPalette(configuration)

        if (mode == SNESControllerPresentationMode.Overlay) {
            OverlayControls(engine, configuration, metrics, palette)
        } else {
            GamepadControls(engine, configuration, metrics, palette)
        }
    }
}

@Composable
private fun GamepadControls(
    engine: SNESEngine,
    configuration: SNESControllerConfiguration,
    metrics: ControllerMetrics,
    palette: ControllerPalette,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.outerPadding, vertical = metrics.outerPadding),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .shadow(14.dp, RoundedCornerShape(24.dp))
                    .background(palette.body, RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                    .padding(metrics.bodyPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            DPad(engine, configuration, metrics, palette, 1f)
            Column(
                modifier = Modifier.padding(horizontal = metrics.sectionSpacing),
                verticalArrangement = Arrangement.spacedBy(metrics.utilitySpacing),
            ) {
                Text(
                    text = if (configuration.theme == SNESControllerTheme.SuperFamicom) "SUPER FAMICOM" else "SNES",
                    color = palette.bodyLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                UtilityButtons(engine, configuration, metrics, palette, 1f)
            }
            ActionButtons(engine, configuration, metrics, palette, 1f)
        }
    }
}

@Composable
private fun OverlayControls(
    engine: SNESEngine,
    configuration: SNESControllerConfiguration,
    metrics: ControllerMetrics,
    palette: ControllerPalette,
) {
    val opacity = configuration.overlayOpacity.coerceIn(0f, 1f)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.outerPadding, vertical = metrics.outerPadding),
    ) {
        DPad(
            engine,
            configuration,
            metrics,
            palette,
            opacity,
            Modifier.align(Alignment.BottomStart),
        )
        UtilityButtons(
            engine,
            configuration,
            metrics,
            palette,
            opacity,
            Modifier.align(Alignment.BottomCenter),
        )
        ActionButtons(
            engine,
            configuration,
            metrics,
            palette,
            opacity,
            Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun DPad(
    engine: SNESEngine,
    configuration: SNESControllerConfiguration,
    metrics: ControllerMetrics,
    palette: ControllerPalette,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ControllerButton(
            "▲",
            SNESButton.Up,
            engine,
            metrics.direction,
            metrics.direction,
            RoundedCornerShape(7.dp),
            palette.directionalPad,
            palette.labels,
            configuration,
            opacity,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControllerButton(
                "◀",
                SNESButton.Left,
                engine,
                metrics.direction,
                metrics.direction,
                RoundedCornerShape(7.dp),
                palette.directionalPad,
                palette.labels,
                configuration,
                opacity,
            )
            Box(
                Modifier
                    .size(metrics.direction)
                    .background(palette.directionalPad.copy(alpha = palette.directionalPad.alpha * opacity)),
            )
            ControllerButton(
                "▶",
                SNESButton.Right,
                engine,
                metrics.direction,
                metrics.direction,
                RoundedCornerShape(7.dp),
                palette.directionalPad,
                palette.labels,
                configuration,
                opacity,
            )
        }
        ControllerButton(
            "▼",
            SNESButton.Down,
            engine,
            metrics.direction,
            metrics.direction,
            RoundedCornerShape(7.dp),
            palette.directionalPad,
            palette.labels,
            configuration,
            opacity,
        )
    }
}

@Composable
private fun UtilityButtons(
    engine: SNESEngine,
    configuration: SNESControllerConfiguration,
    metrics: ControllerMetrics,
    palette: ControllerPalette,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(metrics.utilitySpacing)) {
        ControllerButton(
            "SELECT",
            SNESButton.Select,
            engine,
            metrics.utilityWidth,
            metrics.utilityHeight,
            RoundedCornerShape(50),
            palette.utilityButtons,
            palette.labels,
            configuration,
            opacity,
        )
        ControllerButton(
            "START",
            SNESButton.Start,
            engine,
            metrics.utilityWidth,
            metrics.utilityHeight,
            RoundedCornerShape(50),
            palette.utilityButtons,
            palette.labels,
            configuration,
            opacity,
        )
    }
}

@Composable
private fun ActionButtons(
    engine: SNESEngine,
    configuration: SNESControllerConfiguration,
    metrics: ControllerMetrics,
    palette: ControllerPalette,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.actionSpacing / 2),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.utilitySpacing)) {
            ControllerButton(
                "L",
                SNESButton.L,
                engine,
                metrics.utilityWidth,
                metrics.utilityHeight,
                RoundedCornerShape(50),
                palette.utilityButtons,
                palette.labels,
                configuration,
                opacity,
            )
            ControllerButton(
                "R",
                SNESButton.R,
                engine,
                metrics.utilityWidth,
                metrics.utilityHeight,
                RoundedCornerShape(50),
                palette.utilityButtons,
                palette.labels,
                configuration,
                opacity,
            )
        }
        ControllerButton(
            "X",
            SNESButton.X,
            engine,
            metrics.actionSize,
            metrics.actionSize,
            CircleShape,
            palette.actionButtons,
            palette.labels,
            configuration,
            opacity,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.actionSize)) {
            ControllerButton(
                "Y",
                SNESButton.Y,
                engine,
                metrics.actionSize,
                metrics.actionSize,
                CircleShape,
                palette.actionButtons,
                palette.labels,
                configuration,
                opacity,
            )
            ControllerButton(
                "A",
                SNESButton.A,
                engine,
                metrics.actionSize,
                metrics.actionSize,
                CircleShape,
                palette.actionButtons,
                palette.labels,
                configuration,
                opacity,
            )
        }
        ControllerButton(
            "B",
            SNESButton.B,
            engine,
            metrics.actionSize,
            metrics.actionSize,
            CircleShape,
            palette.actionButtons,
            palette.labels,
            configuration,
            opacity,
        )
    }
}

@Composable
private fun ControllerButton(
    label: String,
    button: SNESButton,
    engine: SNESEngine,
    width: Dp,
    height: Dp,
    shape: Shape,
    color: Color,
    labelColor: Color,
    configuration: SNESControllerConfiguration,
    opacity: Float,
) {
    val surface = color.copy(alpha = color.alpha * opacity)
    Box(
        modifier =
            Modifier
                .size(width, height)
                .shadow(if (configuration.theme == SNESControllerTheme.System) 5.dp else 3.dp, shape)
                .background(surface, shape)
                .border(1.dp, Color.White.copy(alpha = 0.18f * opacity), shape)
                .semantics { contentDescription = label }
                .pointerInput(button) {
                    detectTapGestures(
                        onPress = {
                            engine.setButton(button, true)
                            tryAwaitRelease()
                            engine.setButton(button, false)
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = if (label.length == 1) 20.sp else 9.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

private data class ControllerMetrics(
    val direction: Dp,
    val actionSize: Dp,
    val utilityWidth: Dp,
    val utilityHeight: Dp,
    val sectionSpacing: Dp,
    val utilitySpacing: Dp,
    val actionSpacing: Dp,
    val bodyPadding: Dp,
    val outerPadding: Dp,
) {
    constructor(compact: Boolean) : this(
        direction = if (compact) 38.dp else 48.dp,
        actionSize = if (compact) 50.dp else 64.dp,
        utilityWidth = if (compact) 46.dp else 58.dp,
        utilityHeight = if (compact) 26.dp else 32.dp,
        sectionSpacing = if (compact) 4.dp else 18.dp,
        utilitySpacing = if (compact) 4.dp else 10.dp,
        actionSpacing = if (compact) 8.dp else 16.dp,
        bodyPadding = if (compact) 12.dp else 22.dp,
        outerPadding = if (compact) 8.dp else 20.dp,
    )
}

private data class ControllerPalette(
    val body: Color,
    val directionalPad: Color,
    val actionButtons: Color,
    val utilityButtons: Color,
    val labels: Color,
    val bodyLabel: Color,
)

@Composable
private fun controllerPalette(configuration: SNESControllerConfiguration): ControllerPalette {
    val defaults =
        when (configuration.theme) {
            SNESControllerTheme.System ->
                ControllerPalette(
                    body = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    directionalPad = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    actionButtons = MaterialTheme.colorScheme.primary,
                    utilityButtons = MaterialTheme.colorScheme.secondary,
                    labels = MaterialTheme.colorScheme.onPrimary,
                    bodyLabel = MaterialTheme.colorScheme.onSurface,
                )
            SNESControllerTheme.SNES ->
                ControllerPalette(
                    Color(0xFFB3B3AE),
                    Color(0xFF1A1A1A),
                    Color(0xFFAB0D1F),
                    Color(0xFF292929),
                    Color.White,
                    Color(0xFF262626),
                )
            SNESControllerTheme.SuperFamicom ->
                ControllerPalette(
                    Color(0xFFC7C7C7),
                    Color(0xFF292929),
                    Color(0xFF51408C),
                    Color(0xFF4D4D4D),
                    Color.White,
                    Color(0xFF2E2E2E),
                )
        }
    val overrides = configuration.colors
    return defaults.copy(
        body = overrides.body ?: defaults.body,
        directionalPad = overrides.directionalPad ?: defaults.directionalPad,
        actionButtons = overrides.actionButtons ?: defaults.actionButtons,
        utilityButtons = overrides.utilityButtons ?: defaults.utilityButtons,
        labels = overrides.labels ?: defaults.labels,
        bodyLabel = overrides.bodyLabel ?: defaults.bodyLabel,
    )
}
