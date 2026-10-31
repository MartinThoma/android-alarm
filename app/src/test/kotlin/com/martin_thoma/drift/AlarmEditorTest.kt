package com.martin_thoma.drift

import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/** The alarm editor: repeat days, label, vibration, snooze length and a visible delete. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class AlarmEditorTest {
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

    private fun alarm() = runBlocking { repository.getAll() }.singleOrNull()

    @Test
    fun `a one-time alarm becomes a weekday alarm with a label`() {
        runBlocking { repository.save(Alarm(time = LocalTime.of(6, 30), enabled = false)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            await { compose.onAllNodesWithText("Once").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText("Once") and hasClickAction()).performClick()
            await { compose.onAllNodesWithText("Edit alarm").fetchSemanticsNodes().isNotEmpty() }

            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday").forEach {
                compose.onNode(hasContentDescription(it)).performClick()
                compose.waitForIdle()
            }
            compose.onNode(hasSetTextAction()).performTextInput("Work")
            compose.onNodeWithContentDescription("Longer snooze").performClick()
            compose.onNode(hasText("Vibrate") and hasClickAction()).performClick()
            await { alarm()?.let { it.label == "Work" && it.snoozeMinutes == 15 && !it.vibrate && it.repeatDays.size == 5 } == true }
            compose.onRoot().captureRoboImage("../docs/screenshots/alarm-editor.png")

            val edited = checkNotNull(alarm())
            assertEquals(DayOfWeek.entries.take(5).toSet(), edited.repeatDays)
            // Choosing days for an alarm that was off turns it on.
            assertEquals(true, edited.enabled)
            assertFalse(edited.vibrate)

            // Deleting is a visible button, not only a swipe.
            compose.onNodeWithContentDescription("Delete").performClick()
            await { alarm() == null }
        }
        assertNull(alarm())
    }
}
