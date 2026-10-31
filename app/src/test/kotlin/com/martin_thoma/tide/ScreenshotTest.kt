package com.martin_thoma.tide

import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.martin_thoma.tide.core.data.AlarmRepository
import com.martin_thoma.tide.core.model.Alarm
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Starts the real app (Hilt, navigation, view model, Room in memory) and checks what it shows.
 * `./gradlew :app:recordRoborazziDebug` also saves the README screenshots in docs/screenshots/.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class ScreenshotTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var time: FakeTimeSource

    private val weekdays = DayOfWeek.entries.take(5).toSet()

    @Before
    fun setUp() {
        hilt.inject()
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        // Still waves, so screenshots are reproducible.
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    /** Room loads on a background thread that waitForIdle() does not track, so wait for the text itself. */
    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun saveSampleAlarms() =
        runBlocking {
            repository.save(Alarm(time = LocalTime.of(6, 30), repeatDays = weekdays, label = "Work"))
            repository.save(Alarm(time = LocalTime.of(8, 15), repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)))
            repository.save(Alarm(time = LocalTime.of(5, 10), enabled = false, label = "Airport"))
        }

    @Test
    fun alarmsAtNightLowTide() {
        RuntimeEnvironment.setQualifiers("+night")
        saveSampleAlarms()
        time.now = ZonedDateTime.parse("2026-11-01T23:18:00+01:00[Europe/Berlin]") // Sunday evening

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Next alarm · in 7 h 12 min")
            awaitText("Mon–Fri · Work")
            compose.onRoot().captureRoboImage("../docs/screenshots/alarms-night.png")
        }
    }

    @Test
    fun alarmsShortlyBeforeWakingHighTide() {
        saveSampleAlarms()
        time.now = ZonedDateTime.parse("2026-11-02T06:05:00+01:00[Europe/Berlin]") // Monday morning

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Next alarm · in 25 min")
            compose.onRoot().captureRoboImage("../docs/screenshots/alarms-day.png")
        }
    }

    @Test
    fun noAlarms() {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("No alarms yet. Tap + to add one.")
            compose.onRoot().captureRoboImage("../docs/screenshots/alarms-empty.png")
        }
    }
}
