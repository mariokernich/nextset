package com.mariokernich.nextset

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.model.RestPreset
import com.mariokernich.nextset.core.store.TimerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.mariokernich.nextset.core.R as CoreR

/**
 * Long-press shortcuts on the app icon: the quick timers and "End rest". The
 * Android counterpart of the Siri shortcuts; they can also be put on the home
 * screen or triggered from other apps.
 */
object Shortcuts {
    const val ACTION_START_QUICK = "com.mariokernich.nextset.action.START_QUICK_TIMER"
    const val ACTION_END_REST = "com.mariokernich.nextset.action.END_REST"
    const val EXTRA_SLOT = "slot"
    const val END_REST_ID = "end-rest"

    fun quickTimerId(slot: Int) = "quick-$slot"

    /** Lets the launcher learn which shortcuts are used, for its suggestions. */
    fun reportUsed(context: Context, id: String) {
        ShortcutManagerCompat.reportShortcutUsed(context, id)
    }

    /**
     * Keeps the shortcut labels in line with the quick timers, while the app is
     * in the foreground: in the background (alarm, notification buttons, sync)
     * Android limits how often shortcuts may change.
     */
    fun follow(context: Context, store: TimerStore, scope: CoroutineScope) {
        val foreground = ProcessLifecycleOwner.get().lifecycle
        scope.launch {
            combine(
                store.state.map { it.library.visibleQuickTimers.map(RestPreset::seconds) },
                foreground.currentStateFlow.map { it.isAtLeast(Lifecycle.State.STARTED) },
                ::Pair,
            )
                .filter { (_, isInForeground) -> isInForeground }
                .map { (seconds, _) -> seconds }
                .distinctUntilChanged()
                .collect { update(context, it) }
        }
    }

    private fun update(context: Context, quickTimerSeconds: List<Int>) {
        val start = quickTimerSeconds.mapIndexed { index, seconds ->
            val clock = DurationFormat.clock(seconds)
            ShortcutInfoCompat.Builder(context, quickTimerId(index))
                .setShortLabel(context.getString(R.string.shortcut_start, clock))
                .setLongLabel(context.getString(CoreR.string.start_rest_for, clock))
                .setIcon(IconCompat.createWithResource(context, CoreR.mipmap.ic_launcher))
                .setIntent(Intent(context, MainActivity::class.java).setAction(ACTION_START_QUICK).putExtra(EXTRA_SLOT, index))
                .build()
        }
        val end = ShortcutInfoCompat.Builder(context, END_REST_ID)
            .setShortLabel(context.getString(CoreR.string.end_rest))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_shortcut_end))
            .setIntent(Intent(context, MainActivity::class.java).setAction(ACTION_END_REST))
            .build()
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, start + end) }
    }
}
