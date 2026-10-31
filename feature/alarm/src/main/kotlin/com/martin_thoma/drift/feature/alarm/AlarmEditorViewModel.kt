package com.martin_thoma.drift.feature.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.model.Blink
import com.martin_thoma.drift.core.ringing.SoundPreview
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalTime

@HiltViewModel(assistedFactory = AlarmEditorViewModel.Factory::class)
internal class AlarmEditorViewModel
    @AssistedInject
    constructor(
        @Assisted private val alarmId: Long,
        private val repository: AlarmRepository,
        private val preview: SoundPreview,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(alarmId: Long): AlarmEditorViewModel
        }

        /** `null` while loading, and once the alarm is deleted. */
        val alarm: StateFlow<Alarm?> =
            repository.alarms
                .map { alarms -> alarms.find { it.id == alarmId } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        private var previewEnd: Job? = null

        /** A new time or new days turn the alarm on: that is what the user is setting it up for. */
        fun setTime(time: LocalTime) = change { it.copy(time = time, enabled = true) }

        fun toggleDay(day: DayOfWeek) =
            change {
                it.copy(
                    repeatDays =
                        if (day in
                            it.repeatDays
                        ) {
                            it.repeatDays - day
                        } else {
                            it.repeatDays + day
                        },
                    enabled = true,
                )
            }

        fun setLabel(label: String) = change { it.copy(label = label.take(MAX_LABEL)) }

        fun setVibrate(vibrate: Boolean) = change { it.copy(vibrate = vibrate) }

        fun setSnoozeMinutes(minutes: Int) = change { it.copy(snoozeMinutes = minutes.coerceIn(1, MAX_SNOOZE)) }

        fun setVolume(percent: Int) = change { it.copy(volumePercent = percent.coerceIn(0, 100)) }

        /** `null` keeps the phone's own brightness on the ringing screen. */
        fun setBrightness(percent: Int?) = change { it.copy(brightnessPercent = percent?.coerceIn(1, 100)) }

        fun setBlink(blink: Blink) = change { it.copy(blink = blink) }

        /** A short sample at [percent], exactly as loud as the alarm will ring (requirements AL-06). */
        fun previewVolume(percent: Int) {
            val current = alarm.value ?: return
            previewEnd?.cancel()
            preview.play(current.soundUri, percent)
            previewEnd =
                viewModelScope.launch {
                    delay(PREVIEW_MILLIS)
                    preview.stop()
                }
        }

        fun stopPreview() {
            previewEnd?.cancel()
            preview.stop()
        }

        fun delete() {
            stopPreview()
            viewModelScope.launch { repository.delete(alarmId) }
        }

        private val writes = Mutex()

        /**
         * Applies [transform] to the alarm as stored, one change after the other, so quick taps
         * (Monday, Tuesday, …) all count instead of overwriting each other.
         */
        private fun change(transform: (Alarm) -> Alarm) {
            viewModelScope.launch {
                writes.withLock {
                    val current = repository.get(alarmId) ?: return@withLock
                    repository.save(transform(current))
                }
            }
        }

        override fun onCleared() {
            preview.stop()
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val PREVIEW_MILLIS = 4_000L
            const val MAX_LABEL = 40
            const val MAX_SNOOZE = 30
        }
    }
