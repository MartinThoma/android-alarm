package com.martin_thoma.tide.feature.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.tide.core.designsystem.GlassPane
import com.martin_thoma.tide.core.designsystem.LocalTideColors
import com.martin_thoma.tide.core.designsystem.TideIcons
import com.martin_thoma.tide.core.designsystem.TideLevel
import com.martin_thoma.tide.core.designsystem.TideSurface
import com.martin_thoma.tide.core.designsystem.formatDuration
import com.martin_thoma.tide.core.designsystem.rememberTimeFormatter
import com.martin_thoma.tide.core.model.Timer
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable
data object TimerRoute : NavKey

private val RING_SIZE = 296.dp

/**
 * One timer at a time, full screen (requirements TM-08): the time left inside a ring that empties
 * as it runs, the tide rising behind it. Several timers: swipe between them. With no timers, the
 * number pad shows straight away.
 */
@Composable
fun TimerScreen(
    onNewTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TimerViewModel = hiltViewModel()
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val current = timers ?: return
    if (current.isEmpty()) {
        NewTimerContent(recents = recents, onStart = viewModel::create, onBack = null, modifier = modifier)
        return
    }
    var now by remember { mutableLongStateOf(viewModel.now()) }
    val anyRunning = current.any { it.isRunning }
    // Seconds are enough here; the screen only ticks while it is shown and a timer runs.
    LaunchedEffect(anyRunning) {
        now = viewModel.now()
        while (anyRunning) {
            delay(1_000 - now % 1_000)
            now = viewModel.now()
        }
    }
    TimerPagesContent(
        timers = current,
        now = now,
        onNewTimer = onNewTimer,
        onStartOrPause = viewModel::startOrPause,
        onAddMinute = viewModel::addMinute,
        onReset = viewModel::reset,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@Composable
internal fun TimerPagesContent(
    timers: List<Timer>,
    now: Long,
    onNewTimer: () -> Unit,
    onStartOrPause: (Timer) -> Unit,
    onAddMinute: (Timer) -> Unit,
    onReset: (Timer) -> Unit,
    onDelete: (Timer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pager = rememberPagerState { timers.size }
    // A timer just added is the one to see.
    var knownCount by remember { mutableIntStateOf(timers.size) }
    LaunchedEffect(timers.size) {
        if (timers.size > knownCount) pager.animateScrollToPage(timers.lastIndex)
        knownCount = timers.size
    }
    val shown = timers.getOrNull(pager.currentPage) ?: timers.last()
    // The tide follows the timer on screen: it comes in as that timer runs out.
    val level = if (shown.isIdle) TideLevel.LOW else TideLevel.forProgress(shown.progress(now))
    val colors = LocalTideColors.current

    TideSurface(level = level, modifier = modifier) {
        Column(
            modifier =
                Modifier.fillMaxSize().windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDelete(shown) }) {
                    Icon(
                        painterResource(TideIcons.Delete),
                        contentDescription = stringResource(R.string.feature_timer_delete),
                        tint = colors.onSea,
                    )
                }
                PageDots(count = timers.size, current = pager.currentPage, modifier = Modifier.weight(1f))
                IconButton(onClick = onNewTimer) {
                    Icon(
                        painterResource(TideIcons.Add),
                        contentDescription = stringResource(R.string.feature_timer_new),
                        tint = colors.onSea,
                    )
                }
            }
            HorizontalPager(state = pager, key = { timers[it].id }, modifier = Modifier.weight(1f)) { page ->
                TimerPage(timers[page], now, onStartOrPause, onAddMinute, onReset)
            }
        }
    }
}

@Composable
private fun PageDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    if (count < 2) {
        Spacer(modifier)
        return
    }
    val colors = LocalTideColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(
                        if (index ==
                            current
                        ) {
                            8.dp
                        } else {
                            6.dp
                        },
                    ).background(colors.onSea.copy(alpha = if (index == current) 0.9f else 0.35f), CircleShape),
            )
        }
    }
}

@Composable
private fun TimerPage(
    timer: Timer,
    now: Long,
    onStartOrPause: (Timer) -> Unit,
    onAddMinute: (Timer) -> Unit,
    onReset: (Timer) -> Unit,
) {
    val colors = LocalTideColors.current
    val expired = timer.hasExpired(now)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.6f))
        TimerRing(timer, now)
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassPane(modifier = Modifier.size(72.dp), shape = CircleShape, onClick = { onAddMinute(timer) }) {
                Text(
                    stringResource(R.string.feature_timer_add_minute),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            FilledIconButton(
                onClick = { if (expired) onReset(timer) else onStartOrPause(timer) },
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.accent, contentColor = colors.onAccent),
            ) {
                val (icon, label) =
                    when {
                        expired -> TideIcons.Stop to R.string.feature_timer_stop
                        timer.isRunning -> TideIcons.Pause to R.string.feature_timer_pause
                        timer.isIdle -> TideIcons.Play to R.string.feature_timer_start
                        else -> TideIcons.Play to R.string.feature_timer_resume
                    }
                Icon(painterResource(icon), contentDescription = stringResource(label), modifier = Modifier.size(40.dp))
            }
            // Keeps its place while hidden, so the main button never moves.
            if (!timer.isIdle && !expired) {
                GlassPane(modifier = Modifier.size(72.dp), shape = CircleShape, onClick = { onReset(timer) }) {
                    Icon(
                        painterResource(TideIcons.Replay),
                        contentDescription = stringResource(R.string.feature_timer_reset),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            } else {
                Spacer(Modifier.size(72.dp))
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

/**
 * A pane of glass with the time left, the label and when it rings, inside a ring that empties
 * as the time runs out. Run out, the ring is gone and the overtime counts up with a plus.
 */
@Composable
private fun TimerRing(
    timer: Timer,
    now: Long,
) {
    val colors = LocalTideColors.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = rememberTimeFormatter()
    val remaining = timer.remaining(now)
    val expired = timer.hasExpired(now)
    val left = 1f - timer.progress(now)
    GlassPane(modifier = Modifier.size(RING_SIZE), shape = CircleShape) {
        Canvas(Modifier.matchParentSize().padding(14.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(colors.onGlass.copy(alpha = 0.1f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (left > 0f) {
                // Clockwise from the top, shrinking towards it as time runs out.
                drawArc(
                    colors.accent,
                    -90f + 360f * (1f - left),
                    360f * left,
                    false,
                    topLeft,
                    arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                timer.label.ifEmpty { stringResource(R.string.feature_timer_title) },
                style = MaterialTheme.typography.titleMedium,
                color = colors.onGlassVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            val time = if (expired) "+" + formatDuration(-remaining, locale) else formatDuration(remaining + 999, locale)
            Text(
                text = time,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = if (time.length > 6) 48.sp else 64.sp),
                color = if (expired) colors.accent else colors.onGlass,
                maxLines = 1,
                softWrap = false,
            )
            when {
                expired -> {
                    StatusLine(stringResource(R.string.feature_timer_times_up), colors.accent)
                }

                timer.isRunning -> {
                    val endsAt = timeFormatter.format(Instant.ofEpochMilli(checkNotNull(timer.endsAt)).atZone(ZoneId.systemDefault()))
                    val description = stringResource(R.string.feature_timer_rings_at, endsAt)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
                    ) {
                        Icon(
                            painterResource(TideIcons.Alarm),
                            contentDescription = null,
                            tint = colors.onGlassVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(endsAt, style = MaterialTheme.typography.titleMedium, color = colors.onGlassVariant)
                    }
                }

                timer.isIdle -> {
                    // A reset timer shows its duration and needs no status line.
                }

                else -> {
                    StatusLine(stringResource(R.string.feature_timer_paused_short), colors.onGlassVariant)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(
    text: String,
    color: Color,
) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = color)
}
