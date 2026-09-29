package com.mariokernich.nextset

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mariokernich.nextset.core.NextSetHost
import com.mariokernich.nextset.core.timer.RestTimerState.Phase

/**
 * Runs while a rest counts down, so NextSet can still be heard when it isn't
 * on screen: Android 17 only lets visible apps and foreground services play
 * sounds, and since Android 15 only they may lower the music of other apps.
 * Its notification is the live notification, so it only runs while that is on.
 */
class RestService : Service() {
    private val store get() = (application as NextSetHost).store

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            leave()
            return START_NOT_STICKY
        }
        val timer = store.timer
        // A started foreground service has to show its notification in any case.
        // Phones before Android 14 don't know the "special use" type yet.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, LiveNotification.ID, LiveNotification(this).notification(timer), type)
        isForeground = true
        if (timer.phase != Phase.RUNNING) leave()
        return START_NOT_STICKY
    }

    /**
     * Leaves the foreground and ends. This has to happen here: stopping the
     * service from outside would also remove the notification of a paused rest.
     */
    private fun leave() {
        // A paused rest keeps its notification, with Resume and End.
        val keepsNotification = store.timer.phase == Phase.PAUSED && store.settings.liveNotificationEnabled
        ServiceCompat.stopForeground(this, if (keepsNotification) ServiceCompat.STOP_FOREGROUND_DETACH else ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    override fun onDestroy() {
        isForeground = false
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.mariokernich.nextset.action.STOP_REST_SERVICE"

        /** Whether the service shows its notification and keeps the app audible. */
        var isForeground = false
            private set

        /** Starts the service or updates its notification after the rest changed. */
        fun start(context: Context) {
            // Can fail when the app isn't allowed to start it from the background.
            runCatching { ContextCompat.startForegroundService(context, Intent(context, RestService::class.java)) }
        }

        /**
         * Asks the service to end. Until it called `startForeground` it must not
         * be stopped (that would crash the app); it then ends by itself as soon
         * as it sees that the rest isn't running.
         */
        fun stop(context: Context) {
            if (!isForeground) return
            // Allowed from the background: the foreground service counts as foreground.
            runCatching { context.startService(Intent(context, RestService::class.java).setAction(ACTION_STOP)) }
        }
    }
}
