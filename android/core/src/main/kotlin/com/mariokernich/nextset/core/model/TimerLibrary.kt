package com.mariokernich.nextset.core.model

import kotlinx.serialization.Serializable

/**
 * The user's timers: one or two quick timers plus a list of further presets.
 *
 * The library is synced between phone and watch; the newest [modifiedAt] wins.
 */
@Serializable
data class TimerLibrary(
    /** Always holds two entries so the second one survives while hidden. */
    val quickTimers: List<RestPreset>,
    /** How many quick timers are shown (1 or 2). */
    val quickTimerCount: Int,
    val presets: List<RestPreset>,
    /** Last edit in epoch milliseconds, 0 for the untouched defaults. */
    val modifiedAt: Long = 0,
) {
    val visibleQuickTimers: List<RestPreset>
        get() = quickTimers.take(maxOf(1, minOf(quickTimerCount, quickTimers.size)))

    /** Every timer that can be started, quick timers first. */
    val allTimers: List<RestPreset> get() = visibleQuickTimers + presets

    fun preset(id: String): RestPreset? = (quickTimers + presets).firstOrNull { it.id == id }

    /** Repairs data that may come from an older version or a bad sync payload. */
    fun normalized(): TimerLibrary {
        val defaults = standard().quickTimers
        return copy(
            quickTimers = (quickTimers + defaults.drop(quickTimers.size)).take(2).map { it.clamped() },
            quickTimerCount = quickTimerCount.coerceIn(1, 2),
            presets = presets.take(MAX_PRESETS).map { it.clamped() },
        )
    }

    /** The timer being edited, or `null` for a new one. */
    fun timer(target: TimerEditorTarget): RestPreset? = when (target) {
        is TimerEditorTarget.Quick -> quickTimers.getOrNull(target.index)
        is TimerEditorTarget.Preset -> presets.firstOrNull { it.id == target.id }
        TimerEditorTarget.NewPreset -> null
    }

    /** Stores duration and name for [target]. */
    fun saving(seconds: Int, name: String, target: TimerEditorTarget): TimerLibrary {
        val trimmed = name.trim()
        return when (target) {
            is TimerEditorTarget.Quick -> {
                if (target.index !in quickTimers.indices) return this
                copy(
                    quickTimers = quickTimers.mapIndexed { index, preset ->
                        if (index == target.index) preset.copy(seconds = RestPreset.clamp(seconds), name = trimmed) else preset
                    },
                )
            }
            is TimerEditorTarget.Preset -> copy(
                presets = presets.map { preset ->
                    if (preset.id == target.id) preset.copy(seconds = RestPreset.clamp(seconds), name = trimmed) else preset
                },
            )
            TimerEditorTarget.NewPreset -> {
                if (presets.size >= MAX_PRESETS) return this
                // In front of the first longer timer: keeps a sorted list sorted
                // and leaves an order the user arranged untouched.
                val preset = RestPreset.of(seconds, trimmed)
                val position = presets.indexOfFirst { it.seconds > preset.seconds }.takeIf { it >= 0 } ?: presets.size
                copy(presets = presets.toMutableList().apply { add(position, preset) })
            }
        }
    }

    fun deleting(target: TimerEditorTarget): TimerLibrary =
        if (target is TimerEditorTarget.Preset) copy(presets = presets.filterNot { it.id == target.id }) else this

    /** Moves the preset at [from] to [to], e.g. one step up or down in the settings. */
    fun movingPreset(from: Int, to: Int): TimerLibrary {
        if (from !in presets.indices || to !in presets.indices || from == to) return this
        val reordered = presets.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return copy(presets = reordered)
    }

    companion object {
        const val MAX_PRESETS = 12

        fun standard(): TimerLibrary = TimerLibrary(
            quickTimers = listOf(RestPreset.of(90), RestPreset.of(180)),
            quickTimerCount = 2,
            presets = listOf(30, 45, 60, 120, 150, 240, 300).map { RestPreset.of(it) },
            modifiedAt = 0,
        )
    }
}
