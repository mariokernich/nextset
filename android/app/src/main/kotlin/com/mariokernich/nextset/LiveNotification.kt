package com.mariokernich.nextset

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.notify.FinishedNotification
import com.mariokernich.nextset.core.timer.RestTimerState
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import java.util.Date
import com.mariokernich.nextset.core.R as CoreR

/**
 * The running rest on the lock screen and in the status bar, the Android
 * counterpart of the Live Activity. The system counts down from the end time,
 * so the notification only changes when the user pauses or changes the time.
 * On Android 16+ it can appear as a Live Update chip in the status bar.
 *
 * While a rest runs, it is the notification of [RestService].
 */
class LiveNotification(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(CoreR.string.channel_running),
            // Not "low": the lock screen may hide low ("silent") notifications. It stays quiet anyway.
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(CoreR.string.channel_running_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // Checked by canPost.
    fun update(state: RestTimerState, enabled: Boolean) {
        if (!enabled || !state.isActive || !FinishedNotification.canPost(context)) {
            cancel()
            return
        }
        manager.notify(ID, notification(state))
    }

    /** For the few seconds [RestService] still runs after the end: no countdown below zero. */
    @SuppressLint("MissingPermission") // Checked by canPost.
    fun showFinished() {
        if (FinishedNotification.canPost(context)) manager.notify(ID, notification(null))
    }

    fun cancel() {
        manager.cancel(ID)
    }

    /** The notification for [state]; `null` or a rest that isn't active shows the end. */
    fun notification(state: RestTimerState?): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(CoreR.drawable.ic_stat_nextset)
            .setColor(ContextCompat.getColor(context, R.color.coral))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(FinishedNotification.openApp(context))

        val end = state?.endAt
        when {
            state?.phase == Phase.RUNNING && end != null -> builder
                .setContentTitle(context.getString(CoreR.string.caption_rest))
                .setContentText(context.getString(CoreR.string.next_set_at, DateFormat.getTimeFormat(context).format(Date(end))))
                .setWhen(end)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, context.getString(CoreR.string.pause), action(RestActionReceiver.ACTION_PAUSE))
                .addAction(0, context.getString(R.string.add_15_short), action(RestActionReceiver.ACTION_ADD))
                .addAction(0, context.getString(CoreR.string.end_rest), action(RestActionReceiver.ACTION_END))
            state?.phase == Phase.PAUSED -> {
                val remaining = DurationFormat.clock(state.displayedSeconds(System.currentTimeMillis()))
                val duration = DurationFormat.clock((state.durationMs / 1000).toInt())
                builder
                    .setContentTitle(context.getString(CoreR.string.caption_paused))
                    .setContentText(context.getString(R.string.paused_detail, remaining, duration))
                    .setShowWhen(false)
                    .addAction(0, context.getString(CoreR.string.resume), action(RestActionReceiver.ACTION_RESUME))
                    .addAction(0, context.getString(CoreR.string.end_rest), action(RestActionReceiver.ACTION_END))
            }
            else -> builder
                .setContentTitle(context.getString(CoreR.string.caption_next_set))
                .setContentText(context.getString(CoreR.string.time_for_next_set))
                .setShowWhen(false)
        }
        return builder.build()
    }

    private fun action(name: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        name.hashCode(),
        Intent(context, RestActionReceiver::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val CHANNEL = "rest_running"
        const val ID = 1
    }
}
