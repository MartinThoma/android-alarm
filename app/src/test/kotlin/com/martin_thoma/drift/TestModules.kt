package com.martin_thoma.drift

import android.content.Context
import androidx.room.Room
import com.martin_thoma.drift.core.database.AlarmDao
import com.martin_thoma.drift.core.database.DatabaseModule
import com.martin_thoma.drift.core.database.DriftDatabase
import com.martin_thoma.drift.core.database.TimerDao
import com.martin_thoma.drift.core.domain.TimeSource
import com.martin_thoma.drift.core.ringing.PhoneSound
import com.martin_thoma.drift.core.ringing.PhoneSoundSource
import com.martin_thoma.drift.core.ringing.PhoneSoundsModule
import com.martin_thoma.drift.core.scheduling.ApplicationScope
import com.martin_thoma.drift.core.scheduling.SchedulingModule
import com.martin_thoma.drift.feature.settings.PhoneSettings
import com.martin_thoma.drift.feature.settings.PhoneSettingsModule
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
    ): DriftDatabase = Room.inMemoryDatabaseBuilder(context, DriftDatabase::class.java).allowMainThreadQueries().build()

    @Provides
    fun alarmDao(database: DriftDatabase): AlarmDao = database.alarmDao()

    @Provides
    fun timerDao(database: DriftDatabase): TimerDao = database.timerDao()
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

/** A phone with two alarm tones and three ringtones. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PhoneSoundsModule::class])
object FakePhoneSoundsModule {
    private const val MEDIA = "content://media/internal/audio/media/"

    val sounds =
        listOf(
            PhoneSound(MEDIA + 1, "Morning Glory", PhoneSound.Kind.ALARM),
            PhoneSound(MEDIA + 2, "Sunrise", PhoneSound.Kind.ALARM),
            PhoneSound(MEDIA + 3, "Bells", PhoneSound.Kind.RINGTONE),
            PhoneSound(MEDIA + 4, "Over the Horizon", PhoneSound.Kind.RINGTONE),
            PhoneSound(MEDIA + 5, "Pulse", PhoneSound.Kind.RINGTONE),
        )

    @Provides
    fun source(): PhoneSoundSource = PhoneSoundSource { sounds }
}

/** A phone that allows everything, until a test says otherwise. Robolectric cannot answer some of these. */
class FakePhoneSettings : PhoneSettings {
    override var notificationsEnabled = true
    override var ringingChannelImportance: Int? = null
    override var fullScreenAllowed = true
    override var exactAlarmsAllowed = true
    override var backgroundRestricted = false
    override var batteryOptimised = false
    override var samsung = false
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PhoneSettingsModule::class])
object FakePhoneSettingsModule {
    @Provides
    @Singleton
    fun fake(): FakePhoneSettings = FakePhoneSettings()

    @Provides
    fun phoneSettings(fake: FakePhoneSettings): PhoneSettings = fake
}
