package com.martin_thoma.drift.feature.alarm

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.drift.core.designsystem.GlassPane
import com.martin_thoma.drift.core.designsystem.LocalTideColors
import com.martin_thoma.drift.core.designsystem.TideAware
import com.martin_thoma.drift.core.designsystem.TideIcons
import com.martin_thoma.drift.core.designsystem.TideLevel
import com.martin_thoma.drift.core.designsystem.TideSurface
import com.martin_thoma.drift.core.designsystem.keepOnOpenWater
import com.martin_thoma.drift.core.designsystem.rememberTimeFormatter
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.ringing.AlarmSounds
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle

@Serializable
data class AlarmEditorRoute(
    val alarmId: Long,
) : NavKey

/** Everything about one alarm. Changes are saved at once, like a switch in the list. */
@Composable
fun AlarmEditorScreen(
    alarmId: Long,
    onBack: () -> Unit,
    onChooseSound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<AlarmEditorViewModel, AlarmEditorViewModel.Factory>(key = "editor-$alarmId") { it.create(alarmId) }
    val alarm by viewModel.alarm.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stopPreview() }
    AlarmEditorContent(
        alarm = alarm ?: return,
        onBack = onBack,
        onDelete = {
            viewModel.delete()
            onBack()
        },
        onTime = viewModel::setTime,
        onToggleDay = viewModel::toggleDay,
        onLabel = viewModel::setLabel,
        onChooseSound = onChooseSound,
        onVolume = viewModel::setVolume,
        onPreviewVolume = viewModel::previewVolume,
        onVibrate = viewModel::setVibrate,
        onSnoozeMinutes = viewModel::setSnoozeMinutes,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmEditorContent(
    alarm: Alarm,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onTime: (LocalTime) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onLabel: (String) -> Unit,
    onChooseSound: () -> Unit,
    onVolume: (Int) -> Unit,
    onPreviewVolume: (Int) -> Unit,
    onVibrate: (Boolean) -> Unit,
    onSnoozeMinutes: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val timeFormatter = rememberTimeFormatter()
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    var editingTime by rememberSaveable { mutableStateOf(false) }

    // The tide only sets the scene here: just high enough that the time sits on open water.
    var level by remember { mutableFloatStateOf(TideLevel.LOW) }
    TideSurface(level = level, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = colors.onSand,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.feature_alarm_edit_title)) },
                    // Delete on the left, done on the right, as in Android's edit screens. Changes are
                    // saved as they are made; the check mark confirms and closes (Martin, on the S25).
                    navigationIcon = {
                        IconButton(onClick = onDelete) {
                            Icon(painterResource(TideIcons.Delete), contentDescription = stringResource(R.string.feature_alarm_delete))
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(TideIcons.Check), contentDescription = stringResource(R.string.feature_alarm_done))
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = colors.onSand,
                            navigationIconContentColor = colors.onSand,
                            actionIconContentColor = colors.onSand,
                        ),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "time") {
                    TideAware(
                        Modifier
                            .keepOnOpenWater { level = it }
                            .clickable(onClickLabel = stringResource(R.string.feature_alarm_change_time)) { editingTime = true }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                    ) {
                        Text(
                            timeFormatter.format(alarm.time),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, lineHeight = 96.sp),
                        )
                    }
                }
                item(key = "days") { RepeatDays(alarm.repeatDays, onToggleDay, Modifier.padding(top = 16.dp, bottom = 24.dp)) }
                item(key = "settings") {
                    GlassPane(Modifier.fillMaxWidth()) {
                        Column {
                            LabelField(alarm.label, onLabel)
                            PaneDivider()
                            SoundRow(alarm.soundUri, onChooseSound)
                            PaneDivider()
                            VolumeRow(alarm.volumePercent, onVolume, onPreviewVolume)
                            PaneDivider()
                            VibrateRow(alarm.vibrate, onVibrate)
                            PaneDivider()
                            SnoozeRow(alarm.snoozeMinutes, onSnoozeMinutes)
                        }
                    }
                }
            }
        }
    }

    if (editingTime) {
        AlarmTimeDialog(
            is24Hour = is24Hour,
            initial = alarm.time,
            onDismiss = { editingTime = false },
            onConfirm = {
                editingTime = false
                onTime(it)
            },
        )
    }
}

/** Seven round toggles, Monday first until the week-start setting exists (requirements GN-02). */
@Composable
private fun RepeatDays(
    days: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (days.isEmpty()) stringResource(R.string.feature_alarm_once) else stringResource(R.string.feature_alarm_repeat),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSandVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                val selected = day in days
                val name = day.getDisplayName(TextStyle.FULL, locale)
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .background(if (selected) colors.accent else colors.glass, CircleShape)
                            .border(1.dp, if (selected) colors.accent else colors.glassRim, CircleShape)
                            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle(day) })
                            .semantics { contentDescription = name },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.onAccent else colors.onGlass,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelField(
    label: String,
    onLabel: (String) -> Unit,
) {
    val colors = LocalTideColors.current
    // Typed text stays local, so the cursor does not jump while the database catches up.
    var text by remember { mutableStateOf(label) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onLabel(it)
        },
        label = { Text(stringResource(R.string.feature_alarm_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.onGlassVariant.copy(alpha = 0.5f),
                focusedLabelColor = colors.accent,
            ),
    )
}

@Composable
private fun SoundRow(
    soundUri: String?,
    onClick: () -> Unit,
) {
    val title = AlarmSounds.builtIn(soundUri)?.let { stringResource(it.title) } ?: AlarmSounds.custom(soundUri)?.title.orEmpty()
    SettingRow(title = stringResource(R.string.feature_alarm_sound), modifier = Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = LocalTideColors.current.onGlassVariant)
    }
}

@Composable
private fun VolumeRow(
    percent: Int,
    onVolume: (Int) -> Unit,
    onPreview: (Int) -> Unit,
) {
    val colors = LocalTideColors.current
    val locale = LocalConfiguration.current.locales[0]
    // The slider moves freely; the alarm is saved, and a sample played, when the finger lifts.
    var value by remember(percent) { mutableFloatStateOf(percent.toFloat()) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.feature_alarm_volume), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                NumberFormat.getPercentInstance(locale).format(value / 100.0),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onGlassVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = {
                onVolume(value.toInt())
                onPreview(value.toInt())
            },
            valueRange = 0f..100f,
            colors =
                SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.onGlass.copy(alpha = 0.15f),
                ),
        )
    }
}

@Composable
private fun VibrateRow(
    vibrate: Boolean,
    onVibrate: (Boolean) -> Unit,
) {
    val colors = LocalTideColors.current
    SettingRow(
        title = stringResource(R.string.feature_alarm_vibrate),
        modifier = Modifier.toggleable(value = vibrate, role = Role.Switch, onValueChange = onVibrate),
    ) {
        Switch(
            checked = vibrate,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = colors.accent,
                    checkedThumbColor = colors.onAccent,
                    uncheckedTrackColor = Color.Transparent,
                    uncheckedThumbColor = colors.onGlassVariant,
                    uncheckedBorderColor = colors.onGlassVariant,
                ),
        )
    }
}

@Composable
private fun SnoozeRow(
    minutes: Int,
    onMinutes: (Int) -> Unit,
) {
    SettingRow(title = stringResource(R.string.feature_alarm_snooze)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", stringResource(R.string.feature_alarm_snooze_shorter), enabled = minutes > 1) {
                onMinutes(minutes - snoozeStep(minutes, down = true))
            }
            Text(
                stringResource(R.string.feature_alarm_snooze_minutes, minutes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            StepButton("+", stringResource(R.string.feature_alarm_snooze_longer), enabled = minutes < 30) {
                onMinutes(minutes + snoozeStep(minutes, down = false))
            }
        }
    }
}

/** One minute at a time up to five minutes, then five at a time: 1, 2, … 5, 10, 15 … 30. */
private fun snoozeStep(
    minutes: Int,
    down: Boolean,
): Int = if (minutes < 5 || (down && minutes == 5)) 1 else 5

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = description }) {
        Text(symbol, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun PaneDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = LocalContentColor.current.copy(alpha = 0.12f))
}
