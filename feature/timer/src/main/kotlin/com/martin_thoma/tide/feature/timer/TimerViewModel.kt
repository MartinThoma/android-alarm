package com.martin_thoma.tide.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martin_thoma.tide.core.data.RecentTimer
import com.martin_thoma.tide.core.data.RecentTimers
import com.martin_thoma.tide.core.data.TimerRepository
import com.martin_thoma.tide.core.model.Timer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TimerViewModel
    @Inject
    constructor(
        repository: TimerRepository,
        recentTimers: RecentTimers,
        private val actions: TimerActions,
    ) : ViewModel() {
        val recents: StateFlow<List<RecentTimer>> = recentTimers.recents

        /** `null` while loading. */
        val timers: StateFlow<List<Timer>?> =
            repository.timers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        fun now(): Long = actions.now()

        fun create(
            setMillis: Long,
            label: String,
        ) {
            viewModelScope.launch { actions.create(setMillis, label) }
        }

        fun startOrPause(timer: Timer) = launch { actions.startOrPause(timer.id) }

        fun addMinute(timer: Timer) = launch { actions.addMinute(timer.id) }

        fun reset(timer: Timer) = launch { actions.reset(timer.id) }

        fun delete(timer: Timer) = launch { actions.delete(timer.id) }

        private fun launch(block: suspend () -> Unit) {
            viewModelScope.launch { block() }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
