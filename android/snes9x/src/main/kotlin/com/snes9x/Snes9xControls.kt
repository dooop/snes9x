// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Snes9xControls(
    engine: Snes9xEngine,
    modifier: Modifier = Modifier,
    configuration: Snes9xControllerConfiguration = Snes9xControllerConfiguration(),
) {
    BoxWithConstraints(modifier = modifier) {
        val mode =
            when (configuration.presentationMode) {
                Snes9xControllerPresentationMode.Automatic ->
                    if (maxWidth > maxHeight || maxHeight < 300.dp) {
                        Snes9xControllerPresentationMode.Overlay
                    } else {
                        Snes9xControllerPresentationMode.Gamepad
                    }
                else -> configuration.presentationMode
            }
        val metrics = ControllerMetrics(compact = maxWidth < 500.dp)
        val palette = controllerPalette(configuration)

        if (mode == Snes9xControllerPresentationMode.Overlay) {
            OverlayControls(engine, configuration, metrics, palette)
        } else {
            GamepadControls(engine, configuration, metrics, palette)
        }
    }
}

@Composable
private fun GamepadControls(
    engine: Snes9xEngine,
    configuration: Snes9xControllerConfiguration,
    metrics: ControllerMetrics,
    palette: ControllerPalette,
) {
    val controllerLabel = rememberControllerLabel(configuration)
    val bodyShape =
        when (configuration.theme) {
            Snes9xControllerTheme.System -> RoundedCornerShape(24.dp)
            Snes9xControllerTheme.Snes9x,
            Snes9xControllerTheme.SuperFamicom,
            -> RoundedCornerShape(72.dp)
        }
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
                    .shadow(14.dp, bodyShape)
                    .background(palette.body, bodyShape)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), bodyShape)
                    .padding(metrics.bodyPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            DPad(engine, configuration, metrics, palette, 1f)
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = metrics.sectionSpacing)
                        .background(palette.panel.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
                        .padding(horizontal = metrics.utilitySpacing, vertical = metrics.utilitySpacing),
                verticalArrangement = Arrangement.spacedBy(metrics.utilitySpacing),
            ) {
                if (controllerLabel.isNotEmpty()) {
                    Text(
                        text = controllerLabel,
                        color = palette.bodyLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                }
                UtilityButtons(engine, configuration, metrics, palette, 1f)
            }
            ActionButtons(engine, configuration, metrics, palette, 1f)
        }
    }
}

@Composable
private fun OverlayControls(
    engine: Snes9xEngine,
    configuration: Snes9xControllerConfiguration,
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
    engine: Snes9xEngine,
    configuration: Snes9xControllerConfiguration,
    metrics: ControllerMetrics,
    palette: ControllerPalette,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ControllerButton(
            "▲",
            Snes9xButton.Up,
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
                Snes9xButton.Left,
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
                    .background(palette.directionalPad.copy(alpha = palette.directionalPad.alpha * opacity))
                    .padding(metrics.direction * 0.23f)
                    .background(Color.Black.copy(alpha = 0.16f * opacity), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.08f * opacity), CircleShape),
            )
            ControllerButton(
                "▶",
                Snes9xButton.Right,
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
            Snes9xButton.Down,
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
    engine: Snes9xEngine,
    configuration: Snes9xControllerConfiguration,
    metrics: ControllerMetrics,
    palette: ControllerPalette,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(metrics.utilitySpacing)) {
        ControllerButton(
            "SELECT",
            Snes9xButton.Select,
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
            Snes9xButton.Start,
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
    engine: Snes9xEngine,
    configuration: Snes9xControllerConfiguration,
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
                Snes9xButton.L,
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
                Snes9xButton.R,
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
        Box(Modifier.size(metrics.actionSize * 2.5f), contentAlignment = Alignment.Center) {
            Box(Modifier.offset(y = -metrics.actionSize * 0.72f)) {
                ControllerButton(
                    "X",
                    Snes9xButton.X,
                    engine,
                    metrics.actionSize,
                    metrics.actionSize,
                    CircleShape,
                    palette.actionColor("X"),
                    palette.labels,
                    configuration,
                    opacity,
                )
            }
            Box(Modifier.offset(x = -metrics.actionSize * 0.72f)) {
                ControllerButton(
                    "Y",
                    Snes9xButton.Y,
                    engine,
                    metrics.actionSize,
                    metrics.actionSize,
                    CircleShape,
                    palette.actionColor("Y"),
                    palette.labels,
                    configuration,
                    opacity,
                )
            }
            Box(Modifier.offset(x = metrics.actionSize * 0.72f)) {
                ControllerButton(
                    "A",
                    Snes9xButton.A,
                    engine,
                    metrics.actionSize,
                    metrics.actionSize,
                    CircleShape,
                    palette.actionColor("A"),
                    palette.labels,
                    configuration,
                    opacity,
                )
            }
            Box(Modifier.offset(y = metrics.actionSize * 0.72f)) {
                ControllerButton(
                    "B",
                    Snes9xButton.B,
                    engine,
                    metrics.actionSize,
                    metrics.actionSize,
                    CircleShape,
                    palette.actionColor("B"),
                    palette.labels,
                    configuration,
                    opacity,
                )
            }
        }
    }
}

@Composable
private fun ControllerButton(
    label: String,
    button: Snes9xButton,
    engine: Snes9xEngine,
    width: Dp,
    height: Dp,
    shape: Shape,
    color: Color,
    labelColor: Color,
    configuration: Snes9xControllerConfiguration,
    opacity: Float,
) {
    val surface = color.copy(alpha = color.alpha * opacity)
    val hapticFeedback = LocalHapticFeedback.current
    var isPressed by remember(button) { mutableStateOf(false) }
    val pressedScale by
        animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = tween(durationMillis = 80),
            label = "controllerButtonScale",
        )
    Box(
        modifier =
            Modifier
                .size(width, height)
                .graphicsLayer {
                    scaleX = pressedScale
                    scaleY = pressedScale
                    alpha = if (isPressed) 0.88f else 1f
                }.shadow(if (configuration.theme == Snes9xControllerTheme.System) 5.dp else 3.dp, shape)
                .background(surface, shape)
                .border(1.dp, Color.White.copy(alpha = 0.18f * opacity), shape)
                .semantics { contentDescription = label }
                .pointerInput(button, configuration.hapticsEnabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            if (configuration.hapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            engine.setButton(button, true)
                            try {
                                tryAwaitRelease()
                            } finally {
                                engine.setButton(button, false)
                                isPressed = false
                            }
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
    val panel: Color,
    val actionColors: Map<String, Color> = emptyMap(),
)

private fun ControllerPalette.actionColor(label: String): Color = actionColors[label] ?: actionButtons

@Composable
private fun controllerPalette(configuration: Snes9xControllerConfiguration): ControllerPalette {
    val defaults =
        when (configuration.theme) {
            Snes9xControllerTheme.System ->
                ControllerPalette(
                    body = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    directionalPad = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    actionButtons = MaterialTheme.colorScheme.primary,
                    utilityButtons = MaterialTheme.colorScheme.secondary,
                    labels = MaterialTheme.colorScheme.onPrimary,
                    bodyLabel = MaterialTheme.colorScheme.onSurface,
                    panel = MaterialTheme.colorScheme.onSurface,
                )
            Snes9xControllerTheme.SuperFamicom ->
                ControllerPalette(
                    Color(0xFFC7C7C7),
                    Color(0xFF292929),
                    Color(0xFF51408C),
                    Color(0xFF4D4D4D),
                    Color.White,
                    Color(0xFF2E2E2E),
                    Color(0xFFADADAD),
                    mapOf(
                        "X" to Color(0xFF4A66B0),
                        "Y" to Color(0xFF3FAE73),
                        "A" to Color(0xFFD95961),
                        "B" to Color(0xFFD6B54D),
                    ),
                )
            Snes9xControllerTheme.Snes9x ->
                ControllerPalette(
                    Color(0xFFB3B3AE),
                    Color(0xFF1A1A1A),
                    Color(0xFF5C4A8F),
                    Color(0xFF3D3D3D),
                    Color.White,
                    Color(0xFF262626),
                    Color(0xFF9E9E9E),
                    mapOf(
                        "X" to Color(0xFFB8A4D4),
                        "Y" to Color(0xFFB8A4D4),
                        "A" to Color(0xFF5C4A8F),
                        "B" to Color(0xFF5C4A8F),
                    ),
                )
        }
    val overrides = configuration.colors
    return defaults.copy(
        body = overrides.body ?: defaults.body,
        directionalPad = overrides.directionalPad ?: defaults.directionalPad,
        actionButtons = overrides.actionButtons ?: defaults.actionButtons,
        actionColors =
            overrides.actionButtons?.let { color -> defaults.actionColors.mapValues { color } }
                ?: defaults.actionColors,
        utilityButtons = overrides.utilityButtons ?: defaults.utilityButtons,
        labels = overrides.labels ?: defaults.labels,
        bodyLabel = overrides.bodyLabel ?: defaults.bodyLabel,
    )
}
