package com.mariokernich.nextset

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mariokernich.nextset.core.NextSetHost
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.timer.RestTimerState.Phase

/** The buttons of the live notification. */
class RestActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = (context.applicationContext as NextSetHost).store
        // A rest whose end went unnoticed (e.g. a late alarm) ends first; then +15 s
        // or Pause no longer apply to it.
        store.refresh()
        when (intent.action) {
            ACTION_PAUSE -> if (store.timer.phase == Phase.RUNNING) store.primaryAction()
            ACTION_RESUME -> if (store.timer.phase == Phase.PAUSED) store.primaryAction()
            ACTION_ADD -> if (store.timer.isActive) store.adjust(TimerStore.ADJUST_STEP)
            ACTION_END -> store.stop()
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.mariokernich.nextset.action.PAUSE"
        const val ACTION_RESUME = "com.mariokernich.nextset.action.RESUME"
        const val ACTION_ADD = "com.mariokernich.nextset.action.ADD"
        const val ACTION_END = "com.mariokernich.nextset.action.END"
    }
}
