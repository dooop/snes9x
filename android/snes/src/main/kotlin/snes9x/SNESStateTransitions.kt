package snes9x

internal fun SNESState.afterPauseRequest(): SNESState =
    when (this) {
        SNESState.Running -> SNESState.Paused
        else -> this
    }

internal fun SNESState.afterResumeRequest(): SNESState =
    when (this) {
        SNESState.Paused -> SNESState.Running
        else -> this
    }
