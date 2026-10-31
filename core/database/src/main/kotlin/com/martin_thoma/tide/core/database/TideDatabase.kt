package com.martin_thoma.tide.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = true)
abstract class TideDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}
