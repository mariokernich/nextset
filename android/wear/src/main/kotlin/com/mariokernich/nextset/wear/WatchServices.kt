package com.mariokernich.nextset.wear

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.mariokernich.nextset.core.alarm.RestAlarm
import com.mariokernich.nextset.core.model.FeedbackSettings
import com.mariokernich.nextset.core.notify.FinishedNotification
import com.mariokernich.nextset.core.store.TimerSideEffects
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.timer.RestTimerState
import com.mariokernich.nextset.core.timer.RestTimerState.Phase

/**
 * Keeps the watch app running while a rest counts down, so the countdown
 * vibrations and the final alert reach the wrist even with the arm lowered.
 *
 * [RestService] stands in for the extended runtime session of the Apple Watch.
 * If it doesn't run, the alarm wakes the app for the end of the rest and the
 * notification takes over.
 */
class WatchServices(private val context: Context, private val store: TimerStore) : TimerSideEffects {
    private val handler = Handler(Looper.getMainLooper())
    private val stopAfterFinish = Runnable {
        if (store.timer.phase != Phase.RUNNING) RestService.stop(context)
    }

    private val isInForeground: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    init {
        FinishedNotification.createChannel(context)
        RestService.createChannel(context)
    }

    override fun timerStateDidChange(state: RestTimerState, settings: FeedbackSettings) {
        when (state.phase) {
            Phase.RUNNING -> {
                handler.removeCallbacks(stopAfterFinish)
                RestService.start(context)
                val end = state.endAt
                if (settings.notificationsEnabled && end != null) RestAlarm.schedule(context, end) else RestAlarm.cancel(context)
            }
            Phase.IDLE, Phase.PAUSED -> {
                RestService.stop(context)
                RestAlarm.cancel(context)
            }
            Phase.FINISHED -> Unit
        }
        if (state.isActive) FinishedNotification.cancel(context)
    }

    override fun timerDidFinish(state: RestTimerState, settings: FeedbackSettings, inTime: Boolean) {
        RestAlarm.cancel(context)
        if (inTime && (isInForeground || RestService.isRunning)) {
            // The alert was just played on the wrist.
            FinishedNotification.cancel(context)
        } else if (settings.notificationsEnabled) {
            FinishedNotification.post(context)
        }
        // Give the final vibration a moment before the service ends.
        handler.removeCallbacks(stopAfterFinish)
        handler.postDelayed(stopAfterFinish, 4_000)
    }

    fun appDidBecomeActive() {
        FinishedNotification.cancel(context)
    }
}
