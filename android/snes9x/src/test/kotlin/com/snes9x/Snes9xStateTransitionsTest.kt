// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

import org.junit.Assert.assertEquals
import org.junit.Test

class Snes9xStateTransitionsTest {
    @Test
    fun resumeBeforeStartKeepsEngineIdle() {
        assertEquals(Snes9xState.Idle, Snes9xState.Idle.afterResumeRequest())
    }

    @Test
    fun lifecycleEventsDoNotInterruptLoading() {
        assertEquals(Snes9xState.Loading, Snes9xState.Loading.afterPauseRequest())
        assertEquals(Snes9xState.Loading, Snes9xState.Loading.afterResumeRequest())
    }

    @Test
    fun runningEngineCanPauseAndResume() {
        val paused = Snes9xState.Running.afterPauseRequest()

        assertEquals(Snes9xState.Paused, paused)
        assertEquals(Snes9xState.Running, paused.afterResumeRequest())
    }

    @Test
    fun terminalStatesIgnoreLifecycleEvents() {
        val failed = Snes9xState.Failed("failure")

        assertEquals(Snes9xState.Stopped, Snes9xState.Stopped.afterResumeRequest())
        assertEquals(failed, failed.afterPauseRequest())
        assertEquals(failed, failed.afterResumeRequest())
    }
}
