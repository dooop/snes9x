package snes9x

import android.net.Uri

data class SNESConfiguration(
    val romUri: Uri,
    val automaticallyStarts: Boolean = true,
    val showsTouchControls: Boolean = true,
)

sealed interface SNESState {
    data object Idle : SNESState

    data object Loading : SNESState

    data object Running : SNESState

    data object Paused : SNESState

    data object Stopped : SNESState

    data class Failed(
        val message: String,
    ) : SNESState
}

enum class SNESButton(
    val mask: Int,
) {
    B(0x01),
    Y(0x02),
    Select(0x04),
    Start(0x08),
    Up(0x10),
    Down(0x20),
    Left(0x40),
    Right(0x80),
    A(0x100),
    X(0x200),
    L(0x400),
    R(0x800),
}
