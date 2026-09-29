package com.mariokernich.nextset.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import com.mariokernich.nextset.core.R as CoreR

/** −15 s · start/pause · +15 s */
@Composable
fun ControlBar(phase: Phase, onAdjust: (Int) -> Unit, onPrimary: () -> Unit) {
    val colors = NextSetTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        AdjustButton(-TimerStore.ADJUST_STEP, onAdjust)

        FilledIconButton(
            onClick = onPrimary,
            modifier = Modifier.size(76.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.accentStrong,
                contentColor = colors.onAccentStrong,
            ),
        ) {
            Crossfade(targetState = phase, label = "primary") { current ->
                Icon(
                    imageVector = when (current) {
                        Phase.RUNNING -> Icons.Rounded.Pause
                        Phase.IDLE, Phase.PAUSED -> Icons.Rounded.PlayArrow
                        Phase.FINISHED -> Icons.Rounded.Refresh
                    },
                    contentDescription = stringResource(
                        when (current) {
                            Phase.RUNNING -> CoreR.string.pause
                            Phase.PAUSED -> CoreR.string.resume
                            Phase.IDLE -> CoreR.string.start_rest
                            Phase.FINISHED -> CoreR.string.repeat_rest
                        },
                    ),
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        AdjustButton(TimerStore.ADJUST_STEP, onAdjust)
    }
}

@Composable
private fun AdjustButton(delta: Int, onAdjust: (Int) -> Unit) {
    val label = stringResource(if (delta > 0) CoreR.string.add_15_seconds else CoreR.string.remove_15_seconds)
    Surface(
        onClick = { onAdjust(delta) },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(58.dp).semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (delta > 0) "+$delta" else "−${-delta}",
                fontWeight = FontWeight.Medium,
                style = TabularNumbers,
                maxLines = 1,
                // One line in the round button, also with the largest font size.
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 19.sp),
            )
        }
    }
}
