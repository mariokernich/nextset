package com.mariokernich.nextset.core.timer

/** Something the user should feel or hear while resting. */
sealed interface TimerEvent {
    /** Epoch milliseconds at which the event is due. */
    val at: Long

    /** One of the final countdown seconds; [secondsLeft] is 3, 2, 1 … */
    data class Tick(val secondsLeft: Int, override val at: Long) : TimerEvent

    data class Finish(override val at: Long) : TimerEvent
}

object CountdownPlan {
    /**
     * Events still ahead for a rest that ends at [endAt], in order.
     *
     * Ticks fire when N, N-1 … 1 seconds are left; ticks that are due right
     * now (e.g. when a 5 s rest starts with a 5 s countdown) are skipped.
     */
    fun events(endAt: Long, countdownSeconds: Int, now: Long): List<TimerEvent> {
        val remaining = endAt - now
        val events = mutableListOf<TimerEvent>()
        for (secondsLeft in countdownSeconds downTo 1) {
            if (secondsLeft * 1000L < remaining - 50) {
                events += TimerEvent.Tick(secondsLeft, endAt - secondsLeft * 1000L)
            }
        }
        events += TimerEvent.Finish(endAt)
        return events
    }
}
