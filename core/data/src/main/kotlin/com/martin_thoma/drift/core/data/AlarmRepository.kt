package com.martin_thoma.drift.core.data

import com.martin_thoma.drift.core.model.Alarm
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The single source of truth for alarms. System alarms are always rebuilt from it. */
interface AlarmRepository {
    /** All alarms, sorted by time of day. */
    val alarms: Flow<List<Alarm>>

    suspend fun getAll(): List<Alarm>

    suspend fun get(id: Long): Alarm?

    /** Inserts a new alarm (id 0) or replaces the alarm with the same id. Returns the id. */
    suspend fun save(alarm: Alarm): Long

    /** Turns an alarm on or off. Either way, a pending snooze ends. */
    suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    )

    /** Sets the sound an alarm rings with; `null` is the default sound. */
    suspend fun setSound(
        id: Long,
        soundUri: String?,
    )

    /** Snoozes an alarm until [until], or ends its snooze if [until] is `null`. */
    suspend fun setSnoozedUntil(
        id: Long,
        until: Instant?,
    )

    suspend fun delete(id: Long)
}
