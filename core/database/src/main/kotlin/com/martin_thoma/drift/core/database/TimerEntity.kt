package com.martin_thoma.drift.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A countdown timer; all times in epoch milliseconds. Added in version 3. */
@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val setMillis: Long,
    val durationMillis: Long,
    val remainingMillis: Long,
    val endsAt: Long?,
    val createdAt: Long,
)
