package com.martin_thoma.drift.feature.stopwatch

import androidx.lifecycle.ViewModel
import com.martin_thoma.drift.core.data.StopwatchRepository
import com.martin_thoma.drift.core.domain.TimeSource
import com.martin_thoma.drift.core.model.Stopwatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class StopwatchViewModel
    @Inject
    constructor(
        private val repository: StopwatchRepository,
        private val timeSource: TimeSource,
    ) : ViewModel() {
        val stopwatch: StateFlow<Stopwatch> = repository.stopwatch

        fun now(): Long = timeSource.now().toInstant().toEpochMilli()

        fun startOrPause() = repository.update { if (it.isRunning) it.pause(now()) else it.start(now()) }

        fun lap() = repository.update { it.lap(now()) }

        fun reset() = repository.update { it.reset() }
    }
