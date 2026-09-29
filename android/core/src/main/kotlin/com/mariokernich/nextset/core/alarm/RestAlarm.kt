package com.mariokernich.nextset.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.mariokernich.nextset.core.NextSetHost

/**
 * Wakes the app exactly when a rest ends, so it can play the end and post the
 * notification even while it's in the background or was stopped. While the app
 * is on screen, the timer store gets there first and the alarm changes nothing.
 */
object RestAlarm {
    fun schedule(context: Context, at: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val operation = operation(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        } else {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(operation(context))
    }

    private fun operation(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, RestAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val host = context.applicationContext as? NextSetHost ?: return
        // Keep the process alive while the end sound and vibration play.
        val pending = goAsync()
        host.store.refresh()
        Handler(Looper.getMainLooper()).postDelayed({ pending.finish() }, 3_000)
    }
}

/**
 * A restart of the device clears the alarm and the notifications. Afterwards a
 * running rest gets them back, and one that ended in the meantime is reported.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        (context.applicationContext as? NextSetHost)?.store?.refresh()
    }
}
