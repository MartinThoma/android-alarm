package com.martin_thoma.drift.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * Device-protected storage is readable before the user unlocks the phone after a reboot,
     * so alarms can ring after an overnight update (requirements AL-18, section 6.2).
     */
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): DriftDatabase =
        Room
            .databaseBuilder(context.createDeviceProtectedStorageContext(), DriftDatabase::class.java, "drift.db")
            .build()

    @Provides
    fun alarmDao(database: DriftDatabase): AlarmDao = database.alarmDao()

    @Provides
    fun timerDao(database: DriftDatabase): TimerDao = database.timerDao()
}
