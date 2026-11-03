package com.martin_thoma.drift

import android.provider.Settings
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.model.Challenge
import com.martin_thoma.drift.core.ringing.RingingState
import com.martin_thoma.drift.core.ringing.RingingStatus
import com.martin_thoma.drift.feature.ringing.RingingActivity
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

    @Inject lateinit var ringingState: RingingState

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

    private fun awaitTextContaining(text: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
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
            awaitText("Work · in 7 h 12 min")
            awaitText("Mon–Fri · Work")
            compose.onRoot().captureRoboImage("../docs/screenshots/alarms-night.png")
        }
    }

    @Test
    fun alarmsShortlyBeforeWakingHighTide() {
        saveSampleAlarms()
        time.now = ZonedDateTime.parse("2026-11-02T06:05:00+01:00[Europe/Berlin]") // Monday morning

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Work · in 25 min")
            compose.onRoot().captureRoboImage("../docs/screenshots/alarms-day.png")
        }
    }

    @Test
    fun alarmsAfternoonMidTide() {
        saveSampleAlarms()
        time.now = ZonedDateTime.parse("2026-11-01T22:00:00+01:00[Europe/Berlin]") // Sunday evening

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Work · in 8 h 30 min")
            compose.onRoot().captureRoboImage("../docs/screenshots/alarms-midtide.png")
        }
    }

    @Test
    fun noAlarms() {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Tap + to set your first alarm.")
            compose.onRoot().captureRoboImage("../docs/screenshots/alarms-empty.png")
        }
    }

    @Test
    fun soundPicker() {
        saveSampleAlarms()
        time.now = ZonedDateTime.parse("2026-11-02T06:05:00+01:00[Europe/Berlin]")

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Mon–Fri · Work")
            compose.onNode(hasText("Mon–Fri · Work") and hasClickAction()).performClick()
            compose.onNode(hasText("Sound") and hasClickAction()).performClick()
            awaitText("Alarm sound")
            awaitText("Canon in D · Pachelbel")
            compose.onRoot().captureRoboImage("../docs/screenshots/sounds.png")
        }
    }

    @Test
    fun soundPickerAtNight() {
        RuntimeEnvironment.setQualifiers("+night")
        saveSampleAlarms()

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Mon–Fri · Work")
            compose.onNode(hasText("Mon–Fri · Work") and hasClickAction()).performClick()
            compose.onNode(hasText("Sound") and hasClickAction()).performClick()
            awaitText("Canon in D · Pachelbel")
            compose.onRoot().captureRoboImage("../docs/screenshots/sounds-night.png")
        }
    }

    @Test
    fun ringing() {
        time.now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
        val alarm = Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays, label = "Work")
        ringingState.update(RingingStatus.Ringing(alarm, time.now.toInstant()))

        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Slide to dismiss")
            awaitText("Monday, November 2")
            compose.onRoot().captureRoboImage("../docs/screenshots/ringing.png")
        }
    }

    @Test
    fun ringingAtNight() {
        RuntimeEnvironment.setQualifiers("+night")
        time.now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
        val alarm = Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays, label = "Work")
        ringingState.update(RingingStatus.Ringing(alarm, time.now.toInstant()))

        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Slide to dismiss")
            awaitText("Monday, November 2")
            compose.onRoot().captureRoboImage("../docs/screenshots/ringing-night.png")
        }
    }

    @Test
    fun ringingWithSums() {
        time.now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
        val alarm = Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays, label = "Work", challenge = Challenge.MATH)
        ringingState.update(RingingStatus.Ringing(alarm, time.now.toInstant()))

        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Solve to dismiss")
            compose.onRoot().captureRoboImage("../docs/screenshots/ringing-sums-closed.png")
            compose.onNode(hasText("Solve to dismiss") and hasClickAction()).performClick()
            awaitText("Sum 1 of 3")
            compose.onRoot().captureRoboImage("../docs/screenshots/ringing-sums.png")
        }
    }

    @Test
    fun ringingWithHold() {
        time.now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
        val alarm = Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays, label = "Work", challenge = Challenge.HOLD)
        ringingState.update(RingingStatus.Ringing(alarm, time.now.toInstant()))

        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Hold to dismiss")
            compose.onRoot().captureRoboImage("../docs/screenshots/ringing-hold.png")
        }
    }

    /** German has the longest words of the four languages, so it shows first whether text fits. */
    @Test
    fun inGerman() {
        RuntimeEnvironment.setQualifiers("+de")
        saveSampleAlarms()
        time.now = ZonedDateTime.parse("2026-11-01T23:18:00+01:00[Europe/Berlin]")

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Work · in 7 Std. 12 Min.")
            awaitText("Mo.–Fr. · Work")
            compose.onRoot().captureRoboImage("../docs/screenshots/de/alarms.png")
            compose.onNode(hasText("Mo.–Fr. · Work") and hasClickAction()).performClick()
            compose.onNode(hasText("Weckton") and hasClickAction()).performClick()
            awaitText("Grillenzirpen")
            compose.onRoot().captureRoboImage("../docs/screenshots/de/sounds.png")
        }
    }

    @Test
    fun ringingInGerman() {
        RuntimeEnvironment.setQualifiers("+de")
        time.now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
        ringingState.update(RingingStatus.Ringing(Alarm(id = 1, time = LocalTime.of(6, 30), label = "Arbeit"), time.now.toInstant()))

        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Zum Beenden schieben")
            compose.onRoot().captureRoboImage("../docs/screenshots/de/ringing.png")
        }
    }

    /** Arabic is written right to left: the whole layout, the arrows and the dismiss slider mirror. */
    @Test
    fun inArabic() {
        RuntimeEnvironment.setQualifiers("+ar")
        saveSampleAlarms()
        time.now = ZonedDateTime.parse("2026-11-02T06:05:00+01:00[Europe/Berlin]")

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitTextContaining("بعد")
            compose.onRoot().captureRoboImage("../docs/screenshots/ar/alarms.png")
            compose.onAllNodes(hasText("Work", substring = true) and hasClickAction())[0].performClick()
            compose.onNode(hasText("الصوت") and hasClickAction()).performClick()
            awaitText("صوت المنبه")
            compose.onRoot().captureRoboImage("../docs/screenshots/ar/sounds.png")
        }
    }

    @Test
    fun ringingInArabicAndRussian() {
        time.now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
        ringingState.update(RingingStatus.Ringing(Alarm(id = 1, time = LocalTime.of(6, 30)), time.now.toInstant()))
        for ((qualifier, slide) in listOf("+ar" to "اسحب للإيقاف", "+ru" to "Проведите, чтобы выключить")) {
            RuntimeEnvironment.setQualifiers(qualifier)
            ActivityScenario.launch(RingingActivity::class.java).use {
                awaitText(slide)
                compose.onRoot().captureRoboImage("../docs/screenshots/${qualifier.drop(1)}/ringing.png")
            }
        }
    }
}
