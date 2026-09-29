package com.mariokernich.nextset.core.feedback

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticPattern {
    /** A rest was started. */
    START,

    /** One of the last countdown seconds. */
    TICK,

    /** The rest is over. */
    FINISH,
}

/**
 * Vibration patterns that are clearly noticeable, even with the phone on a
 * bench. They use the alarm usage, so they also come through on silent.
 */
class HapticPlayer(context: Context, private val device: FeedbackDevice) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun play(pattern: HapticPattern) {
        if (!vibrator.hasVibrator()) return
        val effect = effect(pattern)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    private fun effect(pattern: HapticPattern): VibrationEffect = when (pattern) {
        HapticPattern.START -> oneShot(30, 150)
        HapticPattern.TICK -> oneShot(if (device == FeedbackDevice.WATCH) 70 else 45, 255)
        HapticPattern.FINISH -> when (device) {
            // Three hard knocks followed by a buzz.
            FeedbackDevice.PHONE -> waveform(
                longArrayOf(0, 50, 70, 50, 70, 50, 110, 600),
                intArrayOf(0, 255, 0, 255, 0, 255, 0, 200),
            )
            // Two long pulses make the end unmistakable on the wrist.
            FeedbackDevice.WATCH -> waveform(longArrayOf(0, 300, 450, 300), intArrayOf(0, 255, 0, 255))
        }
    }

    private fun oneShot(milliseconds: Long, amplitude: Int): VibrationEffect =
        VibrationEffect.createOneShot(milliseconds, if (vibrator.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE)

    private fun waveform(timings: LongArray, amplitudes: IntArray): VibrationEffect =
        if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
}
