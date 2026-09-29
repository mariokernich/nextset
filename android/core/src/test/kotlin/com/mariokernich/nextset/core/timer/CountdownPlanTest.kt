package com.mariokernich.nextset.core.timer

import kotlin.test.Test
import kotlin.test.assertEquals

class CountdownPlanTest {
    private val end = 2_000_000L

    @Test
    fun ticksForTheLastSecondsThenFinish() {
        val events = CountdownPlan.events(end, countdownSeconds = 3, now = end - 60_000)

        assertEquals(
            listOf(
                TimerEvent.Tick(3, end - 3_000),
                TimerEvent.Tick(2, end - 2_000),
                TimerEvent.Tick(1, end - 1_000),
                TimerEvent.Finish(end),
            ),
            events,
        )
    }

    @Test
    fun skipsTicksThatArePastOrDueNow() {
        val events = CountdownPlan.events(end, countdownSeconds = 5, now = end - 2_500)
        assertEquals(listOf(end - 2_000, end - 1_000, end), events.map { it.at })

        val startingAtCountdown = CountdownPlan.events(end, countdownSeconds = 5, now = end - 5_000)
        assertEquals(TimerEvent.Tick(4, end - 4_000), startingAtCountdown.first())
    }

    @Test
    fun countdownCanBeTurnedOff() {
        assertEquals(listOf(TimerEvent.Finish(end)), CountdownPlan.events(end, countdownSeconds = 0, now = end - 60_000))
    }

    @Test
    fun finishIsStillReportedWhenAlreadyOverdue() {
        assertEquals(listOf(TimerEvent.Finish(end)), CountdownPlan.events(end, countdownSeconds = 3, now = end + 10_000))
    }

    @Test
    fun eventsAreInChronologicalOrder() {
        val events = CountdownPlan.events(end, countdownSeconds = 10, now = end - 30_000)
        assertEquals(11, events.size)
        assertEquals(events.map { it.at }.sorted(), events.map { it.at })
    }
}
