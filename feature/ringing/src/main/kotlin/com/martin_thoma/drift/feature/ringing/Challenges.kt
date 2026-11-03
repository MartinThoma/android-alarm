package com.martin_thoma.drift.feature.ringing

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin_thoma.drift.core.designsystem.GlassPane
import com.martin_thoma.drift.core.designsystem.LocalTideColors
import com.martin_thoma.drift.core.designsystem.TideAware
import com.martin_thoma.drift.core.designsystem.TideIcons
import com.martin_thoma.drift.core.designsystem.TideLevel
import com.martin_thoma.drift.core.designsystem.tideVariantColor
import com.martin_thoma.drift.core.model.Challenge
import kotlinx.coroutines.launch
import java.text.NumberFormat

/** A control on the sand is as tall as the slider's track. */
private val SAND_CONTROL_HEIGHT = 64.dp

/**
 * How the alarm is dismissed, on the sand below the snooze pane: the slider, the shake bar, or a
 * button that opens the sums ([onSolve]). Without an accelerometer, shaking falls back to the slider:
 * an alarm that cannot be turned off would only stop by itself after ten minutes.
 */
@Composable
internal fun DismissControl(
    challenge: Challenge,
    onDismiss: () -> Unit,
    onSolve: () -> Unit,
) {
    when (challenge) {
        Challenge.SLIDE -> {
            SlideToDismiss(onDismiss)
        }

        // The circle sits in the middle of the screen, under the time (see HoldToDismiss); the sand stays bare.
        Challenge.HOLD -> {}

        Challenge.SHAKE -> {
            val context = LocalContext.current
            val sensors = remember { context.getSystemService(SensorManager::class.java) }
            val accelerometer = remember { sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
            if (sensors != null && accelerometer != null) ShakeToDismiss(sensors, accelerometer, onDismiss) else SlideToDismiss(onDismiss)
        }

        Challenge.MATH -> {
            GlassPane(
                modifier = Modifier.fillMaxWidth().height(SAND_CONTROL_HEIGHT),
                shape = CircleShape,
                // Flat like the slider's track: snooze stays the one raised pane.
                onClick = onSolve,
            ) {
                Text(
                    stringResource(R.string.feature_ringing_solve_to_dismiss),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

/** How long to hold the circle. The alarm editor says "three seconds" (`feature_alarm_challenge_hold_hint`). */
private const val HOLD_MILLIS = 3_000L

/**
 * A big circle in the middle of the screen. Held down, a ring fills around it; held for
 * [HOLD_MILLIS], it dismisses. Let go early, and the ring drains back. The time is measured
 * in frames, not as an animation, so "Remove animations" does not make it a tap.
 * TalkBack users double-tap and hold, which TalkBack sends as a long click.
 */
@Composable
internal fun HoldToDismiss(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val dismiss by rememberUpdatedState(onDismiss)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissLabel = stringResource(R.string.feature_ringing_dismiss)
    GlassPane(
        shape = CircleShape,
        modifier =
            modifier
                .size(176.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val hold =
                            scope.launch {
                                val start = withFrameMillis { it }
                                do {
                                    val held = withFrameMillis { it } - start
                                    progress.snapTo((held / HOLD_MILLIS.toFloat()).coerceAtMost(1f))
                                } while (progress.value < 1f)
                                dismiss()
                            }
                        // Returns when the finger lifts or slides off the circle.
                        waitForUpOrCancellation()
                        if (hold.isActive) {
                            hold.cancel()
                            scope.launch { progress.animateTo(0f, spring()) }
                        }
                    }
                }.semantics(mergeDescendants = true) {
                    role = Role.Button
                    onLongClick(label = dismissLabel) {
                        dismiss()
                        true
                    }
                },
    ) {
        Canvas(Modifier.matchParentSize().padding(8.dp)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawArc(colors.onGlass.copy(alpha = 0.15f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(colors.accent, startAngle = -90f, sweepAngle = 360f * progress.value, useCenter = false, style = stroke)
        }
        Text(
            stringResource(R.string.feature_ringing_hold_to_dismiss),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp),
        )
    }
}

/** A bar that fills with every shake; full, it dismisses. Progress survives a rotation, which shaking may cause. */
@Composable
private fun ShakeToDismiss(
    sensors: SensorManager,
    accelerometer: Sensor,
    onDismiss: () -> Unit,
) {
    val colors = LocalTideColors.current
    var shakes by rememberSaveable { mutableIntStateOf(0) }
    val dismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(sensors, accelerometer) {
        val counter = ShakeCounter(shakes)
        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (counter.shakes >= ShakeCounter.NEEDED) return
                    if (!counter.onReading(event.values[0], event.values[1], event.values[2], event.timestamp)) return
                    shakes = counter.shakes
                    if (shakes == ShakeCounter.NEEDED) dismiss()
                }

                override fun onAccuracyChanged(
                    sensor: Sensor,
                    accuracy: Int,
                ) = Unit
            }
        sensors.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors.unregisterListener(listener) }
    }
    val progress by animateFloatAsState(shakes / ShakeCounter.NEEDED.toFloat(), label = "shakeProgress")
    val label = stringResource(R.string.feature_ringing_shake_to_dismiss)
    GlassPane(
        shape = CircleShape,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(SAND_CONTROL_HEIGHT)
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(shakes / ShakeCounter.NEEDED.toFloat(), 0f..1f)
                },
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onGlassVariant)
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.onGlass.copy(alpha = 0.15f)),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(colors.accent))
            }
        }
    }
}

/**
 * The sums, in place of the time while they are open: [MathProblem.COUNT] of them, one after the
 * other, typed on a keypad over the water. [solved] counts the sums done so far, [onCorrect] adds
 * one; the caller dismisses after the last. Back returns to the
 * time and snooze, and the sums done stay done.
 */
@Composable
internal fun SumsPanel(
    seed: Long,
    solved: Int,
    onCorrect: () -> Unit,
    onBack: () -> Unit,
) {
    val problem = MathProblem.series(seed)[solved.coerceAtMost(MathProblem.COUNT - 1)]
    val locale = LocalConfiguration.current.locales[0]
    val numbers = remember(locale) { NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false } }
    var typed by rememberSaveable(seed, solved) { mutableStateOf("") }
    var wrong by rememberSaveable(seed, solved) { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val shoreline = maxHeight * TideLevel.HIGH
        Box(
            Modifier
                .fillMaxWidth()
                .height(shoreline - 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = shoreline - 16.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                TideAware { underwater ->
                    Text(
                        stringResource(R.string.feature_ringing_sum_progress, solved + 1, MathProblem.COUNT),
                        style = MaterialTheme.typography.titleMedium,
                        color = tideVariantColor(underwater),
                    )
                }
                TideAware {
                    Text(
                        "${numbers.format(problem.left)} ${problem.operator.symbol} ${numbers.format(problem.right)}",
                        // A sum reads left to right, also in Arabic.
                        style =
                            MaterialTheme.typography.displayLarge.copy(
                                fontSize = 64.sp,
                                lineHeight = 72.sp,
                                textDirection = TextDirection.Ltr,
                            ),
                        maxLines = 1,
                    )
                }
                AnswerField(if (typed.isEmpty()) null else numbers.format(typed.toInt()))
                TideAware {
                    Text(
                        if (wrong) stringResource(R.string.feature_ringing_wrong_answer) else "",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Keypad(
                    digit = { numbers.format(it) },
                    onDigit = { digit ->
                        wrong = false
                        if (typed.length < MAX_DIGITS) typed += digit
                    },
                    onDelete = {
                        wrong = false
                        typed = typed.dropLast(1)
                    },
                    onCheck = {
                        when {
                            typed.isEmpty() -> {}

                            typed.toInt() == problem.answer -> {
                                onCorrect()
                            }

                            else -> {
                                wrong = true
                                typed = ""
                            }
                        }
                    },
                )
            }
        }
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
            GlassPane(modifier = Modifier.fillMaxWidth().height(SAND_CONTROL_HEIGHT), shape = CircleShape, onClick = onBack) {
                Text(
                    stringResource(R.string.feature_ringing_back),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

private const val MAX_DIGITS = 2

/** The typed answer on glass, or a question mark until the first digit. */
@Composable
private fun AnswerField(answer: String?) {
    val colors = LocalTideColors.current
    val label = stringResource(R.string.feature_ringing_answer)
    GlassPane(
        modifier =
            Modifier
                .width(160.dp)
                .height(72.dp)
                .semantics(mergeDescendants = true) { contentDescription = "$label: ${answer.orEmpty()}" },
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            answer ?: "?",
            style = MaterialTheme.typography.displaySmall,
            color = if (answer == null) colors.onGlassVariant else colors.onGlass,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** 1 to 9, then delete, 0 and check, laid out like a phone's keypad in every language. */
@Composable
private fun Keypad(
    digit: (Int) -> String,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onCheck: () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        GlassPane(Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { n -> TextKey(digit(n)) { onDigit('0' + n) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconKey(TideIcons.Backspace, stringResource(R.string.feature_ringing_delete_digit), onDelete)
                    TextKey(digit(0)) { onDigit('0') }
                    IconKey(TideIcons.Check, stringResource(R.string.feature_ringing_check_answer), onCheck, accent = true)
                }
            }
        }
    }
}

@Composable
private fun RowScope.TextKey(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(KEY_HEIGHT)
                .clip(RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun RowScope.IconKey(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    val colors = LocalTideColors.current
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(KEY_HEIGHT)
                .clip(RoundedCornerShape(20.dp))
                .then(if (accent) Modifier.background(colors.accent) else Modifier)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = if (accent) colors.onAccent else colors.onGlass)
    }
}

private val KEY_HEIGHT = 64.dp
