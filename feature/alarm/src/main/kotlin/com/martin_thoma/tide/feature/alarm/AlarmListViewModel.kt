package com.martin_thoma.tide.feature.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martin_thoma.tide.core.data.AlarmRepository
import com.martin_thoma.tide.core.domain.NextTrigger
import com.martin_thoma.tide.core.domain.TimeSource
import com.martin_thoma.tide.core.domain.UpcomingAlarm
import com.martin_thoma.tide.core.model.Alarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

internal data class AlarmListUiState(
    val loading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    val upcoming: UpcomingAlarm? = null,
    val now: ZonedDateTime? = null,
)

@HiltViewModel
internal class AlarmListViewModel
    @Inject
    constructor(
        private val repository: AlarmRepository,
        private val timeSource: TimeSource,
    ) : ViewModel() {
        /** Ticks at the start of every minute, so "in 7 h 12 min" stays current. */
        private val minuteTicks: Flow<Unit> =
            flow {
                while (true) {
                    emit(Unit)
                    val now = timeSource.now()
                    delay(ChronoUnit.MILLIS.between(now, now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)))
                }
            }

        val uiState: StateFlow<AlarmListUiState> =
            combine(repository.alarms, minuteTicks) { alarms, _ ->
                val now = timeSource.now()
                AlarmListUiState(loading = false, alarms = alarms, upcoming = NextTrigger.upcoming(alarms, now), now = now)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AlarmListUiState())

        fun add(time: LocalTime) {
            viewModelScope.launch { repository.save(Alarm(time = time)) }
        }

        fun setEnabled(
            alarm: Alarm,
            enabled: Boolean,
        ) {
            viewModelScope.launch { repository.setEnabled(alarm.id, enabled) }
        }

        fun delete(alarm: Alarm) {
            viewModelScope.launch { repository.delete(alarm.id) }
        }

        /** Undo for [delete]: puts the alarm back with its id and settings. */
        fun restore(alarm: Alarm) {
            viewModelScope.launch { repository.save(alarm) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
