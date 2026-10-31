package com.martin_thoma.tide.feature.timer

import com.martin_thoma.tide.core.data.TimerRepository
import com.martin_thoma.tide.core.domain.TimeSource
import com.martin_thoma.tide.core.model.Timer
import com.martin_thoma.tide.core.ringing.RingingControls
import com.martin_thoma.tide.core.ringing.RingingState
import com.martin_thoma.tide.core.ringing.RingingStatus
import javax.inject.Inject

/**
 * What the timer screen and the timer notification can do. A timer that is ringing right now is
 * changed through the ringing service, so its sound stops too.
 */
class TimerActions
    @Inject
    constructor(
        private val repository: TimerRepository,
        private val timeSource: TimeSource,
        private val ringing: RingingState,
        private val controls: RingingControls,
    ) {
        fun now(): Long = timeSource.now().toInstant().toEpochMilli()

        suspend fun create(
            setMillis: Long,
            label: String,
        ): Long = repository.save(Timer(label = label.trim(), setMillis = setMillis, createdAt = now()).start(now()))

        suspend fun startOrPause(id: Long) = repository.update(id) { if (it.isRunning) it.pause(now()) else it.start(now()) }

        suspend fun addMinute(id: Long) {
            if (isRinging(id)) controls.addMinute() else repository.update(id) { it.addMinute(now()) }
        }

        suspend fun reset(id: Long) {
            if (isRinging(id)) controls.dismiss() else repository.update(id) { it.reset() }
        }

        suspend fun delete(id: Long) {
            if (isRinging(id)) controls.dismiss()
            repository.delete(id)
        }

        private fun isRinging(id: Long) = (ringing.status.value as? RingingStatus.TimerRinging)?.timer?.id == id
    }
