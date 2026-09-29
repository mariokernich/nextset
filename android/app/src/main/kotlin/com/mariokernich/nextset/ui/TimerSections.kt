package com.mariokernich.nextset.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariokernich.nextset.R
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.model.RestPreset
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.model.TimerLibrary
import com.mariokernich.nextset.core.store.TimerSnapshot
import com.mariokernich.nextset.core.R as CoreR

/** The one or two big buttons that start a rest with a single tap. */
@Composable
fun QuickTimers(state: TimerSnapshot, onStart: (RestPreset) -> Unit, onEdit: (TimerEditorTarget) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        state.library.visibleQuickTimers.forEachIndexed { index, preset ->
            QuickTimerCard(
                preset = preset,
                index = index,
                isCurrent = state.timer.isActive && state.timer.presetId == preset.id,
                onStart = { onStart(preset) },
                onEdit = { onEdit(TimerEditorTarget.Quick(index)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickTimerCard(
    preset: RestPreset,
    index: Int,
    isCurrent: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val scheme = MaterialTheme.colorScheme
    val spoken = DurationFormat.spoken(LocalResources.current, preset.seconds)
    val startLabel = stringResource(CoreR.string.start_rest_for, spoken)
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(28.dp)
    // Only while running: an infinite transition asks for every frame, even when its value doesn't change.
    val pulse = if (isCurrent) pulseAlpha() else 1f

    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.accent.copy(alpha = if (isCurrent) 0.16f else 0.07f).compositeOver(scheme.surfaceContainerLow))
                .combinedClickable(onClick = onStart, onLongClick = { menu = true })
                .semantics {
                    contentDescription = listOf(startLabel, preset.trimmedName).filter { it.isNotEmpty() }.joinToString(", ")
                    selected = isCurrent
                }
                .padding(vertical = 16.dp, horizontal = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCurrent) Icons.Rounded.Timer else Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp).alpha(pulse),
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    text = preset.trimmedName.ifEmpty { stringResource(CoreR.string.quick_timer_n, index + 1) },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = DurationFormat.clock(preset.seconds),
                lineHeight = 46.sp,
                fontWeight = FontWeight.SemiBold,
                style = TabularNumbers,
                maxLines = 1,
                // "10:00" still fits with a large font or display size.
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 42.sp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(CoreR.string.edit)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    menu = false
                    onEdit()
                },
            )
        }
    }
}

@Composable
private fun pulseAlpha(): Float {
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )
    return alpha
}

/** Further presets, one tap away. */
@Composable
fun Presets(
    state: TimerSnapshot,
    onStart: (RestPreset) -> Unit,
    onEdit: (TimerEditorTarget) -> Unit,
    onDelete: (String) -> Unit,
) {
    val presets = state.library.presets
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(CoreR.string.more_timers),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(
                onClick = { onEdit(TimerEditorTarget.NewPreset) },
                enabled = presets.size < TimerLibrary.MAX_PRESETS,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(CoreR.string.add_timer))
            }
        }

        if (presets.isEmpty()) {
            Text(
                text = stringResource(R.string.presets_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Like an adaptive grid: as many columns of at least 76 dp as fit.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = maxOf(1, ((maxWidth + 10.dp) / (76.dp + 10.dp)).toInt())
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    presets.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { preset ->
                                PresetButton(
                                    preset = preset,
                                    isCurrent = state.timer.isActive && state.timer.presetId == preset.id,
                                    onStart = { onStart(preset) },
                                    onEdit = { onEdit(TimerEditorTarget.Preset(preset.id)) },
                                    onDelete = { onDelete(preset.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetButton(
    preset: RestPreset,
    isCurrent: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val scheme = MaterialTheme.colorScheme
    val startLabel = stringResource(CoreR.string.start_rest_for, DurationFormat.spoken(LocalResources.current, preset.seconds))
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Box(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(if (isCurrent) colors.accent.copy(alpha = 0.16f).compositeOver(scheme.surfaceContainerLow) else scheme.surfaceContainerLow.copy(alpha = 0.9f))
                .combinedClickable(onClick = onStart, onLongClick = { menu = true })
                .semantics {
                    // With the name, so two timers of 1:30 stay apart.
                    contentDescription = listOf(startLabel, preset.trimmedName).filter { it.isNotEmpty() }.joinToString(", ")
                    selected = isCurrent
                }
                .padding(vertical = 6.dp, horizontal = 4.dp),
        ) {
            Text(
                text = DurationFormat.clock(preset.seconds),
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                style = TabularNumbers,
                color = if (isCurrent) colors.accentText else scheme.onSurface,
            )
            if (preset.trimmedName.isNotEmpty()) {
                Text(
                    text = preset.trimmedName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(CoreR.string.edit)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    menu = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(CoreR.string.delete), color = scheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = scheme.error) },
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}
