package com.martin_thoma.tide.feature.ringing

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin_thoma.tide.core.designsystem.GlassPane
import com.martin_thoma.tide.core.designsystem.LocalTideColors
import com.martin_thoma.tide.core.designsystem.TideAware
import com.martin_thoma.tide.core.designsystem.TideIcons
import com.martin_thoma.tide.core.designsystem.TideLevel
import com.martin_thoma.tide.core.designsystem.TideSurface
import com.martin_thoma.tide.core.designsystem.TideTheme
import com.martin_thoma.tide.core.designsystem.formatDuration
import com.martin_thoma.tide.core.designsystem.rememberDateFormatter
import com.martin_thoma.tide.core.designsystem.rememberTimeFormatter
import com.martin_thoma.tide.core.designsystem.tideVariantColor
import com.martin_thoma.tide.core.model.Alarm
import com.martin_thoma.tide.core.ringing.RingingStatus
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/** Past this share of the track, letting go dismisses; before it, the handle slides back. */
private const val DISMISS_THRESHOLD = 0.8f

private val SNOOZE_HEIGHT = 88.dp

/** Clearance between the snooze pane and the foam at the shoreline. */
private val SHALLOW_WATER = 32.dp

/**
 * High tide: the alarm rings now. Huge time and label, readable without glasses; snooze is
 * the bigger, easier target, dismiss needs a deliberate slide (requirements 7.3).
 */
@Composable
internal fun RingingContent(
    status: RingingStatus,
    now: ZonedDateTime,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onAddMinute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TideSurface(level = TideLevel.HIGH, modifier = modifier) {
        // While the alarm or timer loads (a few milliseconds), only the sea shows.
        when (status) {
            is RingingStatus.Ringing -> AlarmRinging(status, now, onSnooze, onDismiss)
            is RingingStatus.TimerRinging -> TimerRinging(status, now, onAddMinute, onDismiss)
            else -> Unit
        }
    }
}

@Composable
private fun AlarmRinging(
    status: RingingStatus.Ringing,
    now: ZonedDateTime,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val timeFormatter = rememberTimeFormatter()
    val dateFormatter = rememberDateFormatter("EEEEdMMMM")
    RingingLayout(
        headline = {
            TideAware { underwater ->
                Text(dateFormatter.format(now), style = MaterialTheme.typography.titleMedium, color = tideVariantColor(underwater))
            }
            BigText(timeFormatter.format(now))
            Label(status.alarm.label.ifEmpty { stringResource(R.string.feature_ringing_alarm) })
        },
        upper = { modifier -> SnoozeButton(minutes = status.alarm.snoozeMinutes, onClick = onSnooze, modifier = modifier) },
        lower = { SlideToDismiss(onDismiss = onDismiss) },
    )
}

/** A timer has run out: its overtime counts up; "+1 min" on the water, Stop on the sand. */
@Composable
private fun TimerRinging(
    status: RingingStatus.TimerRinging,
    now: ZonedDateTime,
    onAddMinute: () -> Unit,
    onStop: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val overtime = (now.toInstant().toEpochMilli() - (status.timer.endsAt ?: 0)).coerceAtLeast(0)
    RingingLayout(
        headline = {
            TideAware { underwater ->
                Text(
                    stringResource(R.string.feature_ringing_times_up),
                    style = MaterialTheme.typography.titleMedium,
                    color = tideVariantColor(underwater),
                )
            }
            // One character more than a time of day, so a little smaller.
            BigText("+" + formatDuration(overtime, locale), fontSize = 88.sp)
            Label(status.timer.label.ifEmpty { stringResource(R.string.feature_ringing_timer) })
        },
        upper = { modifier ->
            GlassPane(modifier = modifier.fillMaxWidth().height(SNOOZE_HEIGHT), shape = CircleShape, onClick = onAddMinute) {
                Text(
                    stringResource(R.string.feature_ringing_add_minute),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        },
        lower = {
            GlassPane(modifier = Modifier.fillMaxWidth().height(64.dp), shape = CircleShape, raised = true, onClick = onStop) {
                Text(
                    stringResource(R.string.feature_ringing_stop),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        },
    )
}

@Composable
private fun BigText(
    text: String,
    fontSize: TextUnit = 112.sp,
) {
    TideAware {
        Text(
            text,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize, lineHeight = fontSize * 1.08f),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun Label(text: String) {
    TideAware { Text(text, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) }
}

/** Placed by the waterline: [upper] on the water, clear of the foam; [lower] on the sand. */
@Composable
private fun RingingLayout(
    headline: @Composable ColumnScope.() -> Unit,
    upper: @Composable (Modifier) -> Unit,
    lower: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val shoreline = maxHeight * TideLevel.HIGH
        val upperBottom = shoreline - SHALLOW_WATER - 24.dp
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(upperBottom - SNOOZE_HEIGHT)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = headline,
        )
        upper(
            Modifier
                .offset(y = upperBottom - SNOOZE_HEIGHT)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 32.dp),
        )
        Box(
            modifier =
                Modifier
                    .offset(y = shoreline)
                    .fillMaxWidth()
                    .height(maxHeight - shoreline)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            lower()
        }
    }
}

@Composable
private fun SnoozeButton(
    minutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPane(modifier = modifier.fillMaxWidth().height(SNOOZE_HEIGHT), shape = CircleShape, raised = true, onClick = onClick) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(stringResource(R.string.feature_ringing_snooze), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.feature_ringing_snooze_minutes, minutes),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalTideColors.current.onGlassVariant,
            )
        }
    }
}

@Composable
private fun SlideToDismiss(onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val thumbSize = 56.dp
    val inset = 4.dp
    var trackWidth by remember { mutableIntStateOf(0) }
    var offset by remember { mutableFloatStateOf(0f) }
    val maxOffset = with(density) { (trackWidth - (thumbSize + inset * 2).toPx()).coerceAtLeast(0f) }
    val progress = if (maxOffset > 0f) offset / maxOffset else 0f
    val dismissLabel = stringResource(R.string.feature_ringing_dismiss)
    val colors = LocalTideColors.current

    GlassPane(
        shape = CircleShape,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(thumbSize + inset * 2)
                .onSizeChanged { trackWidth = it.width }
                .draggable(
                    state = rememberDraggableState { delta -> offset = (offset + delta).coerceIn(0f, maxOffset) },
                    orientation = Orientation.Horizontal,
                    reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                    onDragStopped = { velocity ->
                        val dismiss = offset >= maxOffset * DISMISS_THRESHOLD
                        animate(offset, if (dismiss) maxOffset else 0f, velocity, spring()) { value, _ -> offset = value }
                        if (dismiss) onDismiss()
                    },
                ).semantics(mergeDescendants = true) {
                    // TalkBack users double-tap instead of sliding.
                    role = Role.Button
                    onClick(label = dismissLabel) {
                        onDismiss()
                        true
                    }
                },
    ) {
        Text(
            text = stringResource(R.string.feature_ringing_slide_to_dismiss),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onGlassVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = thumbSize)
                    .alpha(1f - progress),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .padding(inset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(colors.dismissThumb)
                    .border(1.dp, colors.dismissThumbRim, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(TideIcons.ArrowForward), contentDescription = null, tint = colors.onDismissThumb)
        }
    }
}

@Preview(widthDp = 393, heightDp = 851)
@Composable
private fun RingingPreview() {
    val now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
    TideTheme(darkTheme = false) {
        RingingContent(
            status = RingingStatus.Ringing(Alarm(id = 1, time = LocalTime.of(6, 30), label = "Work"), now.toInstant()),
            now = now,
            onSnooze = {},
            onDismiss = {},
            onAddMinute = {},
        )
    }
}
