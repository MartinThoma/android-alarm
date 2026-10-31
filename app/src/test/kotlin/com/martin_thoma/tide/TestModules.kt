package com.martin_thoma.tide

import android.content.Context
import androidx.room.Room
import com.martin_thoma.tide.core.database.AlarmDao
import com.martin_thoma.tide.core.database.DatabaseModule
import com.martin_thoma.tide.core.database.TideDatabase
import com.martin_thoma.tide.core.domain.TimeSource
import com.martin_thoma.tide.core.scheduling.ApplicationScope
import com.martin_thoma.tide.core.scheduling.SchedulingModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.time.ZonedDateTime
import javax.inject.Singleton

/** A clock the test sets. */
class FakeTimeSource : TimeSource {
    var now: ZonedDateTime = ZonedDateTime.parse("2026-11-01T23:18:00+01:00[Europe/Berlin]")

    override fun now(): ZonedDateTime = now
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object InMemoryDatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): TideDatabase = Room.inMemoryDatabaseBuilder(context, TideDatabase::class.java).allowMainThreadQueries().build()

    @Provides
    fun alarmDao(database: TideDatabase): AlarmDao = database.alarmDao()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SchedulingModule::class])
object FakeTimeModule {
    @Provides
    @Singleton
    fun fakeTimeSource(): FakeTimeSource = FakeTimeSource()

    @Provides
    fun timeSource(fake: FakeTimeSource): TimeSource = fake

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())
}
