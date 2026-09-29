package com.mariokernich.nextset.core.format

import android.content.res.Resources
import com.mariokernich.nextset.core.R

object DurationFormat {
    /** "1:30", "0:45", "12:00". */
    fun clock(seconds: Int): String {
        val clamped = maxOf(0, seconds)
        val rest = clamped % 60
        return "${clamped / 60}:" + if (rest < 10) "0$rest" else "$rest"
    }

    /** Spoken form for TalkBack, e.g. "1 minute, 30 seconds". */
    fun spoken(resources: Resources, seconds: Int): String {
        val clamped = maxOf(0, seconds)
        val minutes = clamped / 60
        val rest = clamped % 60
        return buildList {
            if (minutes > 0) add(resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes))
            if (rest > 0 || minutes == 0) add(resources.getQuantityString(R.plurals.duration_seconds, rest, rest))
        }.joinToString(", ")
    }
}
