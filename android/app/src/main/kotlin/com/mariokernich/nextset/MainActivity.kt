package com.mariokernich.nextset

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mariokernich.nextset.core.DemoMode
import com.mariokernich.nextset.core.NextSetHost
import com.mariokernich.nextset.ui.NextSetApp
import com.mariokernich.nextset.ui.NextSetTheme

class MainActivity : ComponentActivity() {
    private val store get() = (application as NextSetHost).store

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val opensSettings = BuildConfig.DEBUG && intent.getStringExtra(DemoMode.EXTRA) == "settings"
        if (savedInstanceState == null) handle(intent)
        setContent {
            NextSetTheme {
                NextSetApp(store, opensSettings = opensSettings)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** Launcher shortcuts and, in debug builds, the screenshot states. */
    private fun handle(intent: Intent?) {
        when (intent?.action) {
            Shortcuts.ACTION_START_QUICK -> {
                val slot = intent.getIntExtra(Shortcuts.EXTRA_SLOT, 0)
                store.library.quickTimers.getOrNull(slot)?.let(store::start)
                Shortcuts.reportUsed(this, Shortcuts.quickTimerId(slot))
            }
            Shortcuts.ACTION_END_REST -> {
                store.stop()
                Shortcuts.reportUsed(this, Shortcuts.END_REST_ID)
            }
        }
        if (BuildConfig.DEBUG && intent != null) {
            DemoMode.apply(store, intent.getStringExtra(DemoMode.EXTRA), intent.getLongExtra(DemoMode.EXTRA_END, 0))
        }
    }
}
