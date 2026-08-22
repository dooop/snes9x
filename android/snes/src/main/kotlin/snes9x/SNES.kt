package snes9x

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SNES(
    configuration: SNESConfiguration,
    modifier: Modifier = Modifier,
    controllerConfiguration: SNESControllerConfiguration = SNESControllerConfiguration(),
) {
    val context = LocalContext.current
    val engine = remember(configuration) { SNESEngine(context, configuration) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(engine, lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> engine.resume()
                    Lifecycle.Event.ON_PAUSE -> engine.pause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (configuration.automaticallyStarts) engine.start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.close()
        }
    }

    SNESView(
        engine = engine,
        modifier = modifier,
        showsControls = configuration.showsTouchControls,
        controllerConfiguration = controllerConfiguration,
    )
}
