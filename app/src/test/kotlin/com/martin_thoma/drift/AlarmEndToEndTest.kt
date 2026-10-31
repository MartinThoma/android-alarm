package com.martin_thoma.drift

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.ringing.RingingService
import com.martin_thoma.drift.core.ringing.RingingState
import com.martin_thoma.drift.core.ringing.RingingStatus
import com.martin_thoma.drift.core.scheduling.ScheduleSync
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * The whole path of an alarm through the real modules: set it in the UI, find it scheduled in
 * the system's AlarmManager, let the system fire it, ring, dismiss.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class AlarmEndToEndTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var scheduleSync: ScheduleSync

    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var ringingState: RingingState

    @Inject lateinit var time: FakeTimeSource

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val alarmManager = application.getSystemService(AlarmManager::class.java)

    @Before
    fun setUp() {
        hilt.inject()
        // The real app starts this in DriftApplication, which tests replace.
        scheduleSync.start()
        time.now = ZonedDateTime.parse("2026-11-02T06:00:00+01:00[Europe/Berlin]")
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        ShadowMediaPlayer.setMediaInfoProvider { _: DataSource -> ShadowMediaPlayer.MediaInfo() }
    }

    private fun awaitUntil(condition: () -> Boolean) {
        repeat(300) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        error("Condition not met")
    }

    @Test
    fun `an alarm set in the app is scheduled, rings and can be dismissed`() {
        // 1. Set a one-time alarm for 07:00 (the time picker's default) in the UI.
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Tap + to set your first alarm.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("New alarm").performClick()
            compose.onNodeWithText("Set alarm").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Once").fetchSemanticsNodes().isNotEmpty() }
        }
        val alarm = runBlocking { repository.getAll() }.single()

        // 2. The system knows about it, as an alarm clock (shown in the status bar).
        val expected = ZonedDateTime.parse("2026-11-02T07:00:00+01:00[Europe/Berlin]").toInstant().toEpochMilli()
        awaitUntil { alarmManager.nextAlarmClock?.triggerTime == expected }
        val scheduled = shadowOf(alarmManager).peekNextScheduledAlarm()!!

        // 3. At 07:00 the system fires it; the receiver starts the ringing service.
        time.now = ZonedDateTime.parse("2026-11-02T07:00:00+01:00[Europe/Berlin]")
        shadowOf(alarmManager).fireAlarm(scheduled)
        var ringIntent: Intent? = null
        awaitUntil {
            ringIntent = ringIntent ?: shadowOf(application).nextStartedService
            ringIntent?.component?.className == RingingService::class.java.name
        }

        // 4. It rings; a one-time alarm turns itself off.
        val service = Robolectric.buildService(RingingService::class.java, ringIntent).create()
        service.get().onStartCommand(ringIntent, 0, 1)
        awaitUntil { ringingState.status.value is RingingStatus.Ringing }
        assertEquals(alarm.id, (ringingState.status.value as RingingStatus.Ringing).alarm.id)
        awaitUntil { runBlocking { repository.get(alarm.id) }?.enabled == false }

        // 5. Dismiss ends the ring.
        service.get().onStartCommand(Intent(RingingService.ACTION_DISMISS), 0, 2)
        assertEquals(RingingStatus.Idle, ringingState.status.value)
        assertFalse(runBlocking { repository.get(alarm.id) }!!.enabled)
    }
}
