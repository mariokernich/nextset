package com.mariokernich.nextset.core

import com.mariokernich.nextset.core.feedback.SoundCue
import com.mariokernich.nextset.core.feedback.ToneSynth
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.model.SoundStyle
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormattingAndSoundTest {
    @Test
    fun clock() {
        mapOf(0 to "0:00", 5 to "0:05", 45 to "0:45", 90 to "1:30", 600 to "10:00", 3599 to "59:59")
            .forEach { (seconds, expected) -> assertEquals(expected, DurationFormat.clock(seconds)) }
    }

    @Test
    fun soundsAreAudibleAndInRange() {
        for (style in SoundStyle.entries) {
            for (cue in SoundCue.entries) {
                val samples = ToneSynth.samples(cue, style)
                assertTrue(samples.isNotEmpty())
                assertTrue(samples.all { abs(it) <= 1f }, "$style $cue exceeds the range")
                assertTrue(samples.any { abs(it) > 0.2f }, "$style $cue should be clearly audible")
                assertEquals(samples.size, ToneSynth.pcm16(samples).size)
            }
        }
    }

    @Test
    fun finishIsLongerThanTick() {
        for (style in SoundStyle.entries) {
            assertTrue(ToneSynth.samples(SoundCue.FINISH, style).size > ToneSynth.samples(SoundCue.TICK, style).size)
        }
    }
}
