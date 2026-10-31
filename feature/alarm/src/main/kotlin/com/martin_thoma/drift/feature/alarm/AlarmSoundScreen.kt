package com.martin_thoma.drift.feature.alarm

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.drift.core.designsystem.GlassPane
import com.martin_thoma.drift.core.designsystem.LocalTideColors
import com.martin_thoma.drift.core.designsystem.TideIcons
import com.martin_thoma.drift.core.designsystem.TideLevel
import com.martin_thoma.drift.core.designsystem.TideSurface
import com.martin_thoma.drift.core.designsystem.rememberTimeFormatter
import com.martin_thoma.drift.core.ringing.AlarmSounds
import com.martin_thoma.drift.core.ringing.CustomSound
import com.martin_thoma.drift.core.ringing.ImportResult
import kotlinx.serialization.Serializable

@Serializable
data class AlarmSoundRoute(
    val alarmId: Long,
) : NavKey

/** Chooses an alarm's sound, grouped by kind, with a preview at the alarm's volume (requirements 8). */
@Composable
fun AlarmSoundScreen(
    alarmId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<AlarmSoundViewModel, AlarmSoundViewModel.Factory>(key = "sound-$alarmId") { it.create(alarmId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // A preview never plays on behind another screen, the ringing screen included.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stopPreview() }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let(viewModel::add) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tooLarge = stringResource(R.string.feature_alarm_sound_too_large)
    val notPlayable = stringResource(R.string.feature_alarm_sound_not_playable)
    LaunchedEffect(viewModel) {
        viewModel.importProblems.collect { problem ->
            snackbarHostState.showSnackbar(if (problem == ImportResult.TooLarge) tooLarge else notPlayable)
        }
    }
    AlarmSoundContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelect = viewModel::select,
        onAdd = { pickFile.launch(arrayOf("audio/*")) },
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmSoundContent(
    state: AlarmSoundUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (CustomSound) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val timeFormatter = rememberTimeFormatter()

    TideSurface(level = TideLevel.LOW, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = colors.onSand,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.feature_alarm_sound_title))
                            state.alarm?.let { alarm ->
                                Text(
                                    text =
                                        listOf(timeFormatter.format(alarm.time), alarm.label)
                                            .filter { it.isNotEmpty() }
                                            .joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSandVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(TideIcons.ArrowBack), contentDescription = stringResource(R.string.feature_alarm_back))
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = colors.onSand,
                            navigationIconContentColor = colors.onSand,
                        ),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    SoundGroup.DEFAULT,
                    SoundGroup.NATURE,
                    SoundGroup.NOISY,
                    SoundGroup.CLASSICS,
                ).forEach { soundGroup(it, state, onSelect) }
                item(key = "custom") {
                    GroupHeading(SoundGroup.MINE)
                    GlassPane(Modifier.fillMaxWidth()) {
                        Column {
                            state.customSounds.forEach { sound ->
                                SoundRow(
                                    title = sound.title,
                                    selected = state.selected == sound.uri,
                                    playing = state.previewing == sound.uri,
                                    onClick = { onSelect(sound.uri) },
                                    trailing = {
                                        IconButton(onClick = { onDelete(sound) }) {
                                            Icon(
                                                painterResource(TideIcons.Delete),
                                                contentDescription = stringResource(R.string.feature_alarm_sound_delete, sound.title),
                                                tint = colors.onGlassVariant,
                                            )
                                        }
                                    },
                                )
                                PaneDivider()
                            }
                            AddSoundRow(onAdd)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.soundGroup(
    group: SoundGroup,
    state: AlarmSoundUiState,
    onSelect: (String) -> Unit,
) {
    val sounds = AlarmSounds.builtIn.filter { it.category == group.category }
    item(key = group.name) {
        GroupHeading(group)
        GlassPane(Modifier.fillMaxWidth()) {
            Column {
                sounds.forEachIndexed { index, sound ->
                    if (index > 0) PaneDivider()
                    SoundRow(
                        title = stringResource(sound.title),
                        selected = state.selected == sound.uri,
                        playing = state.previewing == sound.uri,
                        onClick = { onSelect(sound.uri) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeading(group: SoundGroup) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decorative: the heading names the group for TalkBack.
        SoundGroupArt(group)
        Text(text = stringResource(group.heading), style = MaterialTheme.typography.titleMedium, color = LocalTideColors.current.onSand)
    }
}

@Composable
private fun PaneDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = LocalContentColor.current.copy(alpha = 0.12f))
}

@Composable
private fun SoundRow(
    title: String,
    selected: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalTideColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.padding(12.dp),
            colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.onGlassVariant),
        )
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (playing) {
            Icon(
                painterResource(TideIcons.VolumeUp),
                contentDescription = stringResource(R.string.feature_alarm_sound_playing),
                tint = colors.accent,
                modifier = Modifier.padding(horizontal = 12.dp).size(20.dp),
            )
        }
        trailing()
    }
}

@Composable
private fun AddSoundRow(onClick: () -> Unit) {
    val colors = LocalTideColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(TideIcons.Add), contentDescription = null, tint = colors.accent)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.feature_alarm_sound_add), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.feature_alarm_sound_add_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onGlassVariant,
            )
        }
    }
}
