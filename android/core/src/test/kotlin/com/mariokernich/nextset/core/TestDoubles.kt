package com.mariokernich.nextset.core

import com.mariokernich.nextset.core.feedback.Feedback
import com.mariokernich.nextset.core.model.FeedbackSettings
import com.mariokernich.nextset.core.store.Storage

class MemoryStorage : Storage {
    private val values = mutableMapOf<Storage.Key, String>()

    override fun read(key: Storage.Key): String? = values[key]

    override fun write(key: Storage.Key, value: String) {
        values[key] = value
    }
}

object SilentFeedback : Feedback {
    override fun timerStarted(settings: FeedbackSettings) = Unit

    override fun countdownTick(secondsLeft: Int, settings: FeedbackSettings) = Unit

    override fun finished(settings: FeedbackSettings) = Unit
}
