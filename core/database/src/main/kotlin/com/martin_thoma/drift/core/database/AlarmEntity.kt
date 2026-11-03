package com.martin_thoma.drift.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    /** Bit `n` set = repeats on ISO day `n + 1` (bit 0 = Monday). 0 = one-time alarm. */
    val repeatDays: Int,
    val enabled: Boolean,
    val label: String,
    val volumePercent: Int,
    val soundUri: String?,
    val vibrate: Boolean,
    val snoozeMinutes: Int,
    /** Epoch milliseconds; set while the alarm is snoozed. Added in version 2. */
    val snoozedUntil: Long? = null,
    /** Added in version 4. */
    val brightnessPercent: Int? = null,
    /** A [com.martin_thoma.drift.core.model.Blink] name. Added in version 4. */
    @ColumnInfo(defaultValue = "OFF") val blink: String = "OFF",
    /** A [com.martin_thoma.drift.core.model.Challenge] name. Added in version 5. */
    @ColumnInfo(defaultValue = "SLIDE") val challenge: String = "SLIDE",
)
