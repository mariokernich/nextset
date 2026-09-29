package com.mariokernich.nextset.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.store.TimerSnapshot
import com.mariokernich.nextset.core.store.TimerStore
import com.mariokernich.nextset.core.timer.RestTimerState
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import com.mariokernich.nextset.core.ui.drawTimerRing
import kotlinx.coroutines.delay
import com.mariokernich.nextset.core.R as CoreR

/** Full-screen ring while resting. */
@Composable
fun TimerScreen(state: TimerSnapshot, store: TimerStore, isAmbient: Boolean, ambientUpdate: Long) {
    val timer = state.timer
    val now by rememberNow(timer, isAmbient, ambientUpdate)
    val density = LocalDensity.current
    val inCountdown = timer.phase == Phase.RUNNING && state.countdownSecondsLeft != null
    val tint = if (inCountdown) WearColors.coral else WearColors.accent

    // Full screen: no clock on top of the ring.
    ScreenScaffold(timeText = {}) { _ ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val diameter = minOf(maxWidth, maxHeight)
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .graphicsLayer { alpha = if (isAmbient || timer.phase == Phase.PAUSED) 0.5f else 1f },
            ) {
                drawTimerRing(
                    progress = if (timer.phase == Phase.FINISHED) 1f else timer.fractionRemaining(now),
                    tint = tint,
                    track = WearColors.track,
                    strokeWidth = maxOf(8.dp.toPx(), size.minDimension * 0.08f),
                    glow = !isAmbient,
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = diameter * 0.14f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        when (timer.phase) {
                            Phase.IDLE -> CoreR.string.caption_ready
                            Phase.RUNNING -> CoreR.string.caption_rest
                            Phase.PAUSED -> CoreR.string.caption_paused
                            Phase.FINISHED -> CoreR.string.caption_next_set
                        },
                    ).uppercase(),
                    fontSize = with(density) { maxOf(10.dp, diameter * 0.075f).toSp() },
                    fontWeight = FontWeight.Bold,
                    color = if (timer.phase == Phase.FINISHED) WearColors.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                val timeSize = with(density) { (diameter * 0.27f).toSp() }
                Text(
                    text = if (timer.phase == Phase.FINISHED) stringResource(CoreR.string.go) else DurationFormat.clock(timer.displayedSeconds(now)),
                    fontSize = timeSize,
                    lineHeight = timeSize,
                    fontWeight = FontWeight.SemiBold,
                    style = TabularNumbers,
                    color = when {
                        timer.phase == Phase.FINISHED -> WearColors.accent
                        inCountdown && !isAmbient -> WearColors.coral
                        else -> WearColors.accent
                    },
                    maxLines = 1,
                )
                // Not in Always On: once the rest's service ends, the watch sleeps and it would stand still.
                if (timer.phase == Phase.FINISHED && !isAmbient) {
                    Text(
                        text = "+" + DurationFormat.clock((timer.overtime(now) / 1000).toInt()),
                        fontSize = with(density) { maxOf(11.dp, diameter * 0.09f).toSp() },
                        fontWeight = FontWeight.SemiBold,
                        style = TabularNumbers,
                        color = WearColors.coral,
                    )
                }
            }

            if (!isAmbient) {
                NeutralButton(
                    onClick = store::stop,
                    label = stringResource(CoreR.string.end_rest),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = diameter * 0.13f),
                ) { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = diameter * 0.12f),
                ) {
                    if (timer.phase == Phase.FINISHED) {
                        PrimaryButton(onClick = store::primaryAction, label = stringResource(CoreR.string.repeat_rest)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                        }
                    } else {
                        AdjustButton(-TimerStore.ADJUST_STEP, stringResource(CoreR.string.remove_15_seconds), store::adjust)
                        PrimaryButton(
                            onClick = store::primaryAction,
                            label = stringResource(if (timer.phase == Phase.RUNNING) CoreR.string.pause else CoreR.string.resume),
                        ) {
                            Icon(if (timer.phase == Phase.RUNNING) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                        }
                        AdjustButton(TimerStore.ADJUST_STEP, stringResource(CoreR.string.add_15_seconds), store::adjust)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(onClick: () -> Unit, label: String, content: @Composable () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics { contentDescription = label },
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = WearColors.accent, contentColor = WearColors.onAccent),
    ) { content() }
}

@Composable
private fun NeutralButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(34.dp).semantics { contentDescription = label },
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = WearColors.neutral, contentColor = WearColors.accent),
    ) { content() }
}

@Composable
private fun AdjustButton(delta: Int, label: String, onAdjust: (Int) -> Unit) {
    TextButton(
        onClick = { onAdjust(delta) },
        modifier = Modifier.size(40.dp).semantics { contentDescription = label },
        colors = TextButtonDefaults.filledTextButtonColors(containerColor = WearColors.neutral, contentColor = WearColors.accent),
    ) {
        Text(if (delta > 0) "+$delta" else "−${-delta}", fontWeight = FontWeight.SemiBold, style = TabularNumbers)
    }
}

/**
 * The current time: every frame while a rest runs on screen, otherwise just
 * when the shown seconds change, in step with the countdown vibrations.
 */
@Composable
private fun rememberNow(timer: RestTimerState, isAmbient: Boolean, ambientUpdate: Long): State<Long> {
    // Keyed, so a changed rest never shows for a frame with the time of before.
    val now = remember(timer, isAmbient, ambientUpdate) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer, isAmbient, ambientUpdate) {
        if (timer.phase == Phase.RUNNING && !isAmbient) {
            while (true) withFrameMillis { now.longValue = System.currentTimeMillis() }
        } else if (timer.phase == Phase.RUNNING || timer.phase == Phase.FINISHED) {
            while (true) {
                delay(timer.millisUntilNextSecond(System.currentTimeMillis()))
                now.longValue = System.currentTimeMillis()
            }
        }
    }
    return now
}
