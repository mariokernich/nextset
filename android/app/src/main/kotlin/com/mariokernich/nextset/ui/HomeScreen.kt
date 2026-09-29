package com.mariokernich.nextset.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mariokernich.nextset.R
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.store.TimerSnapshot
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.ui.KettlebellMark
import com.mariokernich.nextset.core.R as CoreR

/** The only screen: dial, controls, quick timers and further presets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: TimerSnapshot,
    store: TimerStore,
    onOpenSettings: () -> Unit,
    onEdit: (TimerEditorTarget) -> Unit,
) {
    Scaffold(
        modifier = Modifier.background(NextSetTheme.colors.backdrop),
        containerColor = Color.Transparent,
        // A transparent container has no content colour of its own.
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
                    ) {
                        KettlebellMark(bell = MaterialTheme.colorScheme.onSurface, modifier = Modifier.height(22.dp))
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                },
                navigationIcon = {
                    SoundToggle(isOn = state.settings.soundEnabled) {
                        store.updateSettings { it.copy(soundEnabled = !it.soundEnabled) }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(CoreR.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val dialSize = maxOf(200.dp, minOf(maxWidth - 80.dp, maxHeight * 0.42f, 340.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    TimerDial(state = state, onStop = store::stop, modifier = Modifier.size(dialSize))
                    ControlBar(
                        phase = state.timer.phase,
                        onAdjust = store::adjust,
                        onPrimary = store::primaryAction,
                    )
                    QuickTimers(state = state, onStart = { store.start(it) }, onEdit = onEdit)
                    Presets(
                        state = state,
                        onStart = { store.start(it) },
                        onEdit = onEdit,
                        onDelete = { id -> store.updateLibrary { it.deleting(TimerEditorTarget.Preset(id)) } },
                    )
                }
            }
        }
    }
}

/** Quick mute for the countdown sounds, right in the toolbar. */
@Composable
private fun SoundToggle(isOn: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Crossfade(targetState = isOn, label = "sound") { on ->
            Icon(
                imageVector = if (on) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                contentDescription = stringResource(if (on) R.string.sounds_on else R.string.sounds_off),
            )
        }
    }
}
