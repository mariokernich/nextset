package com.mariokernich.nextset.ui

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mariokernich.nextset.R
import com.mariokernich.nextset.core.model.RestPreset
import com.mariokernich.nextset.core.R as CoreR

/** Minutes and seconds wheels (5 s steps) bound to a number of seconds, like on iOS. */
@Composable
fun DurationPicker(seconds: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    val minuteLabels = remember(resources) { Array(61) { resources.getString(CoreR.string.minutes_unit, it) } }
    val secondLabels = remember(resources) { Array(12) { resources.getString(CoreR.string.seconds_unit, it * 5) } }
    val current by rememberUpdatedState(seconds)
    val change by rememberUpdatedState(onChange)
    val minutesLabel = stringResource(R.string.minutes)
    val secondsLabel = stringResource(R.string.seconds)
    // The wheels are platform views: give them the text colour of the Compose theme.
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val textSize = with(LocalDensity.current) { 22.sp.toPx() }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 0
                    maxValue = minuteLabels.lastIndex
                    displayedValues = minuteLabels
                    wrapSelectorWheel = false
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setOnValueChangedListener { _, _, minutes -> change(RestPreset.clamp(minutes * 60 + current % 60)) }
                }
            },
            update = {
                it.textColor = textColor
                it.textSize = textSize
                it.value = current / 60
            },
            modifier = Modifier.width(120.dp).semantics { contentDescription = minutesLabel },
        )
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = 0
                    maxValue = secondLabels.lastIndex
                    displayedValues = secondLabels
                    wrapSelectorWheel = false
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setOnValueChangedListener { picker, _, step ->
                        val clamped = RestPreset.clamp(current / 60 * 60 + step * 5)
                        change(clamped)
                        // 0:00 becomes 0:05 and 60:30 becomes 60:00. If that leaves the value
                        // as it was, nothing redraws the wheel, so put it right here.
                        picker.value = (clamped % 60) / 5
                    }
                }
            },
            update = {
                it.textColor = textColor
                it.textSize = textSize
                it.value = (current % 60) / 5
            },
            modifier = Modifier.width(120.dp).semantics { contentDescription = secondsLabel },
        )
    }
}
