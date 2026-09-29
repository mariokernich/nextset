package com.mariokernich.nextset.core.sync

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.mariokernich.nextset.core.NextSetHost
import com.mariokernich.nextset.core.model.TimerLibrary
import com.mariokernich.nextset.core.store.NextSetJson
import com.mariokernich.nextset.core.store.TimerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Keeps the timer library in sync between phone and watch.
 *
 * Each device publishes its library as a data item of the Wearable Data Layer;
 * the other one picks it up, also while it isn't running
 * ([LibraryListenerService]). The newest edit wins. Without Google Play
 * services or a paired device, nothing happens.
 */
class LibrarySync(context: Context, private val store: TimerStore, private val scope: CoroutineScope) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    fun start() {
        store.onLocalLibraryChange = { publish(it) }
        scope.launch {
            val items = runCatching { dataClient.getDataItems(LIBRARY_URI, DataClient.FILTER_LITERAL).await() }.getOrNull()
            if (items != null) {
                try {
                    items.mapNotNull(::decode).forEach(store::applyRemoteLibrary)
                } finally {
                    items.release()
                }
            }
            // Make sure a freshly installed counterpart gets the edited timers.
            if (store.library.modifiedAt > 0) publish(store.library)
        }
    }

    private fun publish(library: TimerLibrary) {
        val request = PutDataMapRequest.create(PATH)
            .apply { dataMap.putString(KEY, NextSetJson.encodeToString(library)) }
            .asPutDataRequest()
            .setUrgent()
        scope.launch {
            runCatching { dataClient.putDataItem(request).await() }
        }
    }

    companion object {
        const val PATH = "/library"
        private const val KEY = "library"
        private val LIBRARY_URI: Uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(PATH).build()

        internal fun decode(item: DataItem): TimerLibrary? = runCatching {
            DataMapItem.fromDataItem(item).dataMap.getString(KEY)?.let { NextSetJson.decodeFromString<TimerLibrary>(it) }
        }.getOrNull()
    }
}

/** Receives timers edited on the other device, also while the app isn't running. */
class LibraryListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val libraries = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == LibrarySync.PATH }
            .mapNotNull { LibrarySync.decode(it.dataItem) }
        if (libraries.isEmpty()) return
        val store = (application as? NextSetHost)?.store ?: return
        Handler(Looper.getMainLooper()).post {
            libraries.forEach(store::applyRemoteLibrary)
        }
    }
}
