package com.martin_thoma.drift

import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.data.TimerRepository
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.model.Timer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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

/**
 * Every control on every screen can be hit and is named for TalkBack, as Android's accessibility
 * guide asks: touch targets of at least 48 × 48 dp, and a text or description on everything
 * that does something. (Google's Accessibility Test Framework does not run under Robolectric.)
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class AccessibilityTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var alarms: AlarmRepository

    @Inject lateinit var timers: TimerRepository

    @Inject lateinit var time: FakeTimeSource

    private val problems = mutableListOf<String>()

    @Before
    fun setUp() {
        hilt.inject()
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    private fun await(text: String) {
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private val interactive =
        SemanticsMatcher("does something") {
            it.config.contains(SemanticsActions.OnClick) || it.config.contains(SemanticsActions.SetProgress)
        }

    private fun hasCustomAction(label: String) =
        SemanticsMatcher("has the action '$label'") { node ->
            node.config
                .getOrNull(SemanticsActions.CustomActions)
                .orEmpty()
                .any { it.label == label }
        }

    private fun SemanticsNode.name(): String =
        listOfNotNull(
            config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(),
            config.getOrNull(SemanticsProperties.Text)?.joinToString(),
            config.getOrNull(SemanticsProperties.EditableText)?.text,
        ).joinToString(" ").trim()

    /** Notes every control on the current screen that is too small, has no name, or shares its name. */
    private fun check(screen: String) {
        val min =
            48 *
                ApplicationProvider
                    .getApplicationContext<android.content.Context>()
                    .resources.displayMetrics.density - 1
        val nodes = compose.onAllNodes(interactive).fetchSemanticsNodes()
        nodes
            .groupBy { it.name() }
            .filter { (name, same) -> name.isNotEmpty() && same.size > 1 }
            .forEach { (name, same) -> problems += "$screen: ${same.size} controls are all called '$name'" }
        for (node in nodes) {
            val name = node.name()
            if (name.isEmpty()) problems += "$screen: a control without a name at ${node.boundsInRoot}"
            val width = maxOf(node.size.width.toFloat(), node.touchBoundsInRoot.width)
            val height = maxOf(node.size.height.toFloat(), node.touchBoundsInRoot.height)
            // Material 3's slider is 44 dp tall and Compose does not widen it; it spans the whole width,
            // so it is accepted as Material's own control (docs/guidelines.md).
            val slider = node.config.contains(SemanticsActions.SetProgress)
            if (!slider && (width < min || height < min)) {
                problems += "$screen: '$name' is ${width.toInt()} × ${height.toInt()} px, less than 48 dp"
            }
        }
    }

    @Test
    fun `every control can be hit and is named`() {
        val now = time.now.toInstant().toEpochMilli()
        runBlocking {
            alarms.save(Alarm(time = LocalTime.of(6, 30), repeatDays = DayOfWeek.entries.take(5).toSet(), label = "Work"))
            alarms.save(Alarm(time = LocalTime.of(9, 0), enabled = false))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            await("Work")
            check("alarm list")
            // What a swipe deletes, TalkBack can delete too.
            compose.onAllNodes(hasCustomAction("Delete")).assertCountEquals(2)

            compose.onNodeWithContentDescription("New alarm").performClick()
            await("Set alarm")
            check("new alarm")
            compose.onNodeWithText("Cancel").performClick()

            compose.onNode(hasText("Mon–Fri · Work") and hasClickAction()).performClick()
            await("Edit alarm")
            check("alarm editor")

            compose.onNode(hasText("Sound") and hasClickAction()).performClick()
            await("Alarm sound")
            check("sound picker")
            compose.onNodeWithContentDescription("Back").performClick()
            await("Edit alarm")
            compose.onNodeWithContentDescription("Done").performClick()
            await("Work")

            compose.onNodeWithText("Stopwatch").performClick()
            compose.onNodeWithContentDescription("Start").performClick()
            compose.onNodeWithContentDescription("Lap").performClick()
            compose.waitForIdle()
            check("stopwatch")
            compose.onNodeWithContentDescription("Pause").performClick()

            compose.onNodeWithText("Timer").performClick()
            compose.waitForIdle()
            check("new timer")

            runBlocking { timers.save(Timer(label = "Tea", setMillis = 600_000, createdAt = now).start(now)) }
            await("Tea")
            check("timer")

            runBlocking { timers.save(Timer(label = "Eggs", setMillis = 420_000, createdAt = now).start(now)) }
            await("Eggs")
            check("timer list")
            compose.onAllNodes(hasCustomAction("Delete timer")).assertCountEquals(2)
        }
        assertTrue(problems.distinct().joinToString("\n"), problems.isEmpty())
    }
}
