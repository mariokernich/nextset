package com.mariokernich.nextset.core.timer

import com.mariokernich.nextset.core.model.RestPreset
import kotlin.math.ceil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The state of the rest timer as a plain value.
 *
 * Time is always derived from absolute points in time (epoch milliseconds), so
 * the timer stays correct while the app is in the background and can be
 * restored after a restart.
 */
@Serializable
data class RestTimerState(
    val phase: Phase = Phase.IDLE,
    /** Length of the current rest including ± adjustments. */
    val durationMs: Long = 0,
    /** Planned end while running. */
    val endAt: Long? = null,
    /** Remaining time while paused. */
    val pausedRemainingMs: Long? = null,
    /** When the rest ended. */
    val finishedAt: Long? = null,
    /** The preset that started this rest, if any. */
    val presetId: String? = null,
    /** The duration that was originally requested, used for "repeat". */
    val requestedSeconds: Int = 0,
) {
    @Serializable
    enum class Phase {
        @SerialName("idle") IDLE,
        @SerialName("running") RUNNING,
        @SerialName("paused") PAUSED,
        @SerialName("finished") FINISHED,
    }

    val isActive: Boolean get() = phase == Phase.RUNNING || phase == Phase.PAUSED

    // Transitions

    fun started(seconds: Int, presetId: String? = null, now: Long): RestTimerState {
        val clamped = RestPreset.clamp(seconds)
        return RestTimerState(
            phase = Phase.RUNNING,
            durationMs = clamped * 1000L,
            endAt = now + clamped * 1000L,
            presetId = presetId,
            requestedSeconds = clamped,
        )
    }

    fun paused(now: Long): RestTimerState {
        if (phase != Phase.RUNNING || endAt == null) return this
        return copy(phase = Phase.PAUSED, pausedRemainingMs = maxOf(0, endAt - now), endAt = null)
    }

    fun resumed(now: Long): RestTimerState {
        if (phase != Phase.PAUSED || pausedRemainingMs == null) return this
        return copy(phase = Phase.RUNNING, endAt = now + pausedRemainingMs, pausedRemainingMs = null)
    }

    /** Adds or removes time. Taking away everything that's left finishes the rest. */
    fun adjusted(deltaMs: Long, now: Long): RestTimerState {
        if (!isActive) return this
        val newRemaining = remaining(now) + deltaMs
        if (newRemaining <= 500) return finished(now)
        val newDuration = maxOf(durationMs + deltaMs, newRemaining)
        return if (phase == Phase.RUNNING) {
            copy(durationMs = newDuration, endAt = now + newRemaining)
        } else {
            copy(durationMs = newDuration, pausedRemainingMs = newRemaining)
        }
    }

    fun finished(at: Long): RestTimerState {
        if (!isActive) return this
        return copy(phase = Phase.FINISHED, finishedAt = at, endAt = null, pausedRemainingMs = null)
    }

    /** Back to idle, keeping the last request for "repeat". */
    fun reset(): RestTimerState = RestTimerState(presetId = presetId, requestedSeconds = requestedSeconds)

    // Derived values

    fun remaining(now: Long): Long = when (phase) {
        Phase.RUNNING -> maxOf(0, (endAt ?: now) - now)
        Phase.PAUSED -> pausedRemainingMs ?: 0
        Phase.IDLE, Phase.FINISHED -> 0
    }

    /**
     * Whole seconds as shown on a countdown: 0:01 during the last second.
     *
     * A few milliseconds of tolerance keep once-per-second updates that land
     * exactly on a second boundary from showing the previous second.
     */
    fun displayedSeconds(now: Long): Int = maxOf(0, ceil((remaining(now) - 5) / 1000.0).toInt())

    /** 1 at the start of a rest, 0 at its end. */
    fun fractionRemaining(now: Long): Float = when (phase) {
        Phase.IDLE -> 1f
        Phase.FINISHED -> 0f
        Phase.RUNNING, Phase.PAUSED ->
            if (durationMs > 0) (remaining(now).toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    }

    /** Time since the rest ended ("overtime"). */
    fun overtime(now: Long): Long {
        if (phase != Phase.FINISHED || finishedAt == null) return 0
        return maxOf(0, now - finishedAt)
    }
}
