package com.martin_thoma.drift

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.ringing.RingingService
import com.martin_thoma.drift.core.ringing.RingingState
import com.martin_thoma.drift.core.ringing.RingingStatus
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/** The ringing service with the real Room repository, notifications and audio manager. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class RingingTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var ringingState: RingingState

    @Inject lateinit var time: FakeTimeSource

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var startId = 0

    @Before
    fun setUp() {
        hilt.inject()
        // Every sound "plays" in Robolectric, the bundled chime included.
        ShadowMediaPlayer.setMediaInfoProvider { _: DataSource -> ShadowMediaPlayer.MediaInfo() }
        time.now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 2, 0)
    }

    private fun command(
        service: ServiceController<RingingService>,
        intent: Intent,
    ) {
        service.get().onStartCommand(intent, 0, ++startId)
    }

    /** Room runs off the main thread; let both sides finish until [condition] holds. */
    private fun awaitUntil(condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        error("Condition not met")
    }

    private fun ring(alarm: Alarm): Pair<ServiceController<RingingService>, Long> {
        val id = runBlocking { repository.save(alarm) }
        val service = Robolectric.buildService(RingingService::class.java).create()
        command(service, Intent(RingingService.ACTION_RING).putExtra("com.martin_thoma.drift.extra.ALARM_ID", id))
        awaitUntil { ringingState.status.value is RingingStatus.Ringing }
        return service to id
    }

    @Test
    fun `an alarm rings in the foreground at its own volume`() {
        val (service, _) = ring(Alarm(time = LocalTime.of(6, 30), volumePercent = 100, label = "Work"))

        assertEquals(audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
        assertTrue(shadowOf(service.get()).isLastForegroundNotificationAttached)
        // The foreground notification, updated with the alarm's label once it has loaded.
        val notification = context.getSystemService(NotificationManager::class.java).activeNotifications.single { it.id == 1 }
        assertEquals("Work", notification.notification.extras.getString("android.title"))
    }

    @Test
    fun `dismiss ends the ring and restores the user's volume`() {
        val (service, _) = ring(Alarm(time = LocalTime.of(6, 30), volumePercent = 100))

        command(service, Intent(RingingService.ACTION_DISMISS))

        assertEquals(RingingStatus.Idle, ringingState.status.value)
        assertEquals(2, audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
        assertTrue(shadowOf(service.get()).isStoppedBySelf)
    }

    @Test
    fun `snooze stores the snooze and shows when it rings again`() {
        val (service, id) = ring(Alarm(time = LocalTime.of(6, 30), snoozeMinutes = 5))

        command(service, Intent(RingingService.ACTION_SNOOZE))
        awaitUntil { runBlocking { repository.get(id) }?.snoozedUntil != null }

        assertEquals(Instant.parse("2026-11-02T05:35:00Z"), runBlocking { repository.get(id) }?.snoozedUntil)
        assertEquals(RingingStatus.Idle, ringingState.status.value)
        assertEquals(2, audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
        awaitUntil { context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.tag == "snoozed:$id" } }
    }
}
