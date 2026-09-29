package com.mariokernich.nextset.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PickerGroup
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.model.RestPreset
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.wear.R
import com.mariokernich.nextset.core.R as CoreR

/** Minutes and seconds (5 s steps) for one timer; the crown turns the selected wheel. */
@Composable
fun TimerEditorScreen(target: TimerEditorTarget, store: TimerStore, onDone: () -> Unit) {
    val existing = remember(target) { store.library.timer(target) }
    val initial = existing?.seconds ?: 60
    val minutes = rememberPickerState(initialNumberOfOptions = 61, initiallySelectedIndex = initial / 60, shouldRepeatOptions = false)
    val seconds = rememberPickerState(initialNumberOfOptions = 12, initiallySelectedIndex = (initial % 60) / 5)
    var selected by remember { mutableIntStateOf(0) }
    val total by remember { derivedStateOf { RestPreset.clamp(minutes.selectedOptionIndex * 60 + seconds.selectedOptionIndex * 5) } }
    val minutesLabel = stringResource(R.string.minutes)
    val secondsLabel = stringResource(R.string.seconds)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = DurationFormat.clock(total),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                style = TabularNumbers,
                color = WearColors.coral,
                modifier = Modifier.padding(top = 20.dp),
            )
            PickerGroup(
                selectedPickerState = if (selected == 0) minutes else seconds,
                modifier = Modifier.fillMaxWidth().height(110.dp),
                autoCenter = false,
            ) {
                PickerGroupItem(
                    pickerState = minutes,
                    selected = selected == 0,
                    onSelected = { selected = 0 },
                    modifier = Modifier.width(92.dp),
                    contentDescription = { "$minutesLabel ${minutes.selectedOptionIndex}" },
                ) { index, _ ->
                    Wheel(stringResource(CoreR.string.minutes_unit, index))
                }
                PickerGroupItem(
                    pickerState = seconds,
                    selected = selected == 1,
                    onSelected = { selected = 1 },
                    modifier = Modifier.width(76.dp),
                    contentDescription = { "$secondsLabel ${seconds.selectedOptionIndex * 5}" },
                ) { index, _ ->
                    Wheel(stringResource(CoreR.string.seconds_unit, index * 5))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                if (target is TimerEditorTarget.Preset) {
                    val deleteLabel = stringResource(CoreR.string.delete_timer)
                    FilledIconButton(
                        onClick = {
                            store.updateLibrary { it.deleting(target) }
                            onDone()
                        },
                        modifier = Modifier.size(44.dp).semantics { contentDescription = deleteLabel },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = WearColors.neutral, contentColor = MaterialTheme.colorScheme.error),
                    ) { Icon(Icons.Rounded.Delete, contentDescription = null) }
                }
                val saveLabel = stringResource(CoreR.string.save)
                FilledIconButton(
                    onClick = {
                        // The watch edits only the duration; a name set on the phone stays.
                        store.updateLibrary { it.saving(total, existing?.name.orEmpty(), target) }
                        onDone()
                    },
                    modifier = Modifier.size(44.dp).semantics { contentDescription = saveLabel },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = WearColors.coral, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) { Icon(Icons.Rounded.Check, contentDescription = null) }
            }
        }
    }
}

@Composable
private fun Wheel(text: String) {
    Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Medium, style = TabularNumbers)
}
