package com.martin_thoma.drift

import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.martin_thoma.drift.core.data.TimerRepository
import com.martin_thoma.drift.core.model.Timer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

/** A timer can be renamed from its own screen (Martin, 2026-10-31). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class TimerRenameTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var repository: TimerRepository

    @Inject lateinit var time: FakeTimeSource

    @Before
    fun setUp() {
        hilt.inject()
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    @Test
    fun `tapping the name renames the timer`() {
        val now = time.now.toInstant().toEpochMilli()
        runBlocking { repository.save(Timer(label = "Tea", setMillis = 600_000, createdAt = now).start(now)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Timer").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Tea").fetchSemanticsNodes().isNotEmpty() }

            compose.onNode(hasText("Tea") and hasClickAction()).performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Earl Grey")
            compose.onNode(hasSetTextAction()).performImeAction()

            compose.waitUntil(5_000) {
                shadowOf(Looper.getMainLooper()).idle()
                runBlocking { repository.getAll() }.single().label == "Earl Grey"
            }
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Earl Grey").fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
