package com.martin_thoma.drift

import android.app.Application
import android.app.NotificationManager
import android.provider.Settings
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.martin_thoma.drift.core.notifications.DriftChannels
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

/** The health check: what it reports, its notice on the alarm list, and where each fix leads (requirements GN-04). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class HealthCheckTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var phone: FakePhoneSettings

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        hilt.inject()
        Settings.Global.putFloat(application.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun awaitText(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openHealthCheck() {
        awaitText("Check that alarms can ring")
        compose.onNodeWithText("Check that alarms can ring").performScrollTo().performClick()
        awaitText("Health check")
    }

    /** Taps [label] (a fix) and returns the system screen it opened. */
    private fun fix(label: String): android.content.Intent {
        compose.onNode(hasText(label) and hasClickAction()).performScrollTo().performClick()
        compose.waitForIdle()
        return checkNotNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun `with everything allowed there is no notice, and the check says so`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Check that alarms can ring")
            compose.onNodeWithText("Alarms may not work as they should").assertDoesNotExist()
            openHealthCheck()
            compose.onNodeWithText("Alarms can ring and show themselves.").assertExists()
            compose.onNodeWithText("Turn on").assertDoesNotExist()
        }
    }

    @Test
    fun `a problem shows a notice that opens the check, and each fix opens its setting`() {
        phone.notificationsEnabled = false
        phone.fullScreenAllowed = false
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Alarms may not work as they should")
            compose.onNodeWithText("2 settings need your attention.").assertExists()
            compose.onNodeWithText("Alarms may not work as they should").performClick()
            awaitText("Health check")
            compose.onNodeWithText("2 settings need your attention.").assertExists()

            val turnOn = compose.onAllNodes(hasText("Turn on") and hasClickAction())
            turnOn[0].performClick()
            compose.waitForIdle()
            val notifications = checkNotNull(shadowOf(application).nextStartedActivity)
            assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, notifications.action)
            assertEquals(application.packageName, notifications.getStringExtra(Settings.EXTRA_APP_PACKAGE))
            turnOn[1].performClick()
            compose.waitForIdle()
            assertEquals(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, shadowOf(application).nextStartedActivity.action)
        }
    }

    @Test
    fun `coming back from the settings checks again`() {
        phone.notificationsEnabled = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitText("Alarms may not work as they should")
            compose.onNodeWithText("Alarms may not work as they should").performClick()
            awaitText("1 setting needs your attention.")

            // The user turns notifications on in the system settings and comes back.
            scenario.moveToState(Lifecycle.State.STARTED)
            phone.notificationsEnabled = true
            scenario.moveToState(Lifecycle.State.RESUMED)
            awaitText("Alarms can ring and show themselves.")
        }
    }

    @Test
    fun `a turned-down ringing channel opens that channel`() {
        phone.ringingChannelImportance = NotificationManager.IMPORTANCE_LOW
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Alarms may not work as they should")
            openHealthCheck()
            compose
                .onNodeWithText(
                    "The “Ringing alarms” notifications are turned down. A ringing alarm may not show itself.",
                ).assertExists()
            val intent = fix("Turn on")
            assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.action)
            assertEquals(DriftChannels.RINGING, intent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
        }
    }

    @Test
    fun `an optimised battery is only a tip, a restricted one a problem`() {
        phone.batteryOptimised = true
        phone.samsung = true
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openHealthCheck()
            compose.onNodeWithText("Alarms may not work as they should").assertDoesNotExist()
            compose.onNodeWithText("Alarms can ring and show themselves.").assertExists()
            compose.onNode(hasText("Background usage limits", substring = true)).assertExists()
            assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, fix("Stop optimising").action)

            scenario.moveToState(Lifecycle.State.STARTED)
            phone.backgroundRestricted = true
            scenario.moveToState(Lifecycle.State.RESUMED)
            awaitText("1 setting needs your attention.")
            assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, fix("Open App info").action)
        }
    }
}
