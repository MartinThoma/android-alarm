package com.martin_thoma.tide.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class TideDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}
