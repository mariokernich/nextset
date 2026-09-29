package com.mariokernich.nextset.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.model.RestPreset
import com.mariokernich.nextset.core.store.TimerSnapshot
import com.mariokernich.nextset.wear.R
import com.mariokernich.nextset.core.R as CoreR

/** Big quick timers first, further presets below. */
@Composable
fun HomeScreen(state: TimerSnapshot, onStart: (RestPreset) -> Unit, onOpenSettings: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val resources = LocalResources.current

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader {
                    Text(stringResource(R.string.app_name), color = WearColors.coral, fontWeight = FontWeight.Bold)
                }
            }
            state.library.visibleQuickTimers.forEach { preset ->
                item(key = preset.id) {
                    val label = stringResource(CoreR.string.start_rest_for, DurationFormat.spoken(resources, preset.seconds))
                    Button(
                        onClick = { onStart(preset) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).semantics { contentDescription = label },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WearColors.quickTimer, contentColor = WearColors.accent),
                    ) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = DurationFormat.clock(preset.seconds),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = TabularNumbers,
                                maxLines = 1,
                            )
                            if (preset.trimmedName.isNotEmpty()) {
                                Text(
                                    text = preset.trimmedName,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            state.library.presets.chunked(2).forEach { row ->
                item(key = row.first().id) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { preset ->
                            val label = stringResource(CoreR.string.start_rest_for, DurationFormat.spoken(resources, preset.seconds))
                            Button(
                                onClick = { onStart(preset) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = label },
                                colors = ButtonDefaults.buttonColors(containerColor = WearColors.preset, contentColor = WearColors.accent),
                            ) {
                                Text(
                                    text = DurationFormat.clock(preset.seconds),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = TabularNumbers,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (row.size == 1) Column(Modifier.weight(1f)) {}
                    }
                }
            }
            item {
                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text(stringResource(CoreR.string.settings)) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = WearColors.preset),
                )
            }
        }
    }
}
