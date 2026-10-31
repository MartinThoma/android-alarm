package com.martin_thoma.tide.core.scheduling

import com.martin_thoma.tide.core.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/** Reschedules whenever the alarms in the database change. Started once by the Application. */
@Singleton
class ScheduleSync
    @Inject
    constructor(
        private val repository: AlarmRepository,
        private val scheduler: AlarmScheduler,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        fun start() {
            repository.alarms.onEach(scheduler::schedule).launchIn(scope)
        }
    }
