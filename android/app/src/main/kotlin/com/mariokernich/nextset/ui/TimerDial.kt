package com.mariokernich.nextset.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariokernich.nextset.core.format.DurationFormat
import com.mariokernich.nextset.core.store.TimerSnapshot
import com.mariokernich.nextset.core.timer.RestTimerState.Phase
import com.mariokernich.nextset.core.ui.drawTimerRing
import java.util.Date
import kotlin.math.pow
import kotlinx.coroutines.delay
import com.mariokernich.nextset.core.R as CoreR

/** The big countdown ring. */
@Composable
fun TimerDial(state: TimerSnapshot, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val timer = state.timer
    val now by rememberNow(timer.phase)
    val colors = NextSetTheme.colors
    val scheme = MaterialTheme.colorScheme
    val resources = LocalResources.current
    val density = LocalDensity.current

    val seconds = timer.displayedSeconds(now)
    val inCountdown = timer.phase == Phase.RUNNING && seconds in 1..state.settings.countdownSeconds
    val tint = if (inCountdown) colors.coral else colors.accent
    // A short "beat" on every countdown second.
    val beat = if (inCountdown) 1f + 0.07f * ((timer.remaining(now) % 1000) / 1000f).pow(4) else 1f

    val caption = stringResource(
        when (timer.phase) {
            Phase.IDLE -> CoreR.string.caption_ready
            Phase.RUNNING -> CoreR.string.caption_rest
            Phase.PAUSED -> CoreR.string.caption_paused
            Phase.FINISHED -> CoreR.string.caption_next_set
        },
    )
    val spoken = when (timer.phase) {
        Phase.IDLE -> DurationFormat.spoken(resources, state.idleSeconds)
        Phase.RUNNING, Phase.PAUSED -> DurationFormat.spoken(resources, seconds)
        Phase.FINISHED -> stringResource(CoreR.string.rest_is_over)
    }

    Box(modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(scheme.surfaceContainerLowest.copy(alpha = 0.55f))
                .clearAndSetSemantics { contentDescription = "$caption, $spoken" },
        ) {
            val diameter = minOf(maxWidth, maxHeight)
            val lineWidth = maxOf(10.dp, diameter * 0.05f)
            val ringOpacity = when (timer.phase) {
                Phase.IDLE -> 0.6f
                Phase.PAUSED -> 0.45f
                Phase.RUNNING, Phase.FINISHED -> 1f
            }
            Canvas(Modifier.fillMaxSize().padding(lineWidth * 0.6f).graphicsLayer { alpha = ringOpacity }) {
                drawTimerRing(
                    progress = if (timer.phase == Phase.FINISHED) 1f else timer.fractionRemaining(now),
                    tint = tint,
                    track = colors.track,
                    strokeWidth = lineWidth.toPx(),
                )
            }

            val mainSize = with(density) { (diameter * 0.24f).toSp() }
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = diameter * 0.12f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(diameter * 0.015f),
            ) {
                Text(
                    text = caption.uppercase(),
                    fontSize = with(density) { maxOf(11.dp, diameter * 0.045f).toSp() },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = if (timer.phase == Phase.FINISHED) colors.accentText else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = when (timer.phase) {
                        Phase.FINISHED -> stringResource(CoreR.string.go)
                        Phase.IDLE -> DurationFormat.clock(state.idleSeconds)
                        Phase.RUNNING, Phase.PAUSED -> DurationFormat.clock(seconds)
                    },
                    fontSize = mainSize,
                    lineHeight = mainSize,
                    fontWeight = FontWeight.SemiBold,
                    style = TabularNumbers,
                    color = when {
                        timer.phase == Phase.FINISHED -> colors.accent
                        inCountdown -> colors.coral
                        else -> scheme.onSurface
                    },
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        scaleX = beat
                        scaleY = beat
                    },
                )
                Text(
                    text = detail(state, now),
                    fontSize = with(density) { maxOf(12.dp, diameter * 0.05f).toSp() },
                    fontWeight = FontWeight.Medium,
                    style = TabularNumbers,
                    textAlign = TextAlign.Center,
                    color = if (timer.phase == Phase.FINISHED) colors.coral else scheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }

        if (timer.phase != Phase.IDLE) {
            FilledTonalIconButton(onClick = onStop, modifier = Modifier.align(Alignment.TopEnd).size(52.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(CoreR.string.end_rest))
            }
        }
    }
}

@Composable
private fun detail(state: TimerSnapshot, now: Long): String {
    val timer = state.timer
    return when (timer.phase) {
        Phase.IDLE -> stringResource(CoreR.string.tap_timer_to_start)
        Phase.RUNNING -> {
            val end = timer.endAt ?: now
            stringResource(CoreR.string.next_set_at, DateFormat.getTimeFormat(LocalContext.current).format(Date(end)))
        }
        Phase.PAUSED -> stringResource(CoreR.string.of_duration, DurationFormat.clock((timer.durationMs / 1000).toInt()))
        Phase.FINISHED -> "+" + DurationFormat.clock((timer.overtime(now) / 1000).toInt())
    }
}

/** The current time: every frame while a rest runs, every second while showing the overtime. */
@Composable
private fun rememberNow(phase: Phase): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(phase) {
        now.longValue = System.currentTimeMillis()
        when (phase) {
            Phase.RUNNING -> while (true) {
                withFrameMillis { now.longValue = System.currentTimeMillis() }
            }
            Phase.FINISHED -> while (true) {
                delay(1000 - System.currentTimeMillis() % 1000)
                now.longValue = System.currentTimeMillis()
            }
            Phase.IDLE, Phase.PAUSED -> Unit
        }
    }
    return now
}
