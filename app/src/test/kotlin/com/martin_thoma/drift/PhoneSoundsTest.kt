package com.martin_thoma.drift

import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
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
import java.time.LocalTime
import javax.inject.Inject

/** The phone's own alarm tones and ringtones are a group in the sound picker (requirements AL-04). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class PhoneSoundsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var repository: AlarmRepository

    @Before
    fun setUp() {
        hilt.inject()
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
        compose.waitForIdle()
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `a ringtone of the phone can be chosen and is named in the list`() {
        runBlocking { repository.save(Alarm(time = LocalTime.of(6, 30), enabled = false)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            await { shows("Once") }
            compose.onNode(hasText("Once") and hasClickAction()).performClick()
            await { shows("Edit alarm") }
            compose.onNode(hasText("Sound") and hasClickAction()).performClick()
            await { shows("Alarm sound") }

            // The alarm tones show; the many ringtones wait behind a button.
            val picker = compose.onNode(hasScrollAction())
            picker.performScrollToNode(hasText("Morning Glory"))
            compose.onNodeWithText("On this phone").assertExists()
            compose.onNodeWithText("Sunrise").assertExists()
            compose.onNodeWithText("Over the Horizon").assertDoesNotExist()
            picker.performScrollToNode(hasText("Show ringtones (3)"))
            compose.onNodeWithText("Show ringtones (3)").performClick()

            picker.performScrollToNode(hasText("Over the Horizon"))
            compose.onNodeWithText("Over the Horizon").performClick()
            val chosen = FakePhoneSoundsModule.sounds.single { it.title == "Over the Horizon" }.uri
            await { runBlocking { repository.getAll() }.single().soundUri == chosen }

            compose.onNodeWithContentDescription("Back").performClick()
            await { shows("Edit alarm") }
            compose.onNodeWithText("Over the Horizon").assertExists()
            compose.onNodeWithContentDescription("Done").performClick()
            await { shows("Once · Over the Horizon") }
        }
    }
}
