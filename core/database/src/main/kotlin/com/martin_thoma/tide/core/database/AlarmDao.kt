package com.martin_thoma.tide.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun get(id: Long): AlarmEntity?

    /** Returns the new row id on insert, -1 on update. */
    @Upsert
    suspend fun upsert(alarm: AlarmEntity): Long

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    )

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun delete(id: Long)
}
