package com.mariokernich.nextset.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.wear.ambient.AmbientLifecycleObserver
import com.mariokernich.nextset.core.DemoMode
import com.mariokernich.nextset.core.NextSetHost
import com.mariokernich.nextset.wear.ui.WearApp
import com.mariokernich.nextset.wear.ui.WearTheme

class MainActivity : ComponentActivity() {
    private val store get() = (application as NextSetHost).store

    /** Always On: the timer stays on screen, dimmed, when the wrist is lowered. */
    private var isAmbient by mutableStateOf(false)

    /** The minute update in Always On, when the app may otherwise be asleep. */
    private var ambientUpdate by mutableLongStateOf(0L)

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient = true
        }

        override fun onUpdateAmbient() {
            // E.g. the 15-minute reset after the end, which waits while the watch sleeps.
            store.refresh()
            ambientUpdate = System.currentTimeMillis()
        }

        override fun onExitAmbient() {
            isAmbient = false
            store.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))
        if (savedInstanceState == null && BuildConfig.DEBUG) {
            DemoMode.apply(store, intent.getStringExtra(DemoMode.EXTRA), intent.getLongExtra(DemoMode.EXTRA_END, 0))
        }
        setContent {
            WearTheme {
                WearApp(store = store, isAmbient = isAmbient, ambientUpdate = ambientUpdate)
            }
        }
    }
}
