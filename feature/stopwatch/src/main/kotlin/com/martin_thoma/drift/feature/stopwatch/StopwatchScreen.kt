package com.martin_thoma.drift.feature.stopwatch

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.drift.core.designsystem.GlassPane
import com.martin_thoma.drift.core.designsystem.LocalTideColors
import com.martin_thoma.drift.core.designsystem.TideAware
import com.martin_thoma.drift.core.designsystem.TideIcons
import com.martin_thoma.drift.core.designsystem.TideLevel
import com.martin_thoma.drift.core.designsystem.TideSurface
import com.martin_thoma.drift.core.designsystem.TideTheme
import com.martin_thoma.drift.core.designsystem.formatDuration
import com.martin_thoma.drift.core.designsystem.keepOnOpenWater
import com.martin_thoma.drift.core.model.Stopwatch
import com.martin_thoma.drift.core.model.fastestAndSlowest
import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Serializable
data object StopwatchRoute : NavKey

private val DIAL_SIZE = 312.dp
private val GLASS_SIZE = 248.dp

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
    // The tide only sets the scene here: just high enough that the dial floats on open water.
    var level by remember { mutableFloatStateOf(TideLevel.LOW) }
    TideSurface(level = level, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Dial(watch, elapsed, locale, Modifier.keepOnOpenWater { level = it })
            Spacer(Modifier.height(28.dp))
            Controls(watch, onStartOrPause, onLap, onReset)
            Spacer(Modifier.height(24.dp))
            Laps(watch, elapsed, locale, Modifier.weight(1f))
        }
    }
}

/**
 * A stopwatch dial: the time on a disc of glass, and around it, out on the water, sixty ticks
 * that glow like the shore. A point of light sweeps round once a minute and leaves a faint arc;
 * every lap leaves a small mark.
 */
@Composable
private fun Dial(
    watch: Stopwatch,
    elapsed: Long,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val laps = watch.laps()
    // White foam by day, the bioluminescent glow by night.
    val light = if (colors.glow == Color.Transparent) colors.foam else colors.glow
    Box(modifier.size(DIAL_SIZE), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val outer = size.minDimension / 2 - 4.dp.toPx()
            val track = outer - 16.dp.toPx()

            fun point(
                fraction: Float,
                distance: Float,
            ): Offset {
                val angle = 2 * PI * fraction - PI / 2
                return center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * distance
            }
            for (tick in 0 until 60) {
                val major = tick % 5 == 0
                val start = point(tick / 60f, outer - (if (major) 12.dp else 6.dp).toPx())
                val end = point(tick / 60f, outer)
                // A soft halo under each tick, like the light along the shore.
                drawLine(light.copy(alpha = 0.18f), start, end, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(
                    light.copy(alpha = if (major) 0.95f else 0.6f),
                    start,
                    end,
                    strokeWidth = (if (major) 2.dp else 1.2.dp).toPx(),
                    cap = StrokeCap.Round,
                )
            }
            val minute = (elapsed % 60_000) / 60_000f
            val arcTopLeft = center - Offset(track, track)
            val arcSize = Size(track * 2, track * 2)
            drawArc(
                light.copy(alpha = 0.45f),
                -90f,
                360f * minute,
                false,
                arcTopLeft,
                arcSize,
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
            )
            // Laps: where in its minute each one ended.
            laps.forEach { lap -> drawCircle(light.copy(alpha = 0.8f), 2.5.dp.toPx(), point((lap.total % 60_000) / 60_000f, track)) }
            if (!watch.isReset) {
                val head = point(minute, track)
                drawCircle(light.copy(alpha = 0.25f), 11.dp.toPx(), head)
                drawCircle(light, 5.dp.toPx(), head)
            }
        }
        GlassPane(modifier = Modifier.size(GLASS_SIZE), shape = CircleShape) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                ElapsedTime(elapsed, locale)
                if (laps.isNotEmpty()) {
                    Text(
                        stringResource(R.string.feature_stopwatch_lap_number, laps.size + 1),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onGlassVariant,
                    )
                }
            }
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
    // With hours the time is longer; it stays on one line inside the dial.
    val size = if (whole.length > 5) 38.sp else 52.sp
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            whole,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = size, lineHeight = size * 1.1f),
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = fraction,
            style = MaterialTheme.typography.headlineMedium,
            color = LocalTideColors.current.onGlassVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
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
            modifier = Modifier.size(96.dp),
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
        Spacer(Modifier.size(72.dp))
        return
    }
    GlassPane(modifier = Modifier.size(72.dp), shape = CircleShape, onClick = onClick) {
        Icon(painterResource(icon), contentDescription = stringResource(label), modifier = Modifier.align(Alignment.Center))
    }
}

/** Laps as small cards of glass, the one in progress first, then the finished ones, newest first. */
@Composable
private fun Laps(
    watch: Stopwatch,
    elapsed: Long,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val laps = watch.laps()
    if (laps.isEmpty()) return
    val extremes = fastestAndSlowest(laps)
    val current = Stopwatch.Lap(number = laps.size + 1, millis = elapsed - laps.last().total, total = elapsed)
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf(current) + laps.reversed(), key = { it.number }) { lap ->
            GlassPane(Modifier.fillMaxWidth().animateItem(), shape = RoundedCornerShape(20.dp)) {
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
