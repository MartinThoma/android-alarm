package com.martin_thoma.drift

import android.provider.Settings
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The timer's wheel, from Martin's test on the S25 (2026-10-31): it rolls on when flung and carries over 59. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class TimerWheelTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() {
        hilt.inject()
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    private fun minutesWheel() = compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasContentDescription("Minutes")))

    private fun openTimerTab() {
        ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Timer").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Timer").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("Minutes: 0")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `turning the minutes past 59 carries into the hours`() {
        openTimerTab()
        minutesWheel().performScrollToIndex(75)
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("Hours: 1")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasContentDescription("Minutes: 15")).assertExists()
    }

    @Test
    fun `a quick flick keeps the wheel rolling`() {
        openTimerTab()
        // One short, fast stroke over about two numbers (52 dp each): the wheel rolls on, at least twice as far.
        minutesWheel().performTouchInput { swipeUp(startY = centerY + 150f, endY = centerY - 150f, durationMillis = 80) }
        compose.waitForIdle()
        val minutes =
            (0..59).first { m -> compose.onAllNodes(hasContentDescription("Minutes: $m")).fetchSemanticsNodes().isNotEmpty() }
        val hours =
            (0..23).first { h -> compose.onAllNodes(hasContentDescription("Hours: $h")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue("rolled only to ${hours}h ${minutes}m", hours * 60 + minutes > 4)
    }

    @Test
    fun `tapping a number on the wheel selects it`() {
        openTimerTab()
        compose.onNode(hasText("02") and hasAnyAncestor(hasContentDescription("Minutes"))).performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("Minutes: 2")).fetchSemanticsNodes().isNotEmpty() }
    }
}
