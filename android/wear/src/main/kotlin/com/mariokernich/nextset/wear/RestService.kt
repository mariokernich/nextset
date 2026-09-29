package com.mariokernich.nextset.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.mariokernich.nextset.core.NextSetHost
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import java.util.Date
import com.mariokernich.nextset.core.R as CoreR

/**
 * Runs while a rest counts down: keeps the app and the CPU awake for the
 * countdown vibrations and shows the rest as an ongoing activity on the watch
 * face, from where a tap leads back to the timer.
 */
class RestService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timer = (application as NextSetHost).store.timer
        val end = timer.endAt
        // A started foreground service has to show its notification in any case.
        // Watches before Android 14 don't know the "special use" type yet.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(end), type)
        isRunning = true
        if (timer.phase != Phase.RUNNING || end == null) {
            stopSelf()
        } else {
            holdWakeLock(until = end)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        isRunning = false
        super.onDestroy()
    }

    private fun holdWakeLock(until: Long) {
        releaseWakeLock()
        val lock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NextSet:rest")
        // Until a few seconds after the end, for the final vibration.
        lock.acquire(maxOf(0, until - System.currentTimeMillis()) + 6_000)
        wakeLock = lock
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun notification(end: Long?): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(CoreR.drawable.ic_stat_nextset)
            .setContentTitle(getString(CoreR.string.caption_rest))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
        if (end != null) {
            builder
                .setContentText(getString(CoreR.string.next_set_at, DateFormat.getTimeFormat(this).format(Date(end))))
                .setWhen(end)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
            // The ongoing activity counts down on the watch face (elapsed-realtime clock).
            val endOnElapsedClock = SystemClock.elapsedRealtime() + (end - System.currentTimeMillis())
            OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
                .setStaticIcon(CoreR.drawable.ic_stat_nextset)
                .setTouchIntent(open)
                .setStatus(Status.Builder().addTemplate("#time#").addPart("time", Status.TimerPart(endOnElapsedClock)).build())
                .build()
                .apply(applicationContext)
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL = "rest_running"
        private const val NOTIFICATION_ID = 1

        /** Whether the service is in the foreground and keeps the app running. */
        var isRunning = false
            private set

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL,
                context.getString(CoreR.string.channel_running),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(CoreR.string.channel_running_description)
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        /** Starts the service or updates it after the end of the rest changed. */
        fun start(context: Context) {
            // Can fail when the app isn't allowed to start it from the background.
            runCatching { ContextCompat.startForegroundService(context, Intent(context, RestService::class.java)) }
        }

        /**
         * Stopping the service before it called `startForeground` would crash the app.
         * Until then it stops itself as soon as it sees that the rest isn't running.
         */
        fun stop(context: Context) {
            if (isRunning) context.stopService(Intent(context, RestService::class.java))
        }
    }
}
