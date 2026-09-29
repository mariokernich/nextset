package com.mariokernich.nextset.wear

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mariokernich.nextset.core.NextSetHost
import com.mariokernich.nextset.core.feedback.FeedbackCoordinator
import com.mariokernich.nextset.core.feedback.FeedbackDevice
import com.mariokernich.nextset.core.store.PreferencesStorage
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.sync.LibrarySync
import kotlinx.coroutines.MainScope

/** Owns the long-lived objects of the watch app, like `WatchModel` on the Apple Watch. */
class WearApplication : Application(), NextSetHost {
    private val scope = MainScope()

    override lateinit var store: TimerStore
        private set

    private lateinit var services: WatchServices

    override fun onCreate() {
        super.onCreate()
        store = TimerStore(PreferencesStorage(this), FeedbackCoordinator(this, FeedbackDevice.WATCH), scope)
        services = WatchServices(this, store)
        store.sideEffects = services
        LibrarySync(this, store, scope).start()

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    store.refresh()
                    services.appDidBecomeActive()
                }
            },
        )
    }
}
