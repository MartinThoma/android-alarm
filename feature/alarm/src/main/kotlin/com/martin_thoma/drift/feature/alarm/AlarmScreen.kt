package com.martin_thoma.drift.feature.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
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
import com.martin_thoma.drift.core.designsystem.TideTheme
import com.martin_thoma.drift.core.designsystem.rememberTimeFormatter
import com.martin_thoma.drift.core.domain.NextTrigger
import com.martin_thoma.drift.core.domain.UpcomingAlarm
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.ringing.AlarmSounds
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.toKotlinDuration

@Serializable
data object AlarmRoute : NavKey

@Composable
fun AlarmScreen(
    onOpenAlarm: (alarmId: Long) -> Unit,
    modifier: Modifier = Modifier,
    notice: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    val viewModel: AlarmListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AlarmListContent(
        state = state,
        onAdd = viewModel::add,
        onOpen = { onOpenAlarm(it.id) },
        onToggle = viewModel::setEnabled,
        onDelete = viewModel::delete,
        onRestore = viewModel::restore,
        modifier = modifier,
        notice = notice,
        footer = footer,
    )
}

@Composable
internal fun AlarmListContent(
    state: AlarmListUiState,
    onAdd: (LocalTime) -> Unit,
    onOpen: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
    onRestore: (Alarm) -> Unit,
    modifier: Modifier = Modifier,
    // Between the hero and the alarms, e.g. a setting that keeps alarms from ringing; after them, a quiet link.
    notice: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = rememberTimeFormatter()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showNewAlarm by rememberSaveable { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.feature_alarm_deleted)
    val resources = LocalResources.current
    val context = LocalContext.current
    // Without notifications, a ringing alarm cannot show its screen (requirements 6.4); the health check shows it, too.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val undoLabel = stringResource(R.string.feature_alarm_undo)

    val level =
        if (state.upcoming != null && state.now != null) {
            TideLevel.forTimeUntil(Duration.between(state.now, state.upcoming.triggerAt).toKotlinDuration())
        } else {
            TideLevel.LOW
        }

    TideSurface(level = level, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                NewAlarmButton(
                    onClick = {
                        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        showNewAlarm = true
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 112.dp),
            ) {
                item(key = "next") {
                    NextAlarm(state, timeFormatter, Modifier.padding(horizontal = 8.dp))
                    Spacer(Modifier.height(32.dp))
                }
                item(key = "notice") { notice() }
                // One sheet of glass for all alarms, readable wherever the tide is; empty, it shows where
                // alarms will be. It stays one list item either way: a separate item for the empty sheet
                // made the list keep the last deleted row's swipe state, and "Undo" deleted the alarm again.
                if (!state.loading) {
                    item(key = "sheet") {
                        GlassPane(Modifier.fillMaxWidth()) {
                            Column(Modifier.animateContentSize()) {
                                if (state.alarms.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.feature_alarm_empty),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = LocalTideColors.current.onGlassVariant,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                                    )
                                }
                                state.alarms.forEachIndexed { index, alarm ->
                                    key(alarm.id) {
                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                                color = LocalContentColor.current.copy(alpha = 0.12f),
                                            )
                                        }
                                        AlarmRow(
                                            alarm = alarm,
                                            timeFormatter = timeFormatter,
                                            locale = locale,
                                            onOpen = onOpen,
                                            onToggle = onToggle,
                                            onDelete = {
                                                onDelete(alarm)
                                                scope.launch {
                                                    val result =
                                                        snackbarHostState.showSnackbar(
                                                            deletedMessage,
                                                            undoLabel,
                                                            // Long enough to find Undo when half asleep.
                                                            duration = SnackbarDuration.Long,
                                                        )
                                                    if (result == SnackbarResult.ActionPerformed) onRestore(alarm)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "footer") { footer() }
            }
        }
    }

    if (showNewAlarm) {
        AlarmTimeDialog(
            is24Hour = is24Hour,
            onDismiss = { showNewAlarm = false },
            onConfirm = { time ->
                showNewAlarm = false
                onAdd(time)
                // Confirms the alarm and says when it rings (requirements section 8).
                val now = state.now ?: ZonedDateTime.now()
                val trigger = NextTrigger.of(Alarm(time = time), now)
                if (trigger != null) {
                    val message =
                        resources.getString(
                            R.string.feature_alarm_set_confirmation,
                            timeUntilText(resources, timeUntil(now, trigger)),
                        )
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
        )
    }
}

@Composable
private fun NextAlarm(
    state: AlarmListUiState,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
) {
    val upcoming = state.upcoming
    val now = state.now
    // An invisible copy of the full hero keeps its height, so the alarms below never jump when the
    // last alarm is turned off or on (Martin, on the S25). It follows the font size, unlike a fixed height.
    // The copy uses the same formatting, so it is as tall in every script (Arabic digits are taller),
    // and the box takes the taller of the two, so a long label is never cut off.
    Box(modifier, contentAlignment = Alignment.TopStart) {
        Column(Modifier.alpha(0f).clearAndSetSemantics {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                timeFormatter.format(LocalTime.MIDNIGHT),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp, lineHeight = 96.sp),
            )
            Text(stringResource(R.string.feature_alarm_in_minutes, 0), style = MaterialTheme.typography.titleLarge)
        }
        NextAlarmText(state, upcoming, now, timeFormatter)
    }
}

@Composable
private fun NextAlarmText(
    state: AlarmListUiState,
    upcoming: UpcomingAlarm?,
    now: ZonedDateTime?,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (upcoming == null || now == null) {
            if (!state.loading) {
                TideAware { Text(stringResource(R.string.feature_alarm_none), style = MaterialTheme.typography.headlineMedium) }
            }
        } else {
            TideAware {
                Text(
                    text = timeFormatter.format(upcoming.triggerAt),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp, lineHeight = 96.sp),
                )
            }
            val countdown = timeUntilText(timeUntil(now, upcoming.triggerAt))
            TideAware {
                Text(
                    text =
                        if (upcoming.alarm.label.isEmpty()) {
                            countdown
                        } else {
                            stringResource(R.string.feature_alarm_with_label, upcoming.alarm.label, countdown)
                        },
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    timeFormatter: DateTimeFormatter,
    locale: Locale,
    onOpen: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeToDelete(onDelete = onDelete, modifier = modifier) {
        val colors = LocalTideColors.current
        val deleteLabel = stringResource(R.string.feature_alarm_delete)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = stringResource(R.string.feature_alarm_edit_title)) { onOpen(alarm) }
                    // Swiping is not the only way: TalkBack offers Delete as an action. The editor will show it, too.
                    .semantics {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(deleteLabel) {
                                    onDelete()
                                    true
                                },
                            )
                    }.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = timeFormatter.format(alarm.time),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (alarm.enabled) colors.onGlass else colors.onGlassVariant,
                )
                Text(
                    text = alarmDetails(alarm, locale, timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onGlassVariant,
                )
            }
            // Named after its alarm, so TalkBack says which alarm it turns on or off.
            val switchName = stringResource(R.string.feature_alarm_switch, timeFormatter.format(alarm.time))
            Switch(
                checked = alarm.enabled,
                onCheckedChange = { onToggle(alarm, it) },
                colors = glassSwitchColors(),
                modifier = Modifier.semantics { contentDescription = switchName },
            )
        }
    }
}

@Composable
private fun NewAlarmButton(onClick: () -> Unit) {
    TideAware { underwater ->
        val colors = LocalTideColors.current
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = if (underwater) colors.onSea else colors.accent,
            contentColor = if (underwater) colors.sea else colors.onAccent,
        ) {
            Icon(painterResource(TideIcons.Add), contentDescription = stringResource(R.string.feature_alarm_new))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmTimeDialog(
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    initial: LocalTime = LocalTime.of(7, 0),
) {
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            // The one clear way to confirm: a filled button, not a text link (Martin, on the S25).
            Button(
                onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = LocalTideColors.current.accent,
                        contentColor = LocalTideColors.current.onAccent,
                    ),
            ) {
                Text(stringResource(R.string.feature_alarm_set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_alarm_cancel)) }
        },
        text = { TimePicker(state = pickerState) },
    )
}

@Composable
internal fun glassSwitchColors(): SwitchColors {
    val colors = LocalTideColors.current
    return SwitchDefaults.colors(
        checkedTrackColor = colors.accent,
        checkedThumbColor = colors.onAccent,
        uncheckedTrackColor = Color.Transparent,
        uncheckedThumbColor = colors.onGlassVariant,
        uncheckedBorderColor = colors.onGlassVariant,
    )
}

private fun timeUntilText(
    resources: Resources,
    timeUntil: TimeUntil,
): String =
    when (timeUntil) {
        is TimeUntil.Minutes -> resources.getString(R.string.feature_alarm_in_minutes, timeUntil.minutes)
        is TimeUntil.HoursMinutes -> resources.getString(R.string.feature_alarm_in_hours_minutes, timeUntil.hours, timeUntil.minutes)
        is TimeUntil.DaysHours -> resources.getString(R.string.feature_alarm_in_days_hours, timeUntil.days, timeUntil.hours)
    }

@Composable
private fun timeUntilText(timeUntil: TimeUntil): String =
    when (timeUntil) {
        is TimeUntil.Minutes -> stringResource(R.string.feature_alarm_in_minutes, timeUntil.minutes)
        is TimeUntil.HoursMinutes -> stringResource(R.string.feature_alarm_in_hours_minutes, timeUntil.hours, timeUntil.minutes)
        is TimeUntil.DaysHours -> stringResource(R.string.feature_alarm_in_days_hours, timeUntil.days, timeUntil.hours)
    }

@Composable
private fun alarmDetails(
    alarm: Alarm,
    locale: Locale,
    timeFormatter: DateTimeFormatter,
): String {
    fun short(day: DayOfWeek) = day.getDisplayName(TextStyle.SHORT, locale)

    val days =
        when (val label = repeatLabel(alarm.repeatDays)) {
            RepeatLabel.Once -> stringResource(R.string.feature_alarm_once)
            RepeatLabel.EveryDay -> stringResource(R.string.feature_alarm_every_day)
            is RepeatLabel.Range -> stringResource(R.string.feature_alarm_range, short(label.first), short(label.last))
            is RepeatLabel.Days -> label.days.joinToString(", ", transform = ::short)
        }
    val labelled = if (alarm.label.isEmpty()) days else stringResource(R.string.feature_alarm_with_label, days, alarm.label)
    // The default sound goes unmentioned; any other sound is named.
    val sound = soundTitle(alarm.soundUri).ifEmpty { null }
    val details =
        if (alarm.soundUri == null ||
            sound == null
        ) {
            labelled
        } else {
            stringResource(R.string.feature_alarm_with_label, labelled, sound)
        }
    val snoozedUntil = alarm.snoozedUntil ?: return details
    val until = timeFormatter.format(snoozedUntil.atZone(ZoneId.systemDefault()))
    return stringResource(R.string.feature_alarm_with_label, stringResource(R.string.feature_alarm_snoozed_until, until), details)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AlarmListPreview() {
    val now = ZonedDateTime.parse("2026-11-01T23:18:00+01:00[Europe/Berlin]")
    val weekdays = DayOfWeek.entries.take(5).toSet()
    val alarms =
        listOf(
            Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays, label = "Work"),
            Alarm(id = 2, time = LocalTime.of(8, 15), repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
            Alarm(id = 3, time = LocalTime.of(5, 10), enabled = false, label = "Airport"),
        )
    TideTheme(darkTheme = true) {
        AlarmListContent(
            state = AlarmListUiState(loading = false, alarms = alarms, upcoming = NextTrigger.upcoming(alarms, now), now = now),
            onAdd = {},
            onOpen = {},
            onToggle = { _, _ -> },
            onDelete = {},
            onRestore = {},
        )
    }
}
