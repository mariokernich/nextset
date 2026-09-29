package com.mariokernich.nextset.core.store

import com.mariokernich.nextset.core.MemoryStorage
import com.mariokernich.nextset.core.SilentFeedback
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.model.TimerLibrary
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher

class TimerStoreTest {
    private var now = 1_000_000L

    // The scheduled countdown never runs here: the tests check what happens right away.
    private fun makeStore(storage: Storage = MemoryStorage()): TimerStore =
        TimerStore(storage, SilentFeedback, CoroutineScope(StandardTestDispatcher()), clock = { now })

    @Test
    fun startsQuickTimer() {
        val store = makeStore()
        val quick = store.library.visibleQuickTimers[0]
        store.start(quick)

        assertEquals(Phase.RUNNING, store.timer.phase)
        assertEquals(quick.id, store.timer.presetId)
        assertEquals(quick.seconds, store.state.value.idleSeconds)
    }

    @Test
    fun primaryActionCyclesThroughStartPauseResume() {
        val store = makeStore()
        store.primaryAction()
        assertEquals(Phase.RUNNING, store.timer.phase)
        store.primaryAction()
        assertEquals(Phase.PAUSED, store.timer.phase)
        store.primaryAction()
        assertEquals(Phase.RUNNING, store.timer.phase)
    }

    @Test
    fun adjustingWhileIdleChangesTheNextDuration() {
        val store = makeStore()
        val before = store.state.value.idleSeconds
        store.adjust(15)
        assertEquals(before + 15, store.state.value.idleSeconds)
        assertEquals(Phase.IDLE, store.timer.phase)

        store.primaryAction()
        assertEquals(before + 15, store.timer.requestedSeconds)
    }

    @Test
    fun removingAllTimeEndsTheRest() {
        val store = makeStore()
        store.start(10)
        store.adjust(-15)
        assertEquals(Phase.FINISHED, store.timer.phase)
    }

    @Test
    fun reportsWhenTheFinalCountdownRuns() {
        val store = makeStore()
        store.updateSettings { it.copy(countdownSeconds = 10) }

        store.start(90)
        assertNull(store.state.value.countdownSecondsLeft)

        store.start(8)
        assertEquals(8, store.state.value.countdownSecondsLeft)

        store.adjust(15)
        assertNull(store.state.value.countdownSecondsLeft)

        store.stop()
        assertNull(store.state.value.countdownSecondsLeft)
    }

    @Test
    fun stopReturnsToIdle() {
        val store = makeStore()
        store.start(60)
        store.stop()
        assertEquals(Phase.IDLE, store.timer.phase)
    }

    @Test
    fun refreshFinishesAnOverdueRest() {
        val store = makeStore()
        store.start(30)
        store.refresh(now + 45_000)
        assertEquals(Phase.FINISHED, store.timer.phase)
    }

    @Test
    fun refreshResetsARestThatEndedLongAgo() {
        val store = makeStore()
        store.start(30)
        store.refresh(now + 30_000)
        store.refresh(now + 30_000 + TimerStore.OVERTIME_LIMIT_MS + 1)
        assertEquals(Phase.IDLE, store.timer.phase)
    }

    @Test
    fun stateSurvivesRelaunch() {
        val storage = MemoryStorage()
        val first = makeStore(storage)
        first.updateSettings { it.copy(countdownSeconds = 10) }
        first.updateLibrary { it.copy(quickTimerCount = 1) }
        first.start(120)

        val second = makeStore(storage)
        assertEquals(Phase.RUNNING, second.timer.phase)
        assertEquals(10, second.settings.countdownSeconds)
        assertEquals(1, second.library.visibleQuickTimers.size)
    }

    @Test
    fun defaultTimersKeepTheirIdsAcrossLaunches() {
        val storage = MemoryStorage()
        val first = makeStore(storage)
        first.start(first.library.visibleQuickTimers[1])

        val second = makeStore(storage)
        assertEquals(second.library.visibleQuickTimers[1].id, second.timer.presetId)
        assertEquals(0, second.library.modifiedAt)
    }

    @Test
    fun localEditsAreForwardedForSync() {
        val store = makeStore()
        var forwarded: TimerLibrary? = null
        store.onLocalLibraryChange = { forwarded = it }
        store.updateLibrary { it.saving(75, "Rows", TimerEditorTarget.NewPreset) }

        assertTrue(forwarded?.presets?.any { it.seconds == 75 && it.name == "Rows" } == true)
        assertTrue(store.library.modifiedAt > 0)
    }

    @Test
    fun onlyNewerRemoteLibrariesAreApplied() {
        val store = makeStore()
        store.updateLibrary { it.copy(quickTimerCount = 1) }

        store.applyRemoteLibrary(TimerLibrary.standard().copy(modifiedAt = 1))
        assertEquals(1, store.library.quickTimerCount)

        store.applyRemoteLibrary(TimerLibrary.standard().copy(modifiedAt = now + 60_000))
        assertEquals(2, store.library.quickTimerCount)
    }
}
