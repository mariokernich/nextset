package com.mariokernich.nextset

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.mariokernich.nextset.core.alarm.RestAlarm
import com.mariokernich.nextset.core.model.FeedbackSettings
import com.mariokernich.nextset.core.notify.FinishedNotification
import com.mariokernich.nextset.core.store.TimerSideEffects
import com.mariokernich.nextset.core.timer.RestTimerState
import com.mariokernich.nextset.core.timer.RestTimerState.Phase

/**
 * Phone-specific reactions to the timer: the live notification with the
 * countdown, the end-of-rest alarm and the "rest is over" notification.
 * Keeping the display on is up to the activity.
 */
class PhoneServices(private val context: Context) : TimerSideEffects {
    private val liveNotification = LiveNotification(context)

    private val isInForeground: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    init {
        FinishedNotification.createChannel(context)
        liveNotification.createChannel()
    }

    override fun timerStateDidChange(state: RestTimerState, settings: FeedbackSettings) {
        when (state.phase) {
            Phase.RUNNING -> {
                val end = state.endAt
                if (settings.notificationsEnabled && end != null) RestAlarm.schedule(context, end) else RestAlarm.cancel(context)
            }
            Phase.IDLE, Phase.PAUSED -> RestAlarm.cancel(context)
            // Leave a pending alarm alone: it may be the only signal while the app is in the background.
            Phase.FINISHED -> Unit
        }
        if (state.isActive) FinishedNotification.cancel(context)
        liveNotification.update(state, enabled = settings.liveNotificationEnabled)
    }

    override fun timerDidFinish(state: RestTimerState, settings: FeedbackSettings, inTime: Boolean) {
        RestAlarm.cancel(context)
        liveNotification.cancel()
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
