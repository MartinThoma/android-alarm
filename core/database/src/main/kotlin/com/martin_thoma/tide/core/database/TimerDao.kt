package com.martin_thoma.tide.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY createdAt, id")
    fun observeAll(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers")
    suspend fun getAll(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun get(id: Long): TimerEntity?

    /** Returns the new row id on insert, -1 on update. */
    @Upsert
    suspend fun upsert(timer: TimerEntity): Long

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun delete(id: Long)
}
