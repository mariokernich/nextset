package com.mariokernich.nextset.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.model.FeedbackSettings
import com.mariokernich.nextset.core.model.RestPreset
import com.mariokernich.nextset.core.model.SoundStyle
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.model.TimerLibrary
import com.mariokernich.nextset.core.store.TimerSnapshot
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.wear.BuildConfig
import com.mariokernich.nextset.wear.R
import com.mariokernich.nextset.core.R as CoreR

@Composable
fun SettingsScreen(state: TimerSnapshot, store: TimerStore, onEdit: (TimerEditorTarget) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val settings = state.settings
    val library = state.library
    fun update(change: (FeedbackSettings) -> FeedbackSettings) = store.updateSettings(change)

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(CoreR.string.settings)) } }

            item { SectionHeader(stringResource(CoreR.string.countdown)) }
            item {
                // Tapping steps through the choices: off, 3, 5, 10 seconds.
                val options = FeedbackSettings.COUNTDOWN_OPTIONS
                Button(
                    onClick = {
                        val next = options[(options.indexOf(settings.countdownSeconds) + 1) % options.size]
                        update { it.copy(countdownSeconds = next) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = WearColors.preset),
                    label = { Text(stringResource(CoreR.string.signal_during_last)) },
                    secondaryLabel = {
                        Text(
                            if (settings.countdownSeconds == 0) {
                                stringResource(CoreR.string.off)
                            } else {
                                pluralStringResource(CoreR.plurals.countdown_seconds, settings.countdownSeconds, settings.countdownSeconds)
                            },
                            color = WearColors.coral,
                        )
                    },
                )
            }
            item {
                SwitchButton(
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { on -> update { it.copy(hapticsEnabled = on) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(CoreR.string.haptics)) },
                )
            }
            item {
                SwitchButton(
                    checked = settings.soundEnabled,
                    onCheckedChange = { on -> update { it.copy(soundEnabled = on) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(CoreR.string.sounds)) },
                )
            }
            if (settings.soundEnabled) {
                item {
                    Button(
                        onClick = {
                            val styles = SoundStyle.entries
                            val next = styles[(styles.indexOf(settings.soundStyle) + 1) % styles.size]
                            update { it.copy(soundStyle = next) }
                            store.feedback.preview(next)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = WearColors.preset),
                        label = { Text(stringResource(CoreR.string.sound)) },
                        secondaryLabel = { Text(stringResource(settings.soundStyle.title), color = WearColors.coral) },
                    )
                }
            }

            item { SectionHeader(stringResource(CoreR.string.timers)) }
            library.visibleQuickTimers.forEachIndexed { index, preset ->
                item(key = "quick-${preset.id}") {
                    TimerButton(preset, isQuick = true) { onEdit(TimerEditorTarget.Quick(index)) }
                }
            }
            library.presets.forEach { preset ->
                item(key = preset.id) {
                    TimerButton(preset, isQuick = false) { onEdit(TimerEditorTarget.Preset(preset.id)) }
                }
            }
            if (library.presets.size < TimerLibrary.MAX_PRESETS) {
                item {
                    Button(
                        onClick = { onEdit(TimerEditorTarget.NewPreset) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = WearColors.preset),
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null, tint = WearColors.coral) },
                        label = { Text(stringResource(CoreR.string.add_timer)) },
                    )
                }
            }
            item { Footnote(stringResource(R.string.timers_sync_phone)) }

            item {
                SwitchButton(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { on -> update { it.copy(notificationsEnabled = on) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(CoreR.string.notify_when_rest_over)) },
                )
            }
            item {
                Footnote(
                    stringResource(CoreR.string.version, BuildConfig.VERSION_NAME) + " · " + stringResource(CoreR.string.tagline),
                )
            }
        }
    }
}

@Composable
private fun TimerButton(preset: RestPreset, isQuick: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = WearColors.preset),
        icon = if (isQuick) {
            { Icon(Icons.Rounded.Bolt, contentDescription = null, tint = WearColors.accent) }
        } else {
            null
        },
        label = { Text(DurationFormat.clock(preset.seconds), style = TabularNumbers) },
        secondaryLabel = if (preset.trimmedName.isNotEmpty()) {
            { Text(preset.trimmedName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else {
            null
        },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp),
    )
}

@Composable
private fun Footnote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private val SoundStyle.title: Int
    get() = when (this) {
        SoundStyle.BEEP -> CoreR.string.sound_beep
        SoundStyle.CHIME -> CoreR.string.sound_chime
        SoundStyle.DIGITAL -> CoreR.string.sound_digital
    }
