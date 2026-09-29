package com.mariokernich.nextset.core.store

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.json.Json

/** Key-value storage for the library, the settings and the timer, each as JSON. */
interface Storage {
    fun read(key: Key): String?

    fun write(key: Key, value: String)

    enum class Key(val raw: String) {
        LIBRARY("library.v1"),
        SETTINGS("settings.v1"),
        TIMER("timer.v1"),
    }
}

/** [Storage] in the app's shared preferences. */
class PreferencesStorage(context: Context) : Storage {
    private val preferences = context.getSharedPreferences("nextset", Context.MODE_PRIVATE)

    override fun read(key: Storage.Key): String? = preferences.getString(key.raw, null)

    override fun write(key: Storage.Key, value: String) {
        preferences.edit { putString(key.raw, value) }
    }
}

/** Lenient on reading, so data from older versions or the other device never breaks the app. */
internal val NextSetJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

internal inline fun <reified T> Storage.load(key: Storage.Key): T? =
    read(key)?.let { runCatching { NextSetJson.decodeFromString<T>(it) }.getOrNull() }

internal inline fun <reified T> Storage.save(key: Storage.Key, value: T) {
    write(key, NextSetJson.encodeToString(value))
}
