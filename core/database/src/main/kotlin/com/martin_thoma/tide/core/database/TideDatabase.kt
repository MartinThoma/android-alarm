package com.martin_thoma.tide.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmEntity::class, TimerEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class TideDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun timerDao(): TimerDao
}
