package com.mariokernich.nextset.core.feedback

import com.mariokernich.nextset.core.model.SoundStyle
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** What a sound is used for. */
enum class SoundCue {
    /** One of the last countdown seconds. */
    TICK,

    /** The rest is over. */
    FINISH,
}

/**
 * Synthesises the countdown sounds as mono samples in -1…1.
 *
 * Generating the audio keeps the app free of sound files and lets every style
 * share the same timing. Same formulas as the iOS app.
 */
object ToneSynth {
    const val SAMPLE_RATE = 44_100

    fun samples(cue: SoundCue, style: SoundStyle): FloatArray = when (style) {
        SoundStyle.BEEP -> when (cue) {
            SoundCue.TICK -> tone(doubleArrayOf(1_046.5), duration = 0.12, decay = 0.0, Harmonics.SINE)
            SoundCue.FINISH -> tone(doubleArrayOf(1_568.0), duration = 0.75, decay = 1.5, Harmonics.SINE)
        }
        SoundStyle.CHIME -> when (cue) {
            SoundCue.TICK -> tone(doubleArrayOf(880.0), duration = 0.35, decay = 9.0, Harmonics.BELL)
            SoundCue.FINISH -> mix(
                tone(doubleArrayOf(880.0, 1_318.5), duration = 1.4, decay = 3.0, Harmonics.BELL),
                tone(doubleArrayOf(1_760.0), duration = 1.4, decay = 4.0, Harmonics.BELL),
                offset = 0.12,
            )
        }
        SoundStyle.DIGITAL -> when (cue) {
            SoundCue.TICK -> tone(doubleArrayOf(2_093.0), duration = 0.07, decay = 0.0, Harmonics.SQUARE)
            SoundCue.FINISH -> {
                val beep = tone(doubleArrayOf(2_093.0), duration = 0.09, decay = 0.0, Harmonics.SQUARE)
                val long = tone(doubleArrayOf(2_093.0), duration = 0.5, decay = 0.0, Harmonics.SQUARE)
                sequence(beep, silence(0.06), beep, silence(0.06), long)
            }
        }
    }

    /** 16-bit PCM for `AudioTrack`. */
    fun pcm16(samples: FloatArray): ShortArray =
        ShortArray(samples.size) { (samples[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }

    // Building blocks

    /** (frequency multiplier, amplitude, extra decay) per partial. */
    enum class Harmonics(val partials: List<Triple<Double, Double, Double>>) {
        SINE(listOf(Triple(1.0, 1.0, 0.0))),

        /** Band-limited square wave: odd harmonics only. */
        SQUARE(listOf(Triple(1.0, 1.0, 0.0), Triple(3.0, 1.0 / 3, 0.0), Triple(5.0, 1.0 / 5, 0.0), Triple(7.0, 1.0 / 7, 0.0))),

        /** Inharmonic partials of a small bell; higher ones fade faster. */
        BELL(listOf(Triple(1.0, 1.0, 0.0), Triple(2.76, 0.45, 2.0), Triple(5.4, 0.25, 5.0), Triple(8.93, 0.1, 8.0))),
    }

    fun tone(frequencies: DoubleArray, duration: Double, decay: Double, harmonics: Harmonics): FloatArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val attack = 0.004 * SAMPLE_RATE
        val release = minOf(0.02 * SAMPLE_RATE, count / 4.0)
        val partials = harmonics.partials
        val norm = partials.sumOf { it.second } * frequencies.size
        return FloatArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            var value = 0.0
            for (frequency in frequencies) {
                for ((multiple, amplitude, extraDecay) in partials) {
                    val f = frequency * multiple
                    if (f >= SAMPLE_RATE / 2.0) continue
                    value += amplitude * exp(-extraDecay * t) * sin(2 * PI * f * t)
                }
            }
            var envelope = exp(-decay * t)
            if (i < attack) envelope *= i / attack
            val untilEnd = (count - i).toDouble()
            if (untilEnd < release) envelope *= untilEnd / release
            (0.85 * envelope * value / norm).toFloat()
        }
    }

    fun silence(duration: Double): FloatArray = FloatArray((duration * SAMPLE_RATE).toInt())

    fun sequence(vararg parts: FloatArray): FloatArray = parts.reduce { acc, part -> acc + part }

    fun mix(a: FloatArray, b: FloatArray, offset: Double): FloatArray {
        val shift = (offset * SAMPLE_RATE).toInt()
        val out = FloatArray(maxOf(a.size, b.size + shift))
        for (i in a.indices) out[i] += a[i] * 0.6f
        for (i in b.indices) out[i + shift] += b[i] * 0.6f
        return out
    }
}
