package com.mariokernich.nextset.core.model

import com.mariokernich.nextset.core.store.NextSetJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimerLibraryTest {
    @Test
    fun standardLibraryHasTwoQuickTimers() {
        val library = TimerLibrary.standard()
        assertEquals(listOf(90, 180), library.visibleQuickTimers.map { it.seconds })
        assertEquals(2 + library.presets.size, library.allTimers.size)
    }

    @Test
    fun hidingTheSecondQuickTimerKeepsItsValue() {
        val library = TimerLibrary.standard().copy(quickTimerCount = 1)

        assertEquals(listOf(90), library.visibleQuickTimers.map { it.seconds })
        assertEquals(2, library.quickTimers.size)
    }

    @Test
    fun normalizationRepairsBrokenData() {
        val broken = TimerLibrary(
            quickTimers = listOf(RestPreset(seconds = 1)),
            quickTimerCount = 7,
            presets = (0 until 20).map { RestPreset.of(60 + it) },
            modifiedAt = 1,
        )
        val fixed = broken.normalized()

        assertEquals(2, fixed.quickTimers.size)
        assertEquals(RestPreset.ALLOWED_SECONDS.first, fixed.quickTimers[0].seconds)
        assertEquals(2, fixed.quickTimerCount)
        assertEquals(TimerLibrary.MAX_PRESETS, fixed.presets.size)
    }

    @Test
    fun findsPresetsByID() {
        val library = TimerLibrary.standard()
        val preset = library.presets[2]
        assertEquals(preset, library.preset(preset.id))
        assertNull(library.preset("missing"))
    }

    @Test
    fun settingsDecodeWithMissingKeys() {
        val settings = NextSetJson.decodeFromString<FeedbackSettings>("""{"countdownSeconds": 10, "soundStyle": "unknown"}""")

        assertEquals(10, settings.countdownSeconds)
        assertEquals(SoundStyle.BEEP, settings.soundStyle)
        assertTrue(settings.hapticsEnabled)
    }

    @Test
    fun editsQuickTimers() {
        val library = TimerLibrary.standard().saving(100, "  Bench  ", TimerEditorTarget.Quick(1))

        assertEquals(100, library.quickTimers[1].seconds)
        assertEquals("Bench", library.quickTimers[1].name)
    }

    @Test
    fun addsPresetsSortedByDuration() {
        val library = TimerLibrary.standard().saving(50, "", TimerEditorTarget.NewPreset)

        assertEquals(library.presets.map { it.seconds }.sorted(), library.presets.map { it.seconds })
        assertTrue(library.presets.any { it.seconds == 50 })
    }

    @Test
    fun addingKeepsAnOrderTheUserArranged() {
        val arranged = TimerLibrary.standard().let { it.copy(presets = it.presets.reversed()) }
        val library = arranged.saving(50, "", TimerEditorTarget.NewPreset)

        assertEquals(arranged.presets, library.presets.filterNot { it.seconds == 50 })
    }

    @Test
    fun deletesPresets() {
        val standard = TimerLibrary.standard()
        val preset = standard.presets[0]
        val library = standard.deleting(TimerEditorTarget.Preset(preset.id))

        assertNull(library.timer(TimerEditorTarget.Preset(preset.id)))
    }

    @Test
    fun respectsThePresetLimit() {
        var library = TimerLibrary.standard()
        for (seconds in 400 until 1000 step 20) {
            library = library.saving(seconds, "", TimerEditorTarget.NewPreset)
        }
        assertEquals(TimerLibrary.MAX_PRESETS, library.presets.size)
    }

    @Test
    fun movesPresets() {
        val standard = TimerLibrary.standard()
        val moved = standard.movingPreset(from = 0, to = 2)

        assertEquals(standard.presets[0], moved.presets[2])
        assertEquals(standard.presets[1], moved.presets[0])
        assertEquals(standard, standard.movingPreset(from = 0, to = 99))
    }
}
