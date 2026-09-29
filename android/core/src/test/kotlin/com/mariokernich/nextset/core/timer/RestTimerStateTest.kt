package com.mariokernich.nextset.core.timer

import com.mariokernich.nextset.core.store.NextSetJson
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class RestTimerStateTest {
    private val t0 = 1_000_000L

    @Test
    fun startsRunningWithFullDuration() {
        val state = RestTimerState().started(90, now = t0)

        assertEquals(Phase.RUNNING, state.phase)
        assertEquals(90_000, state.remaining(t0))
        assertEquals(60_000, state.remaining(t0 + 30_000))
        assertEquals(0.5f, state.fractionRemaining(t0 + 45_000))
        assertEquals(90, state.requestedSeconds)
    }

    @Test
    fun displayRoundsUpSoTheLastSecondShowsOne() {
        val state = RestTimerState().started(10, now = t0)

        assertEquals(10, state.displayedSeconds(t0))
        assertEquals(10, state.displayedSeconds(t0 + 200))
        assertEquals(1, state.displayedSeconds(t0 + 9_500))
        assertEquals(0, state.displayedSeconds(t0 + 10_000))
        assertEquals(0, state.displayedSeconds(t0 + 60_000))
    }

    @Test
    fun updatesOnASecondBoundaryShowTheNewSecond() {
        val state = RestTimerState().started(60, now = t0)

        // Once-per-second updates may land a hair before or after the boundary.
        assertEquals(30, state.displayedSeconds(t0 + 30_000 - 1))
        assertEquals(30, state.displayedSeconds(t0 + 30_000 + 1))
        assertEquals(1, state.displayedSeconds(t0 + 59_900))
        assertEquals(0, state.displayedSeconds(t0 + 59_999))
    }

    @Test
    fun pauseFreezesTheRemainingTime() {
        val paused = RestTimerState().started(60, now = t0).paused(t0 + 20_000)

        assertEquals(Phase.PAUSED, paused.phase)
        assertEquals(40_000, paused.remaining(t0 + 500_000))

        val resumed = paused.resumed(t0 + 100_000)
        assertEquals(Phase.RUNNING, resumed.phase)
        assertEquals(30_000, resumed.remaining(t0 + 110_000))
        assertEquals(t0 + 140_000, resumed.endAt)
    }

    @Test
    fun addingTimeExtendsTheRest() {
        val state = RestTimerState().started(60, now = t0).adjusted(15_000, t0 + 10_000)

        assertEquals(Phase.RUNNING, state.phase)
        assertEquals(65_000, state.remaining(t0 + 10_000))
        assertEquals(75_000, state.durationMs)
    }

    @Test
    fun removingMoreTimeThanLeftFinishesTheRest() {
        val state = RestTimerState().started(60, now = t0).adjusted(-15_000, t0 + 50_000)

        assertEquals(Phase.FINISHED, state.phase)
        assertEquals(t0 + 50_000, state.finishedAt)
    }

    @Test
    fun adjustingWhilePausedKeepsItPaused() {
        val state = RestTimerState().started(60, now = t0).paused(t0 + 30_000).adjusted(-15_000, t0 + 40_000)

        assertEquals(Phase.PAUSED, state.phase)
        assertEquals(15_000, state.remaining(t0 + 90_000))
    }

    @Test
    fun finishTracksOvertime() {
        val state = RestTimerState().started(30, now = t0).finished(t0 + 30_000)

        assertEquals(Phase.FINISHED, state.phase)
        assertFalse(state.isActive)
        assertEquals(12_000, state.overtime(t0 + 42_000))
        assertEquals(0f, state.fractionRemaining(t0 + 42_000))
    }

    @Test
    fun resetKeepsTheLastRequestForRepeat() {
        val state = RestTimerState().started(120, presetId = "squats", now = t0).reset()

        assertEquals(Phase.IDLE, state.phase)
        assertEquals(120, state.requestedSeconds)
        assertEquals("squats", state.presetId)
        assertNull(state.endAt)
    }

    @Test
    fun durationIsClampedToTheAllowedRange() {
        assertEquals(5_000, RestTimerState().started(0, now = t0).durationMs)
        assertEquals(3_600_000, RestTimerState().started(99_999, now = t0).durationMs)
    }

    @Test
    fun survivesEncoding() {
        val state = RestTimerState().started(75, presetId = "rows", now = t0).paused(t0 + 5_000)
        val decoded = NextSetJson.decodeFromString<RestTimerState>(NextSetJson.encodeToString(state))
        assertEquals(state, decoded)
    }
}
