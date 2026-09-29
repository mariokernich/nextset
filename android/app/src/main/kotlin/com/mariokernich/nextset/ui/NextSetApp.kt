package com.mariokernich.nextset.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.timer.RestTimerState.Phase

/** The start screen, with the settings and the timer editor on top of it. */
@Composable
fun NextSetApp(store: TimerStore, opensSettings: Boolean) {
    val state by store.state.collectAsStateWithLifecycle()
    var showsSettings by rememberSaveable { mutableStateOf(opensSettings) }
    var editor by remember { mutableStateOf<TimerEditorTarget?>(null) }
    val notifications = rememberNotificationPermission(onGranted = { store.refresh() })

    KeepScreenOn(state.settings.keepScreenOn && state.timer.isActive)

    // Like on iOS, the permission is asked for when the first rest starts.
    LaunchedEffect(state.timer.phase) {
        val settings = store.settings
        if (store.timer.phase == Phase.RUNNING && (settings.notificationsEnabled || settings.liveNotificationEnabled)) {
            notifications.askOnce()
        }
    }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            state = state,
            store = store,
            onOpenSettings = { showsSettings = true },
            onEdit = { editor = it },
        )
        AnimatedVisibility(
            visible = showsSettings,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            SettingsScreen(
                state = state,
                store = store,
                onClose = { showsSettings = false },
                onEdit = { editor = it },
                onNotificationsWanted = notifications::askOrOpenSettings,
            )
        }
    }

    editor?.let { target ->
        TimerEditorSheet(target = target, store = store, onDismiss = { editor = null })
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/** The notification permission of Android 13+. */
class NotificationPermission(private val context: Context, private val launch: () -> Unit) {
    private val isGranted: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private val preferences get() = context.getSharedPreferences("nextset", Context.MODE_PRIVATE)

    /** Asks once per installation, like iOS does. */
    fun askOnce(): Boolean {
        if (isGranted || preferences.getBoolean(ASKED, false)) return false
        preferences.edit { putBoolean(ASKED, true) }
        launch()
        return true
    }

    /** For a switch the user just turned on: ask, or show the system settings if already declined. */
    fun askOrOpenSettings() {
        if (isGranted || askOnce()) return
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private companion object {
        const val ASKED = "permission.notifications.asked"
    }
}

@Composable
private fun rememberNotificationPermission(onGranted: () -> Unit): NotificationPermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }
    return remember(context, launcher) {
        NotificationPermission(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
