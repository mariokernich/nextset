package com.mariokernich.nextset.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mariokernich.nextset.BuildConfig
import com.mariokernich.nextset.R
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.model.FeedbackSettings
import com.mariokernich.nextset.core.model.RestPreset
import com.mariokernich.nextset.core.model.SoundStyle
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.model.TimerLibrary
import com.mariokernich.nextset.core.store.TimerSnapshot
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.ui.KettlebellMark
import com.mariokernich.nextset.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: TimerSnapshot,
    store: TimerStore,
    notificationsBlocked: Boolean,
    onClose: () -> Unit,
    onEdit: (TimerEditorTarget) -> Unit,
    onNotificationsWanted: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val settings = state.settings
    val library = state.library
    fun updateSettings(change: (FeedbackSettings) -> FeedbackSettings) = store.updateSettings(change)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(CoreR.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp + bottomInset),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item { QuickTimersSection(library, store, onEdit) }
                item { PresetsSection(library, store, onEdit) }
                item {
                    CountdownSection(
                        settings = settings,
                        update = ::updateSettings,
                        onPreview = { store.feedback.preview(it) },
                    )
                }
                item {
                    Section(title = stringResource(R.string.during_rest), footer = stringResource(R.string.during_rest_footer)) {
                        SwitchRow(Icons.Rounded.LightMode, stringResource(R.string.keep_display_on), settings.keepScreenOn) { on ->
                            updateSettings { it.copy(keepScreenOn = on) }
                        }
                        RowDivider()
                        SwitchRow(Icons.Rounded.NotificationsActive, stringResource(CoreR.string.notify_when_rest_over), settings.notificationsEnabled) { on ->
                            updateSettings { it.copy(notificationsEnabled = on) }
                            if (on) onNotificationsWanted()
                        }
                        RowDivider()
                        SwitchRow(Icons.Rounded.ScreenLockPortrait, stringResource(R.string.live_notification), settings.liveNotificationEnabled) { on ->
                            updateSettings { it.copy(liveNotificationEnabled = on) }
                            if (on) onNotificationsWanted()
                        }
                        // The switches are on, but the system doesn't show NextSet's notifications.
                        if (notificationsBlocked && (settings.notificationsEnabled || settings.liveNotificationEnabled)) {
                            RowDivider()
                            SettingsRow(onClick = onNotificationsWanted) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = NextSetTheme.colors.coral)
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.allow_notifications), color = NextSetTheme.colors.coral)
                            }
                        }
                    }
                }
                item { AboutSection() }
            }
        }
    }
}

// Sections

@Composable
private fun QuickTimersSection(library: TimerLibrary, store: TimerStore, onEdit: (TimerEditorTarget) -> Unit) {
    val colors = NextSetTheme.colors
    Section(title = stringResource(CoreR.string.quick_timers), footer = stringResource(R.string.quick_timers_footer)) {
        SettingsRow {
            Text(stringResource(R.string.number_of_quick_timers), modifier = Modifier.weight(1f))
            SingleChoiceSegmentedButtonRow(Modifier.width(120.dp)) {
                listOf(1, 2).forEachIndexed { index, count ->
                    SegmentedButton(
                        selected = library.quickTimerCount == count,
                        onClick = { store.updateLibrary { it.copy(quickTimerCount = count) } },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.accentStrong,
                            activeContentColor = colors.onAccentStrong,
                        ),
                        icon = {},
                    ) { Text(count.toString()) }
                }
            }
        }
        library.visibleQuickTimers.forEachIndexed { index, preset ->
            RowDivider()
            TimerRow(preset, quickIndex = index, onClick = { onEdit(TimerEditorTarget.Quick(index)) })
        }
    }
}

@Composable
private fun PresetsSection(library: TimerLibrary, store: TimerStore, onEdit: (TimerEditorTarget) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val presets = library.presets
    // The Done button disappears with the last timer, so leave the edit mode with it.
    LaunchedEffect(presets.isEmpty()) {
        if (presets.isEmpty()) editing = false
    }
    Section(
        title = stringResource(CoreR.string.more_timers),
        footer = stringResource(R.string.more_timers_footer),
        action = {
            if (presets.isNotEmpty()) {
                TextButton(onClick = { editing = !editing }) {
                    Text(stringResource(if (editing) R.string.done else CoreR.string.edit))
                }
            }
        },
    ) {
        presets.forEachIndexed { index, preset ->
            if (editing) {
                EditablePresetRow(
                    preset = preset,
                    canMoveUp = index > 0,
                    canMoveDown = index < presets.lastIndex,
                    onDelete = { store.updateLibrary { it.deleting(TimerEditorTarget.Preset(preset.id)) } },
                    onMove = { offset -> store.updateLibrary { it.movingPreset(index, index + offset) } },
                )
            } else {
                TimerRow(preset, quickIndex = null, onClick = { onEdit(TimerEditorTarget.Preset(preset.id)) })
            }
            RowDivider()
        }
        val canAdd = presets.size < TimerLibrary.MAX_PRESETS
        SettingsRow(onClick = { onEdit(TimerEditorTarget.NewPreset) }, enabled = canAdd) {
            Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = if (canAdd) NextSetTheme.colors.coral else MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(16.dp))
            Text(stringResource(CoreR.string.add_timer), color = if (canAdd) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun CountdownSection(
    settings: FeedbackSettings,
    update: ((FeedbackSettings) -> FeedbackSettings) -> Unit,
    onPreview: (SoundStyle) -> Unit,
) {
    Section(title = stringResource(CoreR.string.countdown), footer = stringResource(R.string.countdown_footer)) {
        ChoiceRow(
            icon = Icons.Rounded.Timer,
            title = stringResource(CoreR.string.signal_during_last),
            options = FeedbackSettings.COUNTDOWN_OPTIONS,
            selected = settings.countdownSeconds,
            label = { seconds ->
                if (seconds == 0) stringResource(CoreR.string.off) else pluralStringResource(CoreR.plurals.countdown_seconds, seconds, seconds)
            },
            onSelect = { seconds -> update { it.copy(countdownSeconds = seconds) } },
        )
        RowDivider()
        SwitchRow(Icons.Rounded.Vibration, stringResource(CoreR.string.haptics), settings.hapticsEnabled) { on ->
            update { it.copy(hapticsEnabled = on) }
        }
        RowDivider()
        SwitchRow(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(CoreR.string.sounds), settings.soundEnabled) { on ->
            update { it.copy(soundEnabled = on) }
        }
        if (settings.soundEnabled) {
            RowDivider()
            ChoiceRow(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(CoreR.string.sound),
                options = SoundStyle.entries,
                selected = settings.soundStyle,
                label = { stringResource(it.title) },
                onSelect = { style ->
                    update { it.copy(soundStyle = style) }
                    onPreview(style)
                },
            )
            RowDivider()
            SettingsRow(onClick = { onPreview(settings.soundStyle) }) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = NextSetTheme.colors.coral)
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.play_sound))
            }
        }
    }
}

@Composable
private fun AboutSection() {
    Surface(
        color = NextSetTheme.colors.card,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            KettlebellMark(bell = MaterialTheme.colorScheme.onSurface, modifier = Modifier.height(42.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(stringResource(CoreR.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(CoreR.string.tagline), style = MaterialTheme.typography.bodyMedium, color = NextSetTheme.colors.accentText)
            }
        }
    }
}

// Building blocks

/** A group of rows in a card, with a header and an explanation below, like the iOS forms. */
@Composable
private fun Section(
    title: String,
    footer: String? = null,
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, top = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            action()
        }
        Surface(color = NextSetTheme.colors.card, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingsRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        content = content,
    )
}

@Composable
private fun SettingsRow(onClick: () -> Unit, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    SettingsRow(Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick), content)

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // Toggleable rather than clickable, so TalkBack also reads whether it's on.
    SettingsRow(Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = NextSetTheme.colors.coral, checkedThumbColor = Color.White),
        )
    }
}

/** A row with the current value that opens a menu of choices. */
@Composable
private fun <T> ChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SettingsRow(onClick = { expanded = true }) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f))
            Text(label(selected), color = NextSetTheme.colors.accentText, fontWeight = FontWeight.SemiBold)
        }
        // Anchored at the end of the row, so the menu opens below the current value.
        Box(Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(label(option), fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            expanded = false
                            if (option != selected) onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerRow(preset: RestPreset, quickIndex: Int?, onClick: () -> Unit) {
    val colors = NextSetTheme.colors
    val time = DurationFormat.clock(preset.seconds)
    SettingsRow(onClick = onClick) {
        if (quickIndex != null) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = preset.trimmedName.ifEmpty { stringResource(CoreR.string.quick_timer_n, quickIndex + 1) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(time, fontWeight = FontWeight.SemiBold, style = TabularNumbers, color = colors.accentText)
        } else {
            Text(time, fontWeight = FontWeight.SemiBold, style = TabularNumbers, color = colors.accentText)
            Spacer(Modifier.width(12.dp))
            Text(
                text = preset.trimmedName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun EditablePresetRow(
    preset: RestPreset,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
) {
    SettingsRow {
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.RemoveCircle, contentDescription = stringResource(CoreR.string.delete), tint = MaterialTheme.colorScheme.error)
        }
        Text(DurationFormat.clock(preset.seconds), fontWeight = FontWeight.SemiBold, style = TabularNumbers, color = NextSetTheme.colors.accentText)
        Spacer(Modifier.width(12.dp))
        Text(
            text = preset.trimmedName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
            }
        }
    }
}

private val SoundStyle.title: Int
    get() = when (this) {
        SoundStyle.BEEP -> CoreR.string.sound_beep
        SoundStyle.CHIME -> CoreR.string.sound_chime
        SoundStyle.DIGITAL -> CoreR.string.sound_digital
    }
