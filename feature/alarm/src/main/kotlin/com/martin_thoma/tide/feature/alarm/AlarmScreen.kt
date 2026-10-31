package com.martin_thoma.tide.feature.alarm

import android.text.format.DateFormat
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.tide.core.designsystem.LocalTideColors
import com.martin_thoma.tide.core.designsystem.TideAware
import com.martin_thoma.tide.core.designsystem.TideIcons
import com.martin_thoma.tide.core.designsystem.TideLevel
import com.martin_thoma.tide.core.designsystem.TideSurface
import com.martin_thoma.tide.core.designsystem.TideTheme
import com.martin_thoma.tide.core.designsystem.tideVariantColor
import com.martin_thoma.tide.core.domain.NextTrigger
import com.martin_thoma.tide.core.model.Alarm
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.toKotlinDuration

@Serializable
data object AlarmRoute : NavKey

@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    val viewModel: AlarmListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AlarmListContent(
        state = state,
        onAdd = viewModel::add,
        onToggle = viewModel::setEnabled,
        onDelete = viewModel::delete,
        onRestore = viewModel::restore,
        modifier = modifier,
    )
}

@Composable
internal fun AlarmListContent(
    state: AlarmListUiState,
    onAdd: (LocalTime) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
    onRestore: (Alarm) -> Unit,
    modifier: Modifier = Modifier,
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(is24Hour, locale) { DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showNewAlarm by rememberSaveable { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.feature_alarm_deleted)
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
            floatingActionButton = { NewAlarmButton(onClick = { showNewAlarm = true }) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 112.dp),
            ) {
                item(key = "next") {
                    NextAlarm(state, timeFormatter)
                    Spacer(Modifier.height(40.dp))
                }
                if (!state.loading && state.alarms.isEmpty()) {
                    item(key = "empty") {
                        TideAware { underwater ->
                            Text(
                                text = stringResource(R.string.feature_alarm_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = tideVariantColor(underwater),
                            )
                        }
                    }
                }
                items(state.alarms, key = { it.id }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        timeFormatter = timeFormatter,
                        locale = locale,
                        onToggle = onToggle,
                        onDelete = {
                            onDelete(alarm)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                                if (result == SnackbarResult.ActionPerformed) onRestore(alarm)
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showNewAlarm) {
        NewAlarmDialog(
            is24Hour = is24Hour,
            onDismiss = { showNewAlarm = false },
            onConfirm = { time ->
                showNewAlarm = false
                onAdd(time)
            },
        )
    }
}

@Composable
private fun NextAlarm(
    state: AlarmListUiState,
    timeFormatter: DateTimeFormatter,
) {
    val upcoming = state.upcoming
    val now = state.now
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (upcoming == null || now == null) {
            if (!state.loading) {
                TideAware { Text(stringResource(R.string.feature_alarm_none), style = MaterialTheme.typography.headlineMedium) }
            }
        } else {
            TideAware { underwater ->
                Text(
                    text = stringResource(R.string.feature_alarm_next, timeUntilText(timeUntil(now, upcoming.triggerAt))),
                    style = MaterialTheme.typography.labelLarge,
                    color = tideVariantColor(underwater),
                )
            }
            TideAware {
                Text(
                    text = timeFormatter.format(upcoming.triggerAt),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp, lineHeight = 96.sp),
                )
            }
            if (upcoming.alarm.label.isNotEmpty()) {
                TideAware { Text(upcoming.alarm.label, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    timeFormatter: DateTimeFormatter,
    locale: Locale,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            // The row is transparent over the beach, so the hint only shows while swiping.
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                TideAware(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(TideIcons.Delete), contentDescription = stringResource(R.string.feature_alarm_delete))
                    }
                }
            }
        },
    ) {
        TideAware { underwater ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = alarm.enabled, role = Role.Switch, onValueChange = { onToggle(alarm, it) })
                        .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = timeFormatter.format(alarm.time),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (alarm.enabled) LocalContentColor.current else tideVariantColor(underwater),
                    )
                    Text(
                        text = alarmDetails(alarm, locale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tideVariantColor(underwater),
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = null, colors = tideSwitchColors(underwater))
            }
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
            contentColor = if (underwater) colors.sea else colors.sand,
        ) {
            Icon(painterResource(TideIcons.Add), contentDescription = stringResource(R.string.feature_alarm_new))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewAlarmDialog(
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val pickerState = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
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
private fun tideSwitchColors(underwater: Boolean): SwitchColors {
    val colors = LocalTideColors.current
    val variant = tideVariantColor(underwater)
    return SwitchDefaults.colors(
        checkedTrackColor = if (underwater) colors.onSea else colors.accent,
        checkedThumbColor = if (underwater) colors.sea else colors.sand,
        uncheckedTrackColor = Color.Transparent,
        uncheckedThumbColor = variant,
        uncheckedBorderColor = variant,
    )
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
): String {
    fun short(day: DayOfWeek) = day.getDisplayName(TextStyle.SHORT, locale)

    val days =
        when (val label = repeatLabel(alarm.repeatDays)) {
            RepeatLabel.Once -> stringResource(R.string.feature_alarm_once)
            RepeatLabel.EveryDay -> stringResource(R.string.feature_alarm_every_day)
            is RepeatLabel.Range -> stringResource(R.string.feature_alarm_range, short(label.first), short(label.last))
            is RepeatLabel.Days -> label.days.joinToString(", ", transform = ::short)
        }
    return if (alarm.label.isEmpty()) days else stringResource(R.string.feature_alarm_with_label, days, alarm.label)
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
            onToggle = { _, _ -> },
            onDelete = {},
            onRestore = {},
        )
    }
}
