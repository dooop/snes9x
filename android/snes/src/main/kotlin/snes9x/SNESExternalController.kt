package snes9x

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

@Composable
internal fun rememberHasExternalController(): Boolean {
    val context = LocalContext.current
    val inputManager = remember(context) { context.getSystemService(InputManager::class.java) }
    var connected by remember(inputManager) { mutableStateOf(inputManager.hasExternalController()) }

    DisposableEffect(inputManager) {
        val listener =
            object : InputManager.InputDeviceListener {
                private fun refresh() {
                    connected = inputManager.hasExternalController()
                }

                override fun onInputDeviceAdded(deviceId: Int) = refresh()

                override fun onInputDeviceRemoved(deviceId: Int) = refresh()

                override fun onInputDeviceChanged(deviceId: Int) = refresh()
            }
        inputManager.registerInputDeviceListener(listener, null)
        connected = inputManager.hasExternalController()
        onDispose { inputManager.unregisterInputDeviceListener(listener) }
    }

    return connected
}

@Composable
internal fun SNESExternalControllerInput(
    engine: SNESEngine,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context -> SNESControllerInputView(context).also { it.engine = engine } },
        update = { it.engine = engine },
        modifier = modifier,
    )
}

internal fun isExternalGameControllerSource(
    sources: Int,
    isVirtual: Boolean,
): Boolean =
    !isVirtual &&
        (
            ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
                ((sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
        )

private fun InputManager.hasExternalController(): Boolean =
    inputDeviceIds.any { deviceId ->
        getInputDevice(deviceId)?.let {
            isExternalGameControllerSource(it.sources, it.isVirtual)
        } == true
    }

private class SNESControllerInputView(
    context: Context,
) : View(context),
    InputManager.InputDeviceListener {
    var engine: SNESEngine? = null
    private val inputManager = context.getSystemService(InputManager::class.java)
    private val devicePlayers = mutableMapOf<Int, Int>()
    private val directions = mutableMapOf<Int, Set<SNESButton>>()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        inputManager.registerInputDeviceListener(this, null)
        post(::requestFocus)
    }

    override fun onDetachedFromWindow() {
        inputManager.unregisterInputDeviceListener(this)
        devicePlayers.values.toSet().forEach(::releasePlayer)
        devicePlayers.clear()
        directions.clear()
        super.onDetachedFromWindow()
    }

    override fun onInputDeviceAdded(deviceId: Int) = Unit

    override fun onInputDeviceChanged(deviceId: Int) = Unit

    override fun onInputDeviceRemoved(deviceId: Int) {
        directions.remove(deviceId)
        devicePlayers.remove(deviceId)?.let(::releasePlayer)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = handleKey(keyCode, event, pressed = true) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = handleKey(keyCode, event, pressed = false) || super.onKeyUp(keyCode, event)

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_MOVE || !isExternalController(event.device)) {
            return super.onGenericMotionEvent(event)
        }
        val player = playerForDevice(event.deviceId) ?: return super.onGenericMotionEvent(event)
        val horizontal = strongestAxis(event, MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_X)
        val vertical = strongestAxis(event, MotionEvent.AXIS_HAT_Y, MotionEvent.AXIS_Y)
        val pressed =
            buildSet {
                if (horizontal < -AXIS_THRESHOLD) add(SNESButton.Left)
                if (horizontal > AXIS_THRESHOLD) add(SNESButton.Right)
                if (vertical < -AXIS_THRESHOLD) add(SNESButton.Up)
                if (vertical > AXIS_THRESHOLD) add(SNESButton.Down)
            }
        val previous = directions.put(event.deviceId, pressed).orEmpty()
        for (button in DIRECTION_BUTTONS) {
            if ((button in previous) != (button in pressed)) {
                engine?.setButton(button, button in pressed, player)
            }
        }
        return true
    }

    private fun handleKey(
        keyCode: Int,
        event: KeyEvent,
        pressed: Boolean,
    ): Boolean {
        if (!isExternalController(event.device)) return false
        val button = keyCode.toSNESButton() ?: return false
        val player = playerForDevice(event.deviceId) ?: return false
        engine?.setButton(button, pressed, player)
        return true
    }

    private fun playerForDevice(deviceId: Int): Int? {
        val compatibleDevices =
            inputManager.inputDeviceIds
                .filter { isExternalController(inputManager.getInputDevice(it)) }
                .sorted()
        val player = compatibleDevices.indexOf(deviceId).takeIf { it in 0..1 } ?: return null
        val previousPlayer = devicePlayers.put(deviceId, player)
        if (previousPlayer != null && previousPlayer != player) releasePlayer(previousPlayer)
        return player
    }

    private fun releasePlayer(player: Int) {
        SNESButton.entries.forEach { button -> engine?.setButton(button, false, player) }
    }

    private companion object {
        const val AXIS_THRESHOLD = 0.5f
        val DIRECTION_BUTTONS = setOf(SNESButton.Up, SNESButton.Down, SNESButton.Left, SNESButton.Right)

        fun isExternalController(device: InputDevice?): Boolean =
            device?.let { isExternalGameControllerSource(it.sources, it.isVirtual) } == true

        fun strongestAxis(
            event: MotionEvent,
            firstAxis: Int,
            secondAxis: Int,
        ): Float {
            val first = event.getAxisValue(firstAxis)
            val second = event.getAxisValue(secondAxis)
            return if (abs(first) >= abs(second)) first else second
        }

        fun Int.toSNESButton(): SNESButton? =
            when (this) {
                KeyEvent.KEYCODE_DPAD_UP -> SNESButton.Up
                KeyEvent.KEYCODE_DPAD_DOWN -> SNESButton.Down
                KeyEvent.KEYCODE_DPAD_LEFT -> SNESButton.Left
                KeyEvent.KEYCODE_DPAD_RIGHT -> SNESButton.Right
                KeyEvent.KEYCODE_BUTTON_A -> SNESButton.A
                KeyEvent.KEYCODE_BUTTON_B -> SNESButton.B
                KeyEvent.KEYCODE_BUTTON_X -> SNESButton.X
                KeyEvent.KEYCODE_BUTTON_Y -> SNESButton.Y
                KeyEvent.KEYCODE_BUTTON_L1 -> SNESButton.L
                KeyEvent.KEYCODE_BUTTON_R1 -> SNESButton.R
                KeyEvent.KEYCODE_BUTTON_START -> SNESButton.Start
                KeyEvent.KEYCODE_BUTTON_SELECT -> SNESButton.Select
                else -> null
            }
    }
}
