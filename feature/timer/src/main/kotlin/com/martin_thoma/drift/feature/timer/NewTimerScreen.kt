package com.martin_thoma.drift.feature.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.drift.core.data.RecentTimer
import com.martin_thoma.drift.core.designsystem.GlassPane
import com.martin_thoma.drift.core.designsystem.LocalTideColors
import com.martin_thoma.drift.core.designsystem.TideIcons
import com.martin_thoma.drift.core.designsystem.TideLevel
import com.martin_thoma.drift.core.designsystem.TideSurface
import com.martin_thoma.drift.core.designsystem.formatDuration
import com.martin_thoma.drift.core.designsystem.keepOnOpenWater
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.abs

@Serializable
data object NewTimerRoute : NavKey

/** The three parts of a duration; the wheel sets the one selected at the top. */
private enum class Field(
    val max: Int,
    val label: Int,
    val millis: Long,
) {
    HOURS(23, R.string.feature_timer_hours, 3_600_000),
    MINUTES(59, R.string.feature_timer_minutes_unit, 60_000),
    SECONDS(59, R.string.feature_timer_seconds, 1_000),
}

private val WHEEL_ITEM = 52.dp
private const val WHEEL_VISIBLE = 5

/** A further timer, while others run. */
@Composable
fun NewTimerScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TimerViewModel = hiltViewModel()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    NewTimerContent(
        recents = recents,
        onStart = { millis, label ->
            viewModel.create(millis, label)
            onDone()
        },
        onBack = onDone,
        modifier = modifier,
    )
}

/**
 * Setting a timer (requirements TM-01, TM-04): recent timers start with one tap; otherwise tap
 * hours, minutes or seconds at the top and turn the wheel below to set it (Martin, 2026-10-31).
 */
@Composable
internal fun NewTimerContent(
    recents: List<RecentTimer>,
    onStart: (millis: Long, label: String) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var hours by rememberSaveable { mutableIntStateOf(0) }
    var minutes by rememberSaveable { mutableIntStateOf(0) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    var unit by rememberSaveable { mutableStateOf(Field.MINUTES) }
    var label by rememberSaveable { mutableStateOf("") }
    val colors = LocalTideColors.current
    val locale = LocalConfiguration.current.locales[0]
    val millis = hours * Field.HOURS.millis + minutes * Field.MINUTES.millis + seconds * Field.SECONDS.millis
    var level by remember { mutableFloatStateOf(TideLevel.LOW) }

    fun valueOf(u: Field) =
        when (u) {
            Field.HOURS -> hours
            Field.MINUTES -> minutes
            Field.SECONDS -> seconds
        }

    fun set(
        u: Field,
        value: Int,
    ) = when (u) {
        Field.HOURS -> hours = value
        Field.MINUTES -> minutes = value
        Field.SECONDS -> seconds = value
    }

    TideSurface(level = level, modifier = modifier) {
        Column(
            modifier =
                Modifier.fillMaxSize().windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(TideIcons.ArrowBack),
                            contentDescription = stringResource(R.string.feature_timer_back),
                            tint = colors.onSea,
                        )
                    }
                }
            }
            Row(Modifier.keepOnOpenWater { level = it }, verticalAlignment = Alignment.Top) {
                Field.entries.forEachIndexed { index, u ->
                    if (index >
                        0
                    ) {
                        Text(
                            ":",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            color = colors.onSea.copy(alpha = 0.6f),
                        )
                    }
                    Segment(value = valueOf(u), unit = u, selected = unit == u, locale = locale, onSelect = { unit = u })
                }
            }
            Spacer(Modifier.height(12.dp))
            RecentChips(recents, locale, onStart = { onStart(it.setMillis, it.label) })
            Spacer(Modifier.weight(1f))
            // The wheel starts afresh for each unit, at that unit's value.
            key(unit) {
                NumberWheel(value = valueOf(unit), unit = unit, locale = locale, onValue = { set(unit, it) })
            }
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text(stringResource(R.string.feature_timer_label)) },
                singleLine = true,
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().padding(horizontal = 24.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.glass,
                        unfocusedContainerColor = colors.glass,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.glassRim,
                    ),
            )
            Spacer(Modifier.height(16.dp))
            FilledIconButton(
                onClick = { onStart(millis, label) },
                enabled = millis > 0,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.accent, contentColor = colors.onAccent),
            ) {
                Icon(
                    painterResource(TideIcons.Play),
                    contentDescription = stringResource(R.string.feature_timer_start),
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** One of hours, minutes, seconds: tap it to set it with the wheel. The one being set is marked. */
@Composable
private fun Segment(
    value: Int,
    unit: Field,
    selected: Boolean,
    locale: Locale,
    onSelect: () -> Unit,
) {
    val colors = LocalTideColors.current
    val name = stringResource(unit.label)
    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) colors.glass else colors.glass.copy(alpha = 0f))
                .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
                .padding(horizontal = 6.dp)
                .semantics { contentDescription = "$name: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            String.format(locale, "%02d", value),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
            color = if (selected) colors.onGlass else colors.onSea,
        )
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.accent else colors.onSea.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

/**
 * A wheel of numbers that snaps to the one in the middle. TalkBack users get "increase" and
 * "decrease" actions instead of turning it.
 */
@Composable
private fun NumberWheel(
    value: Int,
    unit: Field,
    locale: Locale,
    onValue: (Int) -> Unit,
) {
    val colors = LocalTideColors.current
    val state = rememberLazyListState(initialFirstVisibleItemIndex = value)
    // The item in the middle: the list is padded by two items above and below.
    val centre by remember {
        derivedStateOf {
            val itemPx =
                state.layoutInfo.visibleItemsInfo
                    .firstOrNull()
                    ?.size ?: 1
            (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0).coerceIn(0, unit.max)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { centre }.distinctUntilChanged().collect(onValue)
    }
    // A value set another way (TalkBack's actions) turns the wheel, too.
    LaunchedEffect(value) {
        if (value != centre) state.animateScrollToItem(value)
    }
    val name = stringResource(unit.label)
    val increase = stringResource(R.string.feature_timer_increase)
    val decrease = stringResource(R.string.feature_timer_decrease)
    Box(
        modifier =
            Modifier
                .width(160.dp)
                .height(WHEEL_ITEM * WHEEL_VISIBLE)
                .semantics {
                    contentDescription = name
                    stateDescription = centre.toString()
                    customActions =
                        listOf(
                            CustomAccessibilityAction(increase) {
                                onValue((centre + 1).coerceAtMost(unit.max))
                                true
                            },
                            CustomAccessibilityAction(decrease) {
                                onValue((centre - 1).coerceAtLeast(0))
                                true
                            },
                        )
                },
        contentAlignment = Alignment.Center,
    ) {
        // The window that shows which number is chosen.
        GlassPane(Modifier.fillMaxWidth().height(WHEEL_ITEM), shape = RoundedCornerShape(16.dp)) {}
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = WHEEL_ITEM * (WHEEL_VISIBLE / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(unit.max + 1) { number ->
                val distance = abs(number - centre)
                Box(Modifier.height(WHEEL_ITEM).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        String.format(locale, "%02d", number),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = if (distance == 0) 34.sp else 26.sp),
                        color = if (distance == 0) colors.onGlass else colors.onSand,
                        modifier = Modifier.alpha(if (distance == 0) 1f else (0.55f - 0.2f * (distance - 1)).coerceAtLeast(0.15f)),
                    )
                }
            }
        }
    }
}

/** Recent timers: one tap starts one again, with its label. */
@Composable
private fun RecentChips(
    recents: List<RecentTimer>,
    locale: Locale,
    onStart: (RecentTimer) -> Unit,
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(recents) { recent ->
            val wholeMinutes = recent.setMillis % 60_000 == 0L && recent.setMillis < 3_600_000
            val duration =
                if (wholeMinutes) {
                    stringResource(
                        R.string.feature_timer_minutes,
                        recent.setMillis / 60_000,
                    )
                } else {
                    formatDuration(recent.setMillis, locale)
                }
            GlassPane(shape = CircleShape, onClick = { onStart(recent) }) {
                Text(
                    text = if (recent.label.isEmpty()) duration else "${recent.label} · $duration",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}
