package com.martin_thoma.drift.feature.alarm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.ringing.AlarmSounds
import com.martin_thoma.drift.core.ringing.CustomSound
import com.martin_thoma.drift.core.ringing.CustomSounds
import com.martin_thoma.drift.core.ringing.ImportResult
import com.martin_thoma.drift.core.ringing.PhoneSound
import com.martin_thoma.drift.core.ringing.PhoneSounds
import com.martin_thoma.drift.core.ringing.SoundPreview
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class AlarmSoundUiState(
    val alarm: Alarm? = null,
    val customSounds: List<CustomSound> = emptyList(),
    val phoneSounds: List<PhoneSound> = emptyList(),
    /** The sound playing as a preview, or `null`. */
    val previewing: String? = null,
    /** Why the file the user just picked was not added, until the message has been shown. */
    val importProblem: ImportResult? = null,
) {
    /** The alarm's sound; the default sound has its own uri here, so it can be compared. */
    val selected: String? get() = alarm?.let { it.soundUri ?: AlarmSounds.Default.uri }
}

@HiltViewModel(assistedFactory = AlarmSoundViewModel.Factory::class)
internal class AlarmSoundViewModel
    @AssistedInject
    constructor(
        @Assisted private val alarmId: Long,
        private val repository: AlarmRepository,
        private val customSounds: CustomSounds,
        private val phoneSounds: PhoneSounds,
        private val preview: SoundPreview,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(alarmId: Long): AlarmSoundViewModel
        }

        private val previewing = MutableStateFlow<String?>(null)
        private var previewEnd: Job? = null
        private val importProblem = MutableStateFlow<ImportResult?>(null)

        val uiState: StateFlow<AlarmSoundUiState> =
            combine(
                repository.alarms,
                customSounds.sounds,
                phoneSounds.sounds,
                previewing,
                importProblem,
            ) { alarms, custom, phone, playing, problem ->
                AlarmSoundUiState(
                    alarm = alarms.find { it.id == alarmId },
                    customSounds = custom,
                    phoneSounds = phone,
                    previewing = playing,
                    importProblem = problem,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AlarmSoundUiState())

        /** Chooses [soundUri] for the alarm and plays it; choosing the playing sound again stops it. */
        fun select(soundUri: String) {
            if (previewing.value == soundUri) {
                stopPreview()
                return
            }
            viewModelScope.launch {
                repository.setSound(alarmId, soundUri.takeUnless { it == AlarmSounds.Default.uri })
                phoneSounds.keep(soundUri)
                phoneSounds.forgetAllBut(repository.alarms.first().map { it.soundUri } + soundUri)
            }
            if (AlarmSounds.isSilent(soundUri)) stopPreview() else play(soundUri)
        }

        fun add(source: Uri) {
            viewModelScope.launch {
                when (val result = customSounds.add(source)) {
                    is ImportResult.Added -> select(result.sound.uri)
                    else -> importProblem.value = result
                }
            }
        }

        /** The message about [AlarmSoundUiState.importProblem] has been shown. */
        fun importProblemShown() {
            importProblem.value = null
        }

        /** Deletes the user's sound; an alarm that used it rings with the default sound again. */
        fun delete(sound: CustomSound) {
            if (previewing.value == sound.uri) stopPreview()
            viewModelScope.launch {
                if (uiState.value.selected == sound.uri) repository.setSound(alarmId, null)
                customSounds.delete(sound)
            }
        }

        fun stopPreview() {
            previewEnd?.cancel()
            preview.stop()
            previewing.value = null
        }

        private fun play(soundUri: String) {
            val alarm = uiState.value.alarm ?: return
            previewEnd?.cancel()
            preview.play(soundUri.takeUnless { it == AlarmSounds.Default.uri }, alarm.volumePercent)
            previewing.value = soundUri
            previewEnd =
                viewModelScope.launch {
                    delay(PREVIEW_MILLIS)
                    stopPreview()
                }
        }

        override fun onCleared() {
            preview.stop()
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val PREVIEW_MILLIS = 20_000L
        }
    }
