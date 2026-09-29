package com.mariokernich.nextset.core

import com.mariokernich.nextset.core.store.TimerStore

/**
 * Implemented by the `Application` of the phone and the watch app, so the
 * alarm and the sync can reach the shared timer.
 */
interface NextSetHost {
    val store: TimerStore
}
