package com.mariokernich.nextset.ui

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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.notify.rememberNotificationPermission
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.timer.RestTimerState.Phase

/** The start screen, with the settings and the timer editor on top of it. */
@Composable
fun NextSetApp(store: TimerStore, opensSettings: Boolean) {
    val state by store.state.collectAsStateWithLifecycle()
    var showsSettings by rememberSaveable { mutableStateOf(opensSettings) }
    // Saveable, so the editor survives rotating the phone or switching to dark mode.
    var editor by rememberSaveable(stateSaver = EditorTargetSaver) { mutableStateOf<TimerEditorTarget?>(null) }
    val notifications = rememberNotificationPermission(onGranted = { store.refresh() })
    // Checked again whenever the app comes back, e.g. from the system settings.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val notificationsBlocked = remember(lifecycleState) { !notifications.isGranted }

    // Not while paused: a forgotten pause would keep the display on for good.
    KeepScreenOn(state.settings.keepScreenOn && state.timer.phase == Phase.RUNNING)

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
                notificationsBlocked = notificationsBlocked,
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

private val EditorTargetSaver = Saver<TimerEditorTarget?, String>(
    save = { target ->
        when (target) {
            is TimerEditorTarget.Quick -> "quick:${target.index}"
            is TimerEditorTarget.Preset -> "preset:${target.id}"
            TimerEditorTarget.NewPreset -> "new"
            null -> ""
        }
    },
    restore = { saved ->
        when {
            saved.startsWith("quick:") -> saved.removePrefix("quick:").toIntOrNull()?.let(TimerEditorTarget::Quick)
            saved.startsWith("preset:") -> TimerEditorTarget.Preset(saved.removePrefix("preset:"))
            saved == "new" -> TimerEditorTarget.NewPreset
            else -> null
        }
    },
)
