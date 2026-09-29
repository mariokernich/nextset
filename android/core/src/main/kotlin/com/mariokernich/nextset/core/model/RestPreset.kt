package com.mariokernich.nextset.core.model

import java.util.UUID
import kotlinx.serialization.Serializable

/** A predefined rest duration, e.g. "1:30". */
@Serializable
data class RestPreset(
    val id: String = UUID.randomUUID().toString(),
    val seconds: Int,
    /** Optional label such as "Squats". Empty when unnamed. */
    val name: String = "",
) {
    val trimmedName: String get() = name.trim()

    /** The same timer with its duration inside the allowed range. */
    fun clamped(): RestPreset = if (seconds in ALLOWED_SECONDS) this else copy(seconds = clamp(seconds))

    companion object {
        /** Rest timers between 5 seconds and 60 minutes are allowed. */
        val ALLOWED_SECONDS = 5..3600

        fun clamp(seconds: Int): Int = seconds.coerceIn(ALLOWED_SECONDS)

        fun of(seconds: Int, name: String = ""): RestPreset = RestPreset(seconds = clamp(seconds), name = name)
    }
}
