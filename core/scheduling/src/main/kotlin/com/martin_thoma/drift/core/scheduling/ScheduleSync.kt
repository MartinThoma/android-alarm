package com.martin_thoma.drift.core.scheduling

import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.data.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/** Reschedules whenever the alarms or timers in the database change. Started once by the Application. */
@Singleton
class ScheduleSync
    @Inject
    constructor(
        private val repository: AlarmRepository,
        private val scheduler: AlarmScheduler,
        private val timers: TimerRepository,
        private val timerScheduler: TimerScheduler,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        fun start() {
            repository.alarms.onEach(scheduler::schedule).launchIn(scope)
            timers.timers.onEach(timerScheduler::schedule).launchIn(scope)
        }
    }
