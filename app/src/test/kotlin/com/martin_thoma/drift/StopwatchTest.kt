package com.martin_thoma.drift

import android.app.NotificationManager
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.martin_thoma.drift.core.data.StopwatchRepository
import com.martin_thoma.drift.feature.stopwatch.StopwatchNotifier
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZonedDateTime
import javax.inject.Inject

/** The stopwatch, operated through the screen and through its notification (requirements SW-01 to SW-04). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class StopwatchTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var repository: StopwatchRepository

    @Inject lateinit var notifier: StopwatchNotifier

    @Inject lateinit var time: FakeTimeSource

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val notifications = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        hilt.inject()
        notifier.start()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        time.now = ZonedDateTime.parse("2026-11-02T07:00:00+01:00[Europe/Berlin]")
    }

    private fun advance(millis: Long) {
        time.now = time.now.plusNanos(millis * 1_000_000)
    }

    private fun stopwatchNotification() = notifications.activeNotifications.singleOrNull { it.tag == "stopwatch" }?.notification

    @Test
    fun `start, laps, pause, and reset from the notification`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Stopwatch").performClick()
            compose.onNodeWithContentDescription("Start").performClick()
            advance(61_230)
            compose.onNodeWithContentDescription("Lap").performClick()
            advance(57_670)
            compose.onNodeWithContentDescription("Lap").performClick()
            advance(66_510)
            compose.onNodeWithContentDescription("Lap").performClick()
            advance(15_860)
            compose.onNodeWithContentDescription("Pause").performClick()
            compose.waitForIdle()

            val watch = repository.stopwatch.value
            assertFalse(watch.isRunning)
            assertEquals(listOf(61_230L, 57_670L, 66_510L), watch.laps().map { it.millis })
            assertEquals(201_270L, watch.elapsed(0))
            compose.onNodeWithText("Fastest").assertExists()
            compose.onNodeWithText("Slowest").assertExists()
            compose.onRoot().captureRoboImage("../docs/screenshots/stopwatch.png")
        }

        // Paused: the notification offers Resume and Reset; Reset clears the stopwatch and the notification.
        shadowOf(Looper.getMainLooper()).idle()
        val paused = checkNotNull(stopwatchNotification())
        assertEquals(listOf("Resume", "Reset"), paused.actions.map { it.title.toString() })
        paused.actions
            .single { it.title == "Reset" }
            .actionIntent
            .send()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(repository.stopwatch.value.isReset)
        assertEquals(null, stopwatchNotification())
    }

    @Test
    fun `a running stopwatch counts in the notification by itself`() {
        repository.update { it.start(time.now.toInstant().toEpochMilli() - 90_000) }
        shadowOf(Looper.getMainLooper()).idle()

        val running = checkNotNull(stopwatchNotification())
        assertTrue(running.extras.getBoolean("android.showChronometer"))
        assertEquals(time.now.toInstant().toEpochMilli() - 90_000, running.`when`)
        assertEquals(listOf("Pause", "Lap", "Reset"), running.actions.map { it.title.toString() })
    }
}
