package com.martin_thoma.tide.core.ringing

import com.martin_thoma.tide.core.model.Alarm
import com.martin_thoma.tide.core.model.Timer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What [RingingService] is doing. The ringing screen follows it and closes on [Idle]. */
sealed interface RingingStatus {
    data object Idle : RingingStatus

    /** An alarm fired and its settings are loading. */
    data object Starting : RingingStatus

    data class Ringing(
        val alarm: Alarm,
        val since: Instant,
    ) : RingingStatus

    /** A timer has run out (requirements TM-06). */
    data class TimerRinging(
        val timer: Timer,
        val since: Instant,
    ) : RingingStatus
}

@Singleton
class RingingState
    @Inject
    constructor() {
        private val mutableStatus = MutableStateFlow<RingingStatus>(RingingStatus.Idle)
        val status: StateFlow<RingingStatus> = mutableStatus.asStateFlow()

        /** Only [RingingService] changes the status; tests may set it directly. */
        fun update(status: RingingStatus) {
            mutableStatus.value = status
        }
    }
