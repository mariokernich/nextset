package com.mariokernich.nextset.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sounds the countdown can use. All of them are synthesised at runtime. */
@Serializable
enum class SoundStyle {
    @SerialName("beep") BEEP,
    @SerialName("chime") CHIME,
    @SerialName("digital") DIGITAL,
}

/**
 * Device-local preferences for the countdown and the end of a rest.
 *
 * Every property has a default, so settings stored by an older version (or an
 * unknown sound) never wipe the others.
 */
@Serializable
data class FeedbackSettings(
    /** Countdown signals during the last N seconds (0 = off). */
    val countdownSeconds: Int = 3,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundStyle: SoundStyle = SoundStyle.BEEP,
    /** Phone: keep the display on while a rest is running. */
    val keepScreenOn: Boolean = true,
    /** Notify when a rest ends while the app is in the background. */
    val notificationsEnabled: Boolean = true,
    /** Phone: countdown in a notification on the lock screen and in the status bar. */
    val liveNotificationEnabled: Boolean = true,
) {
    companion object {
        val COUNTDOWN_OPTIONS = listOf(0, 3, 5, 10)
    }
}
