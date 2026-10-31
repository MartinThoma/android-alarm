package com.martin_thoma.tide

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.martin_thoma.tide.core.data.TimerRepository
import com.martin_thoma.tide.core.ringing.RingingService
import com.martin_thoma.tide.core.ringing.RingingState
import com.martin_thoma.tide.core.ringing.RingingStatus
import com.martin_thoma.tide.core.scheduling.ScheduleSync
import com.martin_thoma.tide.feature.ringing.RingingActivity
import com.martin_thoma.tide.feature.timer.TimerNotifier
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject

/** A timer through the real modules (requirements TM-01 to TM-07), with the screenshots for the README. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class TimerTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var scheduleSync: ScheduleSync

    @Inject lateinit var notifier: TimerNotifier

    @Inject lateinit var repository: TimerRepository

    @Inject lateinit var ringingState: RingingState

    @Inject lateinit var time: FakeTimeSource

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val alarmManager = application.getSystemService(AlarmManager::class.java)
    private val notifications = application.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        hilt.inject()
        scheduleSync.start()
        notifier.start()
        Settings.Global.putFloat(application.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        ShadowMediaPlayer.setMediaInfoProvider { _: DataSource -> ShadowMediaPlayer.MediaInfo() }
        time.now = ZonedDateTime.parse("2026-11-02T16:00:00+01:00[Europe/Berlin]")
    }

    private fun nowMillis() = time.now.toInstant().toEpochMilli()

    private fun awaitUntil(condition: () -> Boolean) {
        repeat(300) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        error("Condition not met")
    }

    @Test
    fun `a timer counts down, rings when it runs out, and plus one minute restarts it`() {
        val start = nowMillis()
        ActivityScenario.launch(MainActivity::class.java).use {
            // 1. With no timers, the timer tab opens on the number pad: 1-0-0-0 is 10:00.
            compose.onNodeWithText("Timer").performClick()
            listOf("1", "0", "0", "0").forEach { compose.onNodeWithText(it).performClick() }
            compose.onNode(hasSetTextAction()).performTextInput("Tea")
            compose.onRoot().captureRoboImage("../docs/screenshots/timer-new.png")
            compose.onNodeWithContentDescription("Start").performClick()
            awaitUntil { runBlocking { repository.getAll() }.isNotEmpty() }

            // 2. A few minutes later the tide has come in a little.
            time.now = time.now.plus(Duration.ofSeconds(207))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            compose.waitUntil(5_000) { compose.onAllNodesWithText("06:33").fetchSemanticsNodes().isNotEmpty() }
            compose.onRoot().captureRoboImage("../docs/screenshots/timer.png")
        }
        val timer = runBlocking { repository.getAll() }.single()
        assertEquals("Tea", timer.label)
        assertEquals(start + 600_000, timer.endsAt)

        // 3. The system will wake the phone when it runs out, and the notification counts down by itself.
        awaitUntil { shadowOf(alarmManager).peekNextScheduledAlarm()?.triggerAtMs == timer.endsAt }
        awaitUntil { notifications.activeNotifications.any { it.tag == "timer:${timer.id}" } }
        val running = notifications.activeNotifications.single { it.tag == "timer:${timer.id}" }.notification
        assertTrue(running.extras.getBoolean("android.chronometerCountDown"))
        assertEquals(listOf("Pause", "+1 min", "Reset"), running.actions.map { it.title.toString() })

        // 4. At the end it rings like an alarm.
        time.now = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(timer.endsAt!! + 12_000), time.now.zone)
        shadowOf(alarmManager).fireAlarm(shadowOf(alarmManager).peekNextScheduledAlarm()!!)
        var ringIntent: Intent? = null
        awaitUntil {
            ringIntent = ringIntent ?: shadowOf(application).nextStartedService
            ringIntent?.action == RingingService.ACTION_RING_TIMER
        }
        val service = Robolectric.buildService(RingingService::class.java, ringIntent).create()
        service.get().onStartCommand(ringIntent, 0, 1)
        awaitUntil { ringingState.status.value is RingingStatus.TimerRinging }
        ActivityScenario.launch(RingingActivity::class.java).use {
            compose.waitUntil(5_000) { compose.onAllNodesWithText("+00:12").fetchSemanticsNodes().isNotEmpty() }
            compose.onRoot().captureRoboImage("../docs/screenshots/timer-ringing.png")
        }

        // 5. "+1 min" stops the sound and runs the timer for another minute.
        service.get().onStartCommand(Intent(RingingService.ACTION_ADD_MINUTE), 0, 2)
        awaitUntil { runBlocking { repository.get(timer.id) }?.endsAt == nowMillis() + 60_000 }
        assertEquals(RingingStatus.Idle, ringingState.status.value)
    }
}
