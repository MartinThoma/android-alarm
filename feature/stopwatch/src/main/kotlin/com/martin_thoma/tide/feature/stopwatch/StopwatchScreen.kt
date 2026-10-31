package com.martin_thoma.tide.feature.stopwatch

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.tide.core.designsystem.GlassPane
import com.martin_thoma.tide.core.designsystem.LocalTideColors
import com.martin_thoma.tide.core.designsystem.TideAware
import com.martin_thoma.tide.core.designsystem.TideIcons
import com.martin_thoma.tide.core.designsystem.TideSurface
import com.martin_thoma.tide.core.designsystem.TideTheme
import com.martin_thoma.tide.core.designsystem.formatDuration
import com.martin_thoma.tide.core.model.Stopwatch
import com.martin_thoma.tide.core.model.fastestAndSlowest
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data object StopwatchRoute : NavKey

/** The water stands at mid tide, so the time sits on the sea and the laps on the sand. */
private const val STOPWATCH_TIDE = 0.36f

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    val viewModel: StopwatchViewModel = hiltViewModel()
    val watch by viewModel.stopwatch.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(viewModel.now()) }
    // Hundredths change every frame, but only while the screen shows a running stopwatch.
    LaunchedEffect(watch.isRunning) {
        now = viewModel.now()
        while (watch.isRunning) withInfiniteAnimationFrameMillis { now = viewModel.now() }
    }
    StopwatchContent(
        watch = watch,
        now = now,
        onStartOrPause = viewModel::startOrPause,
        onLap = viewModel::lap,
        onReset = viewModel::reset,
        modifier = modifier,
    )
}

@Composable
internal fun StopwatchContent(
    watch: Stopwatch,
    now: Long,
    onStartOrPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val elapsed = watch.elapsed(now)
    TideSurface(level = STOPWATCH_TIDE, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            ElapsedTime(elapsed, locale)
            Spacer(Modifier.height(40.dp))
            Controls(watch, onStartOrPause, onLap, onReset)
            Spacer(Modifier.height(32.dp))
            Laps(watch, elapsed, locale)
        }
    }
}

/** Minutes and seconds large; hundredths smaller, so the eye rests on what changes slowly. */
@Composable
private fun ElapsedTime(
    elapsed: Long,
    locale: Locale,
) {
    val whole = formatDuration(elapsed, locale)
    val fraction = formatDuration(elapsed, locale, hundredths = true).removePrefix(whole)
    TideAware {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(whole, style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, lineHeight = 96.sp))
            Text(
                text = fraction,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun Controls(
    watch: Stopwatch,
    onStartOrPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
) {
    val colors = LocalTideColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
        // The side buttons keep their places even when hidden, so the main button never moves.
        SideButton(
            visible = !watch.isRunning && !watch.isReset,
            icon = TideIcons.Replay,
            label = R.string.feature_stopwatch_reset,
            onClick = onReset,
        )
        FilledIconButton(
            onClick = onStartOrPause,
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.accent, contentColor = colors.onAccent),
        ) {
            val label =
                when {
                    watch.isRunning -> R.string.feature_stopwatch_pause
                    watch.isReset -> R.string.feature_stopwatch_start
                    else -> R.string.feature_stopwatch_resume
                }
            Icon(
                painter = painterResource(if (watch.isRunning) TideIcons.Pause else TideIcons.Play),
                contentDescription = stringResource(label),
                modifier = Modifier.size(40.dp),
            )
        }
        SideButton(visible = watch.isRunning, icon = TideIcons.Flag, label = R.string.feature_stopwatch_lap, onClick = onLap)
    }
}

@Composable
private fun SideButton(
    visible: Boolean,
    icon: Int,
    label: Int,
    onClick: () -> Unit,
) {
    if (!visible) {
        Spacer(Modifier.size(64.dp))
        return
    }
    GlassPane(modifier = Modifier.size(64.dp), shape = CircleShape, onClick = onClick) {
        Icon(painterResource(icon), contentDescription = stringResource(label), modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun Laps(
    watch: Stopwatch,
    elapsed: Long,
    locale: Locale,
) {
    val laps = watch.laps()
    if (laps.isEmpty()) return
    val extremes = fastestAndSlowest(laps)
    // The lap in progress first, then the finished ones, newest first.
    val current = Stopwatch.Lap(number = laps.size + 1, millis = elapsed - laps.last().total, total = elapsed)
    GlassPane(Modifier.fillMaxWidth()) {
        LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
            items(listOf(current) + laps.reversed(), key = { it.number }) { lap ->
                if (lap.number != current.number) {
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = LocalContentColor.current.copy(alpha = 0.12f))
                }
                LapRow(
                    lap = lap,
                    locale = locale,
                    badge =
                        when (lap) {
                            extremes?.first -> R.string.feature_stopwatch_fastest
                            extremes?.second -> R.string.feature_stopwatch_slowest
                            else -> null
                        },
                )
            }
        }
    }
}

@Composable
private fun LapRow(
    lap: Stopwatch.Lap,
    locale: Locale,
    badge: Int?,
) {
    val colors = LocalTideColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.feature_stopwatch_lap_number, lap.number), style = MaterialTheme.typography.bodyLarge)
            // Named in words as well as colour, so it never depends on colour alone.
            if (badge != null) {
                Text(
                    stringResource(badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (badge == R.string.feature_stopwatch_fastest) colors.accent else colors.onGlassVariant,
                )
            }
        }
        Text(formatDuration(lap.millis, locale, hundredths = true), style = MaterialTheme.typography.titleMedium)
        Text(
            text = formatDuration(lap.total, locale, hundredths = true),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onGlassVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp).widthIn(min = 88.dp),
        )
    }
}

@Preview(widthDp = 393, heightDp = 851)
@Composable
private fun StopwatchPreview() {
    TideTheme(darkTheme = false) {
        StopwatchContent(
            watch =
                Stopwatch()
                    .start(0)
                    .lap(61_230)
                    .lap(118_900)
                    .lap(185_410),
            now = 201_270,
            onStartOrPause = {},
            onLap = {},
            onReset = {},
        )
    }
}
