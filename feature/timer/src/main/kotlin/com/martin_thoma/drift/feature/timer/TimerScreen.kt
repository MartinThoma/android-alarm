package com.martin_thoma.drift.feature.timer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.drift.core.designsystem.GlassPane
import com.martin_thoma.drift.core.designsystem.LocalTideColors
import com.martin_thoma.drift.core.designsystem.SwipeToDelete
import com.martin_thoma.drift.core.designsystem.TideAware
import com.martin_thoma.drift.core.designsystem.TideIcons
import com.martin_thoma.drift.core.designsystem.TideLevel
import com.martin_thoma.drift.core.designsystem.TideSurface
import com.martin_thoma.drift.core.designsystem.formatDuration
import com.martin_thoma.drift.core.designsystem.rememberTimeFormatter
import com.martin_thoma.drift.core.model.Timer
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable
data object TimerRoute : NavKey

private val RING_SIZE = 296.dp
private val RENAME_WIDTH = 200.dp
private const val MAX_LABEL_LENGTH = 40

@Serializable
data class TimerDetailRoute(
    val timerId: Long,
) : NavKey

/**
 * The timer tab. No timers: the wheel to set one. One timer: full screen, the time left inside a
 * ring that empties as it runs, the tide rising behind it (requirements TM-08). Several: a list
 * like the alarms, the one that ends next on top (Martin, 2026-10-31).
 */
@Composable
fun TimerScreen(
    onNewTimer: () -> Unit,
    onOpenTimer: (timerId: Long) -> Unit,
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
    val now = rememberSecondTicks(current.any { it.isRunning }, viewModel::now)
    if (current.size == 1) {
        SingleTimerContent(
            timer = current.single(),
            now = now,
            onStartOrPause = viewModel::startOrPause,
            onAddMinute = viewModel::addMinute,
            onReset = viewModel::reset,
            onRename = viewModel::rename,
            leading = { DeleteButton { viewModel.delete(current.single()) } },
            trailing = { NewTimerButton(onNewTimer) },
            modifier = modifier,
        )
    } else {
        TimerListContent(
            timers = current,
            now = now,
            onNewTimer = onNewTimer,
            onOpen = { onOpenTimer(it.id) },
            onStartOrPause = viewModel::startOrPause,
            onReset = viewModel::reset,
            onDelete = viewModel::delete,
            modifier = modifier,
        )
    }
}

/** One timer of several, opened from the list. */
@Composable
fun TimerDetailScreen(
    timerId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TimerViewModel = hiltViewModel()
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val current = timers ?: return
    val timer = current.find { it.id == timerId }
    // Deleted, here or from its notification: back to the list.
    LaunchedEffect(timer == null) { if (timer == null) onBack() }
    timer ?: return
    val now = rememberSecondTicks(timer.isRunning, viewModel::now)
    SingleTimerContent(
        timer = timer,
        now = now,
        onStartOrPause = viewModel::startOrPause,
        onAddMinute = viewModel::addMinute,
        onReset = viewModel::reset,
        onRename = viewModel::rename,
        leading = {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(TideIcons.ArrowBack),
                    contentDescription = stringResource(R.string.feature_timer_back),
                    tint = LocalTideColors.current.onSea,
                )
            }
        },
        trailing = { DeleteButton { viewModel.delete(timer) } },
        modifier = modifier,
    )
}

/** The current time, every second while [running]; the screen only ticks while it is shown. */
@Composable
private fun rememberSecondTicks(
    running: Boolean,
    clock: () -> Long,
): Long {
    var now by remember { mutableLongStateOf(clock()) }
    LaunchedEffect(running) {
        now = clock()
        while (running) {
            delay(1_000 - now % 1_000)
            now = clock()
        }
    }
    return now
}

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    IconButton(onClick = onDelete) {
        Icon(
            painterResource(TideIcons.Delete),
            contentDescription = stringResource(R.string.feature_timer_delete),
            tint = LocalTideColors.current.onSea,
        )
    }
}

@Composable
private fun NewTimerButton(onNewTimer: () -> Unit) {
    IconButton(onClick = onNewTimer) {
        Icon(
            painterResource(TideIcons.Add),
            contentDescription = stringResource(R.string.feature_timer_new),
            tint = LocalTideColors.current.onSea,
        )
    }
}

@Composable
internal fun SingleTimerContent(
    timer: Timer,
    now: Long,
    onStartOrPause: (Timer) -> Unit,
    onAddMinute: (Timer) -> Unit,
    onReset: (Timer) -> Unit,
    onRename: (Timer, String) -> Unit,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The tide follows the timer: it comes in as the timer runs out.
    val level = if (timer.isIdle) TideLevel.LOW else TideLevel.forProgress(timer.progress(now))
    TideSurface(level = level, modifier = modifier) {
        Column(
            modifier =
                Modifier.fillMaxSize().windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                leading()
                Spacer(Modifier.weight(1f))
                trailing()
            }
            TimerPage(timer, now, onStartOrPause, onAddMinute, onReset, onRename)
        }
    }
}

/** Several timers, like the alarm list: the one that ends next large on top, all of them on glass below. */
@Composable
internal fun TimerListContent(
    timers: List<Timer>,
    now: Long,
    onNewTimer: () -> Unit,
    onOpen: (Timer) -> Unit,
    onStartOrPause: (Timer) -> Unit,
    onReset: (Timer) -> Unit,
    onDelete: (Timer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val locale = LocalConfiguration.current.locales[0]
    val next = timers.filter { it.isRunning }.minByOrNull { it.remaining(now) }
    val level = next?.let { TideLevel.forProgress(it.progress(now)) } ?: TideLevel.LOW
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 112.dp),
            ) {
                if (next != null) {
                    item(key = "next") {
                        Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TideAware {
                                Text(
                                    formatDuration(next.remaining(now) + 999, locale),
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp, lineHeight = 96.sp),
                                )
                            }
                            TideAware { Text(statusText(next, now), style = MaterialTheme.typography.titleLarge) }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
                item(key = "sheet") {
                    GlassPane(Modifier.fillMaxWidth()) {
                        Column(Modifier.animateContentSize()) {
                            timers.forEachIndexed { index, timer ->
                                key(timer.id) {
                                    if (index >
                                        0
                                    ) {
                                        HorizontalDivider(
                                            Modifier.padding(horizontal = 20.dp),
                                            color = colors.onGlass.copy(alpha = 0.12f),
                                        )
                                    }
                                    SwipeToDelete(onDelete = { onDelete(timer) }) {
                                        TimerRow(timer, now, onOpen, onStartOrPause, onReset)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A timer in the list: time left, label and state, a small ring, and its main button. */
@Composable
private fun TimerRow(
    timer: Timer,
    now: Long,
    onOpen: (Timer) -> Unit,
    onStartOrPause: (Timer) -> Unit,
    onReset: (Timer) -> Unit,
) {
    val colors = LocalTideColors.current
    val locale = LocalConfiguration.current.locales[0]
    val expired = timer.hasExpired(now)
    val remaining = timer.remaining(now)
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(timer) }.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (expired) "+" + formatDuration(-remaining, locale) else formatDuration(remaining + 999, locale),
                style = MaterialTheme.typography.displaySmall,
                color = if (expired) colors.accent else colors.onGlass,
            )
            Text(statusText(timer, now), style = MaterialTheme.typography.bodyMedium, color = colors.onGlassVariant, maxLines = 1)
        }
        Canvas(Modifier.size(32.dp)) {
            val stroke = 4.dp.toPx()
            val arc = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(colors.onGlass.copy(alpha = 0.12f), 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            val left = 1f - timer.progress(now)
            if (left >
                0f
            ) {
                drawArc(
                    colors.accent,
                    -90f + 360f * (1f - left),
                    360f * left,
                    false,
                    topLeft,
                    arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        FilledIconButton(
            onClick = { if (expired) onReset(timer) else onStartOrPause(timer) },
            modifier = Modifier.size(48.dp),
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
            Icon(painterResource(icon), contentDescription = stringResource(label))
        }
    }
}

/** "Tea · Rings at 16:10", "Tea · Paused", "Tea · Time's up". */
@Composable
private fun statusText(
    timer: Timer,
    now: Long,
): String {
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = rememberTimeFormatter()
    val name = timer.label.ifEmpty { stringResource(R.string.feature_timer_title) }
    val state =
        when {
            timer.hasExpired(now) -> {
                stringResource(R.string.feature_timer_times_up)
            }

            timer.isRunning -> {
                stringResource(
                    R.string.feature_timer_rings_at,
                    timeFormatter.format(Instant.ofEpochMilli(checkNotNull(timer.endsAt)).atZone(ZoneId.systemDefault())),
                )
            }

            timer.isIdle -> {
                stringResource(R.string.feature_timer_of, formatDuration(timer.setMillis, locale))
            }

            else -> {
                stringResource(R.string.feature_timer_paused_short)
            }
        }
    return "$name · $state"
}

@Composable
private fun TimerPage(
    timer: Timer,
    now: Long,
    onStartOrPause: (Timer) -> Unit,
    onAddMinute: (Timer) -> Unit,
    onReset: (Timer) -> Unit,
    onRename: (Timer, String) -> Unit,
) {
    val colors = LocalTideColors.current
    val expired = timer.hasExpired(now)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.6f))
        TimerRing(timer, now, onRename)
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
    onRename: (Timer, String) -> Unit,
) {
    var renaming by rememberSaveable { mutableStateOf(false) }
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
            // The name, with a pencil: tap it to rename the timer in place (Martin, 2026-10-31).
            if (renaming) {
                RenameField(timer.label) {
                    renaming = false
                    onRename(timer, it)
                }
            } else {
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = stringResource(R.string.feature_timer_rename)) { renaming = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        timer.label.ifEmpty { stringResource(R.string.feature_timer_title) },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onGlassVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Icon(
                        painterResource(TideIcons.Edit),
                        contentDescription = null,
                        tint = colors.onGlassVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
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

/** The timer's name as a text field; the keyboard's done key or tapping elsewhere keeps it. */
@Composable
private fun RenameField(
    current: String,
    onDone: (String) -> Unit,
) {
    val colors = LocalTideColors.current
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(current, TextRange(0, current.length)))
    }
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val finish = {
        if (!done) {
            done = true
            onDone(text.text)
        }
    }
    val description = stringResource(R.string.feature_timer_rename)
    BasicTextField(
        value = text,
        onValueChange = { if (it.text.length <= MAX_LABEL_LENGTH) text = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onGlass, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { finish() }),
        modifier =
            Modifier
                .width(RENAME_WIDTH)
                .focusRequester(focus)
                .onFocusChanged {
                    if (focused && !it.isFocused) finish()
                    focused = it.isFocused
                }.semantics { contentDescription = description },
        decorationBox = { field ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(vertical = 4.dp)) { field() }
                Box(Modifier.fillMaxWidth().height(2.dp).background(colors.accent, RoundedCornerShape(1.dp)))
            }
        },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun StatusLine(
    text: String,
    color: Color,
) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = color)
}
