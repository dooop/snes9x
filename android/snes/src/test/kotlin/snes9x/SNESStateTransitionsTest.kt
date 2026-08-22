// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package snes9x

import org.junit.Assert.assertEquals
import org.junit.Test

class SNESStateTransitionsTest {
    @Test
    fun resumeBeforeStartKeepsEngineIdle() {
        assertEquals(SNESState.Idle, SNESState.Idle.afterResumeRequest())
    }

    @Test
    fun lifecycleEventsDoNotInterruptLoading() {
        assertEquals(SNESState.Loading, SNESState.Loading.afterPauseRequest())
        assertEquals(SNESState.Loading, SNESState.Loading.afterResumeRequest())
    }

    @Test
    fun runningEngineCanPauseAndResume() {
        val paused = SNESState.Running.afterPauseRequest()

        assertEquals(SNESState.Paused, paused)
        assertEquals(SNESState.Running, paused.afterResumeRequest())
    }

    @Test
    fun terminalStatesIgnoreLifecycleEvents() {
        val failed = SNESState.Failed("failure")

        assertEquals(SNESState.Stopped, SNESState.Stopped.afterResumeRequest())
        assertEquals(failed, failed.afterPauseRequest())
        assertEquals(failed, failed.afterResumeRequest())
    }
}
