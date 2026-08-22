package snes9x

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SNESView(
    engine: SNESEngine,
    modifier: Modifier = Modifier,
    showsControls: Boolean = true,
    controllerConfiguration: SNESControllerConfiguration = SNESControllerConfiguration(),
) {
    val frame by engine.frame.collectAsStateWithLifecycle()
    val state by engine.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val deviceConfiguration = LocalConfiguration.current
    val isTelevision =
        (deviceConfiguration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    val hasExternalController = rememberHasExternalController()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier =
            modifier
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    val button = event.nativeKeyEvent.toSNESButton() ?: return@onKeyEvent false
                    engine.setButton(button, event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN)
                    true
                },
        contentAlignment = Alignment.Center,
    ) {
        frame?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "SNES video",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
            )
        } ?: when (val current = state) {
            SNESState.Loading -> CircularProgressIndicator(color = Color.White)
            is SNESState.Failed -> Text(current.message, color = Color.White)
            else -> Text("SNES", color = Color.Gray)
        }

        if (hasExternalController || isTelevision) {
            SNESExternalControllerInput(engine, Modifier.fillMaxSize())
        }

        if (shouldShowOnScreenControls(showsControls, isTelevision, hasExternalController)) {
            SNESControls(
                engine = engine,
                configuration = controllerConfiguration,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize()
                        .wrapContentSize(Alignment.BottomCenter)
                        .padding(20.dp),
            )
        }

        if (shouldShowControllerConnectionPrompt(isTelevision, hasExternalController)) {
            Column(
                modifier =
                    Modifier
                        .background(Color.Black.copy(alpha = 0.72f), MaterialTheme.shapes.large)
                        .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎮", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Connect a controller to play",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

internal fun shouldShowOnScreenControls(
    requested: Boolean,
    isTelevision: Boolean,
    hasExternalController: Boolean,
): Boolean = requested && !isTelevision && !hasExternalController

internal fun shouldShowControllerConnectionPrompt(
    isTelevision: Boolean,
    hasExternalController: Boolean,
): Boolean = isTelevision && !hasExternalController

private fun KeyEvent.toSNESButton(): SNESButton? =
    when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> SNESButton.Up
        KeyEvent.KEYCODE_DPAD_DOWN -> SNESButton.Down
        KeyEvent.KEYCODE_DPAD_LEFT -> SNESButton.Left
        KeyEvent.KEYCODE_DPAD_RIGHT -> SNESButton.Right
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_X -> SNESButton.A
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_Z -> SNESButton.B
        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_S -> SNESButton.X
        KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_A -> SNESButton.Y
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q -> SNESButton.L
        KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_W -> SNESButton.R
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> SNESButton.Start
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_SPACE -> SNESButton.Select
        else -> null
    }
