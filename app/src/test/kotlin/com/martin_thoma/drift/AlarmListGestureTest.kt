package com.martin_thoma.drift

import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import java.time.LocalTime
import javax.inject.Inject

/** Regression tests for the alarm list, from Martin's test on the S25 (2026-10-31). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class AlarmListGestureTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var time: FakeTimeSource

    @Before
    fun setUp() {
        hilt.inject()
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.System.putString(resolver, Settings.System.TIME_12_24, "24")
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
        compose.waitForIdle()
    }

    private fun alarms() = runBlocking { repository.getAll() }

    private fun row() = compose.onNode(hasText("Once · Work") and hasClickAction())

    private fun openList() {
        runBlocking { repository.save(Alarm(time = LocalTime.of(6, 30), label = "Work")) }
        ActivityScenario.launch(MainActivity::class.java)
        await { compose.onAllNodesWithText("Once · Work").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `a short swipe does not delete an alarm`() {
        openList()
        // A third of the row, at a calm pace: the alarm stays.
        row().performTouchInput { swipeLeft(startX = right - 10f, endX = right - 10f - width / 3f, durationMillis = 600) }
        repeat(20) { shadowOf(Looper.getMainLooper()).idle() }
        compose.waitForIdle()
        assertEquals(1, alarms().size)
    }

    @Test
    fun `a short quick flick does not delete an alarm either`() {
        openList()
        // Material deleted on any swipe faster than 125 dp/s; only the distance counts now.
        row().performTouchInput { swipeLeft(startX = right - 10f, endX = right - 10f - width / 3f, durationMillis = 80) }
        repeat(20) { shadowOf(Looper.getMainLooper()).idle() }
        compose.waitForIdle()
        assertEquals(1, alarms().size)
    }

    @Test
    fun `a swipe over most of the row deletes the alarm`() {
        openList()
        row().performTouchInput { swipeLeft(startX = right - 10f, endX = left + width * 0.2f, durationMillis = 600) }
        await { alarms().isEmpty() }
    }

    @Test
    fun `turning off the only alarm does not move the list`() {
        openList()
        val before = row().fetchSemanticsNode().boundsInRoot.top
        compose.onNode(hasText("Once · Work") and hasClickAction()).fetchSemanticsNode()
        compose
            .onAllNodes(
                androidx.compose.ui.test
                    .isToggleable(),
            )[0]
            .performClick()
        await { alarms().single().enabled.not() }
        assertEquals(before, row().fetchSemanticsNode().boundsInRoot.top)
    }
}
