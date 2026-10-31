package com.martin_thoma.tide.core.data

import com.martin_thoma.tide.core.model.Alarm
import kotlinx.coroutines.flow.Flow

/** The single source of truth for alarms. System alarms are always rebuilt from it. */
interface AlarmRepository {
    /** All alarms, sorted by time of day. */
    val alarms: Flow<List<Alarm>>

    suspend fun getAll(): List<Alarm>

    suspend fun get(id: Long): Alarm?

    /** Inserts a new alarm (id 0) or replaces the alarm with the same id. Returns the id. */
    suspend fun save(alarm: Alarm): Long

    suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    )

    suspend fun delete(id: Long)
}
