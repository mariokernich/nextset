package com.mariokernich.nextset

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

/** Owns the long-lived objects of the phone app, like `AppModel` on iOS. */
class NextSetApplication : Application(), NextSetHost {
    private val scope = MainScope()

    override lateinit var store: TimerStore
        private set

    private lateinit var services: PhoneServices

    override fun onCreate() {
        super.onCreate()
        store = TimerStore(PreferencesStorage(this), FeedbackCoordinator(this, FeedbackDevice.PHONE), scope)
        services = PhoneServices(this, store)
        store.sideEffects = services
        LibrarySync(this, store, scope).start()
        Shortcuts.follow(this, store, scope)

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
