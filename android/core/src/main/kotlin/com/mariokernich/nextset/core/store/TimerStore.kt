package com.mariokernich.nextset.core.store

import com.mariokernich.nextset.core.feedback.Feedback
import com.mariokernich.nextset.core.model.FeedbackSettings
import com.mariokernich.nextset.core.model.RestPreset
import com.mariokernich.nextset.core.model.TimerLibrary
import com.mariokernich.nextset.core.timer.CountdownPlan
import com.mariokernich.nextset.core.timer.RestTimerState
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import com.mariokernich.nextset.core.timer.TimerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Platform services that react to the timer (notifications, the end-of-rest
 * alarm, the watch's foreground service …).
 */
interface TimerSideEffects {
    /** Called after every change of the timer or the settings. */
    fun timerStateDidChange(state: RestTimerState, settings: FeedbackSettings)

    /**
     * Called when a rest reaches its end. [inTime] is `false` when the app only
     * noticed the end later, e.g. after being in the background.
     */
    fun timerDidFinish(state: RestTimerState, settings: FeedbackSettings, inTime: Boolean)
}

/** Everything the screens show, as one immutable snapshot. */
data class TimerSnapshot(
    val library: TimerLibrary,
    val settings: FeedbackSettings,
    val timer: RestTimerState,
    /** Duration shown while no rest is running; adjustable with ±15 s. */
    val idleSeconds: Int,
    /**
     * Seconds left while the final countdown runs, otherwise `null`. Driven by
     * the same events as the countdown haptics and sounds.
     */
    val countdownSecondsLeft: Int?,
)

/**
 * The app model shared by the phone and the watch app.
 *
 * Call it from the main thread only; [scope] should run on the main thread too.
 */
class TimerStore(
    private val storage: Storage,
    val feedback: Feedback,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val snapshot: MutableStateFlow<TimerSnapshot>
    val state: StateFlow<TimerSnapshot>

    /** Set by the platform services of the phone or the watch. */
    var sideEffects: TimerSideEffects? = null

    /** Set by the sync to forward local edits to the other device. */
    var onLocalLibraryChange: ((TimerLibrary) -> Unit)? = null

    private var eventJob: Job? = null
    private var autoResetJob: Job? = null

    init {
        val library = storage.load<TimerLibrary>(Storage.Key.LIBRARY)?.normalized()
            // Stored right away, so the default timers keep their IDs across launches
            // (a running rest remembers which quick timer started it).
            ?: TimerLibrary.standard().also { storage.save(Storage.Key.LIBRARY, it) }
        val timer = storage.load<RestTimerState>(Storage.Key.TIMER) ?: RestTimerState()
        val settings = storage.load<FeedbackSettings>(Storage.Key.SETTINGS) ?: FeedbackSettings()
        snapshot = MutableStateFlow(
            TimerSnapshot(
                library = library,
                settings = settings,
                timer = timer,
                idleSeconds = if (timer.requestedSeconds > 0) timer.requestedSeconds else library.visibleQuickTimers[0].seconds,
                countdownSecondsLeft = null,
            ),
        )
        state = snapshot.asStateFlow()
    }

    val library: TimerLibrary get() = snapshot.value.library
    val settings: FeedbackSettings get() = snapshot.value.settings
    val timer: RestTimerState get() = snapshot.value.timer

    // Timer

    fun start(preset: RestPreset) = start(preset.seconds, preset.id)

    fun start(seconds: Int, presetId: String? = null) {
        val started = timer.started(seconds, presetId, clock())
        snapshot.update { it.copy(timer = started, idleSeconds = started.requestedSeconds) }
        feedback.timerStarted(settings)
        timerChanged()
    }

    /** The main button: start, pause or resume. */
    fun primaryAction() {
        when (timer.phase) {
            Phase.IDLE, Phase.FINISHED -> {
                val seconds = snapshot.value.idleSeconds
                start(seconds, library.allTimers.firstOrNull { it.seconds == seconds }?.id)
            }
            Phase.RUNNING -> {
                snapshot.update { it.copy(timer = it.timer.paused(clock())) }
                timerChanged()
            }
            Phase.PAUSED -> {
                snapshot.update { it.copy(timer = it.timer.resumed(clock())) }
                timerChanged()
            }
        }
    }

    /** Adds or removes time. While idle this changes the duration to start. */
    fun adjust(seconds: Int) {
        if (timer.isActive) {
            val adjusted = timer.adjusted(seconds * 1000L, clock())
            snapshot.update { it.copy(timer = adjusted) }
            if (adjusted.phase == Phase.FINISHED) didFinish(inTime = true) else timerChanged()
        } else {
            snapshot.update { it.copy(idleSeconds = RestPreset.clamp(it.idleSeconds + seconds)) }
        }
    }

    /** Ends the rest early, e.g. when ready for the next set. */
    fun stop() {
        if (timer.phase == Phase.IDLE) return
        snapshot.update { it.copy(timer = it.timer.reset()) }
        timerChanged()
    }

    /** Re-checks the timer after the app was in the background, restarted or woken by the alarm. */
    fun refresh(now: Long = clock()) {
        val current = timer
        when (current.phase) {
            Phase.RUNNING -> {
                val end = current.endAt
                if (end != null && end <= now) {
                    snapshot.update { it.copy(timer = current.finished(end)) }
                    didFinish(inTime = now - end < LATE_FINISH_MS)
                    return
                }
            }
            Phase.FINISHED -> {
                if (current.overtime(now) > OVERTIME_LIMIT_MS) {
                    snapshot.update { it.copy(timer = current.reset()) }
                    save()
                } else {
                    scheduleAutoReset()
                }
            }
            Phase.IDLE, Phase.PAUSED -> Unit
        }
        updateCountdown(now)
        scheduleEvents()
        sideEffects?.timerStateDidChange(timer, settings)
    }

    // Library & settings

    fun updateLibrary(change: (TimerLibrary) -> TimerLibrary) {
        val current = library
        val changed = change(current).normalized()
        if (changed == current) return
        val stamped = changed.copy(modifiedAt = clock())
        snapshot.update { it.copy(library = stamped) }
        storage.save(Storage.Key.LIBRARY, stamped)
        onLocalLibraryChange?.invoke(stamped)
    }

    /** Applies timers edited on the other device if they are newer. */
    fun applyRemoteLibrary(remote: TimerLibrary) {
        if (remote.modifiedAt <= library.modifiedAt) return
        val normalized = remote.normalized()
        snapshot.update { it.copy(library = normalized) }
        storage.save(Storage.Key.LIBRARY, normalized)
    }

    fun updateSettings(change: (FeedbackSettings) -> FeedbackSettings) {
        val current = settings
        val changed = change(current)
        if (changed == current) return
        snapshot.update { it.copy(settings = changed) }
        storage.save(Storage.Key.SETTINGS, changed)
        if (changed.countdownSeconds != current.countdownSeconds) {
            updateCountdown()
            scheduleEvents()
        }
        sideEffects?.timerStateDidChange(timer, changed)
    }

    // Internals

    private fun timerChanged() {
        autoResetJob?.cancel()
        updateCountdown()
        save()
        scheduleEvents()
        sideEffects?.timerStateDidChange(timer, settings)
    }

    private fun updateCountdown(now: Long = clock()) {
        val seconds = timer.displayedSeconds(now)
        val inCountdown = timer.phase == Phase.RUNNING && seconds > 0 && seconds <= settings.countdownSeconds
        snapshot.update { it.copy(countdownSecondsLeft = if (inCountdown) seconds else null) }
    }

    private fun didFinish(inTime: Boolean) {
        eventJob?.cancel()
        snapshot.update { it.copy(countdownSecondsLeft = null) }
        if (inTime) feedback.finished(settings)
        save()
        sideEffects?.timerDidFinish(timer, settings, inTime)
        scheduleAutoReset()
    }

    private fun save() {
        storage.save(Storage.Key.TIMER, timer)
    }

    /** Plays the countdown and ends the rest exactly on time while the app runs. */
    private fun scheduleEvents() {
        eventJob?.cancel()
        val end = timer.endAt
        if (timer.phase != Phase.RUNNING || end == null) return
        val events = CountdownPlan.events(end, settings.countdownSeconds, clock())
        eventJob = scope.launch {
            for (event in events) {
                val wait = event.at - clock()
                if (wait > 0) delay(wait)
                handle(event)
            }
        }
    }

    private fun handle(event: TimerEvent) {
        // Negative while early, positive when the app was frozen and wakes late.
        val lateness = clock() - event.at
        when (event) {
            is TimerEvent.Tick -> {
                snapshot.update { it.copy(countdownSecondsLeft = event.secondsLeft) }
                if (lateness < 500) feedback.countdownTick(event.secondsLeft, settings)
            }
            is TimerEvent.Finish -> {
                if (timer.phase != Phase.RUNNING) return
                snapshot.update { it.copy(timer = it.timer.finished(event.at)) }
                didFinish(inTime = lateness < LATE_FINISH_MS)
            }
        }
    }

    private fun scheduleAutoReset() {
        autoResetJob?.cancel()
        if (timer.phase != Phase.FINISHED) return
        val wait = maxOf(0, OVERTIME_LIMIT_MS - timer.overtime(clock()))
        autoResetJob = scope.launch {
            delay(wait)
            if (timer.phase == Phase.FINISHED) {
                snapshot.update { it.copy(timer = it.timer.reset()) }
                timerChanged()
            }
        }
    }

    companion object {
        /** A finished rest falls back to idle after this long. */
        const val OVERTIME_LIMIT_MS = 15 * 60 * 1000L
        const val ADJUST_STEP = 15

        /** An end noticed later than this is not played anymore. */
        private const val LATE_FINISH_MS = 3000L
    }
}
