package com.martin_thoma.tide.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.martin_thoma.tide.core.model.Timer
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data object TimerRoute : NavKey

/**
 * Running and paused timers, each on its own glass pane. The tide is the progress of the timer
 * that ends next: it comes in as that timer runs out (requirements TM-08). With no timers, the
 * number pad shows straight away.
 */
@Composable
fun TimerScreen(
    onNewTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TimerViewModel = hiltViewModel()
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val current = timers ?: return
    if (current.isEmpty()) {
        NewTimerContent(onStart = viewModel::create, onBack = null, modifier = modifier)
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
    TimerListContent(
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
internal fun TimerListContent(
    timers: List<Timer>,
    now: Long,
    onNewTimer: () -> Unit,
    onStartOrPause: (Timer) -> Unit,
    onAddMinute: (Timer) -> Unit,
    onReset: (Timer) -> Unit,
    onDelete: (Timer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = timers.filter { it.isRunning }.minByOrNull { it.remaining(now) }
    val level = next?.let { TideLevel.forProgress(it.progress(now)) } ?: TideLevel.LOW
    val colors = LocalTideColors.current
    TideSurface(level = level, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNewTimer,
                    shape = CircleShape,
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                ) {
                    Icon(painterResource(TideIcons.Add), contentDescription = stringResource(R.string.feature_timer_new))
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(timers, key = { it.id }) { timer ->
                    TimerCard(timer, now, onStartOrPause, onAddMinute, onReset, onDelete, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: Timer,
    now: Long,
    onStartOrPause: (Timer) -> Unit,
    onAddMinute: (Timer) -> Unit,
    onReset: (Timer) -> Unit,
    onDelete: (Timer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val locale = LocalConfiguration.current.locales[0]
    val remaining = timer.remaining(now)
    val expired = timer.hasExpired(now)
    GlassPane(modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(timer.label.ifEmpty { stringResource(R.string.feature_timer_title) }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text =
                            if (expired) {
                                stringResource(R.string.feature_timer_times_up)
                            } else {
                                stringResource(R.string.feature_timer_of, formatDuration(timer.durationMillis, locale))
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (expired) colors.accent else colors.onGlassVariant,
                    )
                }
                IconButton(onClick = { onDelete(timer) }) {
                    Icon(
                        painterResource(TideIcons.Delete),
                        contentDescription = stringResource(R.string.feature_timer_delete),
                        tint = colors.onGlassVariant,
                    )
                }
            }
            // Overtime counts up with a plus, so a run-out timer reads differently even without colour.
            Text(
                text = if (expired) "+" + formatDuration(-remaining, locale) else formatDuration(remaining + 999, locale),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 56.sp),
                color = if (expired) colors.accent else LocalContentColor.current,
                modifier = Modifier.padding(top = 4.dp),
            )
            LinearProgressIndicator(
                progress = { timer.progress(now) },
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp, top = 8.dp, bottom = 12.dp).height(6.dp),
                color = colors.accent,
                trackColor = colors.onGlass.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { if (expired) onReset(timer) else onStartOrPause(timer) },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                ) {
                    val (icon, label) =
                        when {
                            expired -> TideIcons.Replay to R.string.feature_timer_stop
                            timer.isRunning -> TideIcons.Pause to R.string.feature_timer_pause
                            timer.isIdle -> TideIcons.Play to R.string.feature_timer_start
                            else -> TideIcons.Play to R.string.feature_timer_resume
                        }
                    Icon(painterResource(icon), contentDescription = stringResource(label))
                }
                GlassPane(shape = CircleShape, onClick = { onAddMinute(timer) }) {
                    Text(
                        stringResource(R.string.feature_timer_add_minute),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
                if (!timer.isIdle && !expired) {
                    IconButton(onClick = { onReset(timer) }) {
                        Icon(painterResource(TideIcons.Replay), contentDescription = stringResource(R.string.feature_timer_reset))
                    }
                }
            }
        }
    }
}
