package com.mariokernich.nextset.core

import com.mariokernich.nextset.core.store.TimerStore

/**
 * Puts the app into a given state for screenshots. Debug builds read it from
 * the launch intent, e.g. `adb shell am start -n … --es demo running`.
 */
object DemoMode {
    const val EXTRA = "demo"

    /** Epoch seconds at which a `countdown` rest should end. */
    const val EXTRA_END = "demoEnd"

    fun apply(store: TimerStore, value: String?, endEpochSeconds: Long) {
        value ?: return
        // Avoid the notification permission prompt in screenshots.
        store.updateSettings { it.copy(notificationsEnabled = false, liveNotificationEnabled = false) }
        when (value) {
            "running" -> store.start(store.library.visibleQuickTimers[0])
            "countdown" -> store.start(
                if (endEpochSeconds > 0) (endEpochSeconds - System.currentTimeMillis() / 1000).toInt() else 7,
            )
            "paused" -> {
                store.start(150)
                store.primaryAction()
            }
            "finished" -> {
                store.start(30)
                store.adjust(-30)
            }
            // Keep whatever the last launch left behind.
            "keep" -> Unit
            else -> store.stop()
        }
    }
}
