package com.martin_thoma.drift

import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/** Regression test: "Undo" after swiping an alarm away must bring it back (found on the S25, 2026-10-31). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class AlarmUndoTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var repository: AlarmRepository

    @Before
    fun setUp() {
        hilt.inject()
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    /** Room writes finish on the main thread, so let it run while waiting. */
    private fun await(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
        compose.waitForIdle()
    }

    private fun alarms() = runBlocking { repository.getAll() }

    /** Moves the UI clock in small steps, at most [maxMillis] in all, until [condition] holds. */
    private fun advanceUntil(
        maxMillis: Long,
        condition: () -> Boolean,
    ) {
        var waited = 0L
        while (waited < maxMillis) {
            compose.mainClock.advanceTimeBy(FRAME_MILLIS)
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            if (condition()) return
            waited += FRAME_MILLIS
        }
    }

    private companion object {
        const val FRAME_MILLIS = 50L
    }

    @Test
    fun `undo brings back a swiped-away alarm with all its settings`() {
        val work = Alarm(time = LocalTime.of(6, 30), repeatDays = DayOfWeek.entries.take(5).toSet(), label = "Work", snoozeMinutes = 15)
        val id = runBlocking { repository.save(work) }
        ActivityScenario.launch(MainActivity::class.java).use {
            await { compose.onAllNodesWithText("Mon–Fri · Work").fetchSemanticsNodes().isNotEmpty() }

            // From here the clock moves only as a person's would, so the note does not time out unseen.
            compose.mainClock.autoAdvance = false
            compose.onNode(hasText("Mon–Fri · Work") and hasClickAction()).performTouchInput { swipeLeft() }
            advanceUntil(maxMillis = 1_500) { alarms().isEmpty() && compose.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty() }

            compose.onNodeWithText("Undo").performClick()
            advanceUntil(maxMillis = 1_500) { compose.onAllNodesWithText("Mon–Fri · Work").fetchSemanticsNodes().isNotEmpty() }
            // It must stay back, not be swiped away a second time by the old swipe state.
            advanceUntil(maxMillis = 1_000) { false }
        }
        assertEquals(listOf(work.copy(id = id)), alarms())
    }
}
