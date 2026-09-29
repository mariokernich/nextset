package com.mariokernich.nextset.core.feedback

import android.content.Context
import com.mariokernich.nextset.core.model.FeedbackSettings
import com.mariokernich.nextset.core.model.SoundStyle

/** What the timer plays at its moments. */
interface Feedback {
    fun timerStarted(settings: FeedbackSettings)

    fun countdownTick(secondsLeft: Int, settings: FeedbackSettings)

    fun finished(settings: FeedbackSettings)

    /** Lets the user hear a sound style in the settings. */
    fun preview(style: SoundStyle) = Unit
}

enum class FeedbackDevice { PHONE, WATCH }

/** Combines haptics and sounds according to the user's settings. */
class FeedbackCoordinator(context: Context, device: FeedbackDevice) : Feedback {
    private val haptics = HapticPlayer(context, device)
    private val sounds = SoundPlayer(context, device)

    override fun timerStarted(settings: FeedbackSettings) {
        if (settings.hapticsEnabled) haptics.play(HapticPattern.START)
        if (settings.soundEnabled) sounds.prepare(settings.soundStyle)
    }

    override fun countdownTick(secondsLeft: Int, settings: FeedbackSettings) {
        if (settings.hapticsEnabled) haptics.play(HapticPattern.TICK)
        if (settings.soundEnabled) sounds.play(SoundCue.TICK, settings.soundStyle)
    }

    override fun finished(settings: FeedbackSettings) {
        if (settings.hapticsEnabled) haptics.play(HapticPattern.FINISH)
        if (settings.soundEnabled) sounds.play(SoundCue.FINISH, settings.soundStyle)
    }

    override fun preview(style: SoundStyle) {
        sounds.play(SoundCue.FINISH, style)
    }
}
