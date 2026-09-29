package com.mariokernich.nextset.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariokernich.nextset.R
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.model.TimerEditorTarget
import com.mariokernich.nextset.core.store.TimerStore
import kotlinx.coroutines.launch
import com.mariokernich.nextset.core.R as CoreR

/** Sets the duration and an optional name of a timer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerEditorSheet(target: TimerEditorTarget, store: TimerStore, onDismiss: () -> Unit) {
    val existing = remember(target) { store.library.timer(target) }
    var seconds by rememberSaveable { mutableIntStateOf(existing?.seconds ?: 60) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = NextSetTheme.colors

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    val title = when (target) {
        is TimerEditorTarget.Quick -> stringResource(CoreR.string.quick_timer_n, target.index + 1)
        is TimerEditorTarget.Preset -> stringResource(CoreR.string.edit_timer)
        TimerEditorTarget.NewPreset -> stringResource(CoreR.string.new_timer)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = ::close) { Text(stringResource(CoreR.string.cancel)) }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        store.updateLibrary { it.saving(seconds, name, target) }
                        close()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.coral, contentColor = MaterialTheme.colorScheme.onSecondary),
                ) { Text(stringResource(CoreR.string.save)) }
            }

            Text(
                text = DurationFormat.clock(seconds),
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                style = TabularNumbers,
                color = colors.accentText,
            )

            DurationPicker(seconds = seconds, onChange = { seconds = it })

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name_optional)) },
                supportingText = { Text(stringResource(R.string.name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            if (target is TimerEditorTarget.Preset) {
                TextButton(
                    onClick = {
                        store.updateLibrary { it.deleting(target) }
                        close()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(CoreR.string.delete_timer)) }
            }
        }
    }
}
