package com.mariokernich.nextset.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.currentBackStackEntryAsState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.notify.rememberNotificationPermission
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import com.mariokernich.nextset.wear.R

private const val HOME = "home"
private const val TIMER = "timer"
private const val SETTINGS = "settings"
private const val EDITOR = "editor/{target}"

/** Home with the timers; a running rest is shown on top of it. */
@Composable
fun WearApp(store: TimerStore, isAmbient: Boolean, ambientUpdate: Long) {
    val state by store.state.collectAsStateWithLifecycle()
    val navController = rememberSwipeDismissableNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val showsTimer = state.timer.phase != Phase.IDLE
    val notifications = rememberNotificationPermission(onGranted = { store.refresh() })

    LaunchedEffect(showsTimer) {
        val onTimer = navController.currentDestination?.route == TIMER
        if (showsTimer && !onTimer) {
            navController.navigate(TIMER) { popUpTo(HOME) }
        } else if (!showsTimer && onTimer) {
            navController.popBackStack(HOME, inclusive = false)
        }
    }

    // The rest on the watch face and the fallback at its end are notifications,
    // which Wear OS 4+ only shows with permission. Asked for with the first rest.
    LaunchedEffect(state.timer.phase) {
        if (store.timer.phase == Phase.RUNNING) notifications.askOnce()
    }

    // Swiping the timer away ends the rest, like leaving it on the Apple Watch.
    DisposableEffect(navController) {
        var previous: String? = null
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (previous == TIMER && destination.route != TIMER && store.timer.phase != Phase.IDLE) store.stop()
            previous = destination.route
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    AppScaffold {
        SwipeDismissableNavHost(navController = navController, startDestination = HOME) {
            composable(HOME) {
                HomeScreen(
                    state = state,
                    onStart = { store.start(it) },
                    onOpenSettings = { navController.navigate(SETTINGS) },
                )
            }
            composable(TIMER) {
                TimerScreen(state = state, store = store, isAmbient = isAmbient, ambientUpdate = ambientUpdate)
            }
            composable(SETTINGS) {
                SettingsScreen(
                    state = state,
                    store = store,
                    onEdit = { navController.navigate("editor/${it.route}") },
                    onNotificationsWanted = notifications::askOrOpenSettings,
                )
            }
            composable(EDITOR) { entry ->
                val target = entry.arguments?.getString("target")?.let(::editorTarget) ?: TimerEditorTarget.NewPreset
                TimerEditorScreen(target = target, store = store, onDone = { navController.popBackStack() })
            }
        }
    }

    // Always On is meant for the timer: the other screens would stay bright,
    // so they make way for a nearly black screen.
    if (isAmbient && route != TIMER) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.app_name), color = Color.Gray)
        }
    }
}

private val TimerEditorTarget.route: String
    get() = when (this) {
        is TimerEditorTarget.Quick -> "quick-$index"
        is TimerEditorTarget.Preset -> "preset-$id"
        TimerEditorTarget.NewPreset -> "new"
    }

private fun editorTarget(route: String): TimerEditorTarget = when {
    route.startsWith("quick-") -> TimerEditorTarget.Quick(route.removePrefix("quick-").toIntOrNull() ?: 0)
    route.startsWith("preset-") -> TimerEditorTarget.Preset(route.removePrefix("preset-"))
    else -> TimerEditorTarget.NewPreset
}
