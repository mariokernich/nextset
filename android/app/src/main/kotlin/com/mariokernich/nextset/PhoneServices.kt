package com.mariokernich.nextset

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
 * Phone-specific reactions to the timer: the live notification with the
 * countdown and its [RestService], the end-of-rest alarm and the "rest is
 * over" notification. Keeping the display on is up to the activity.
 */
class PhoneServices(private val context: Context, private val store: TimerStore) : TimerSideEffects {
    private val liveNotification = LiveNotification(context)
    private val handler = Handler(Looper.getMainLooper())
    private val stopAfterFinish = Runnable {
        if (store.timer.phase != Phase.RUNNING) RestService.stop(context)
    }

    private val isInForeground: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    init {
        FinishedNotification.createChannel(context)
        liveNotification.createChannel()
    }

    override fun timerStateDidChange(state: RestTimerState, settings: FeedbackSettings) {
        when (state.phase) {
            // Also without the notification: the alarm ends the rest on time (sound,
            // vibration, live notification) when the phone sleeps in the meantime.
            Phase.RUNNING -> {
                handler.removeCallbacks(stopAfterFinish)
                state.endAt?.let { RestAlarm.schedule(context, it) } ?: RestAlarm.cancel(context)
            }
            Phase.IDLE, Phase.PAUSED -> RestAlarm.cancel(context)
            // Leave a pending alarm alone: it may be the only signal while the app is in the background.
            Phase.FINISHED -> Unit
        }
        // Out of date once the next rest starts or the finished one is reset.
        if (state.phase != Phase.FINISHED) FinishedNotification.cancel(context)
        liveNotification.update(state, enabled = settings.liveNotificationEnabled)
        when {
            state.phase == Phase.RUNNING && settings.liveNotificationEnabled -> RestService.start(context)
            state.phase != Phase.FINISHED -> RestService.stop(context)
        }
    }

    override fun timerDidFinish(state: RestTimerState, settings: FeedbackSettings, inTime: Boolean) {
        RestAlarm.cancel(context)
        if (RestService.isForeground) {
            // The service stays for the end sound, then takes its notification along.
            liveNotification.showFinished()
            handler.removeCallbacks(stopAfterFinish)
            handler.postDelayed(stopAfterFinish, 3_000)
        } else {
            liveNotification.cancel()
        }
        if (isInForeground) {
            // The app just played the end itself.
            FinishedNotification.cancel(context)
        } else if (settings.notificationsEnabled) {
            FinishedNotification.post(context)
        }
    }

    fun appDidBecomeActive() {
        FinishedNotification.cancel(context)
    }
}
