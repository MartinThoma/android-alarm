package com.martin_thoma.drift

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.model.Challenge
import com.martin_thoma.drift.core.ringing.RingingService
import com.martin_thoma.drift.core.ringing.RingingState
import com.martin_thoma.drift.core.ringing.RingingStatus
import com.martin_thoma.drift.feature.ringing.RingingActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.SensorBuilder
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/** Dismiss challenges: slide, shake or sums on the ringing screen, chosen in the editor (requirements AL-21). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class ChallengeTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var ringingState: RingingState

    @Inject lateinit var time: FakeTimeSource

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        hilt.inject()
        Settings.System.putString(application.contentResolver, Settings.System.TIME_12_24, "24")
        Settings.Global.putFloat(application.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        ShadowMediaPlayer.setMediaInfoProvider { _: DataSource -> ShadowMediaPlayer.MediaInfo() }
        time.now = ZonedDateTime.parse("2026-11-02T06:30:00+01:00[Europe/Berlin]")
    }

    private fun ringing(challenge: Challenge) =
        ringingState.update(RingingStatus.Ringing(Alarm(id = 1, time = LocalTime.of(6, 30), challenge = challenge), Instant.EPOCH))

    /** Room runs off the main thread; let both sides finish until [condition] holds. */
    private fun awaitUntil(condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        error("Condition not met")
    }

    private fun awaitText(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    /** The ringing screen's controls ask [RingingService] to dismiss; Robolectric only records the request. */
    private fun dismissRequested(): Boolean {
        val shadow = shadowOf(application)
        while (true) {
            val intent = shadow.nextStartedService ?: return false
            if (intent.action == RingingService.ACTION_DISMISS) return true
        }
    }

    /** The sum on screen, as "47 + 38", solved. */
    private fun answerOnScreen(): Int {
        val sum = Regex("""(\d+) ([+−×]) (\d+)""")
        val text =
            compose
                .onNode(SemanticsMatcher("a sum") { node -> node.text()?.let(sum::matches) == true }, useUnmergedTree = true)
                .fetchSemanticsNode()
                .text()!!
        val (left, operator, right) = sum.matchEntire(text)!!.destructured
        return when (operator) {
            "+" -> left.toInt() + right.toInt()
            "−" -> left.toInt() - right.toInt()
            else -> left.toInt() * right.toInt()
        }
    }

    private fun androidx.compose.ui.semantics.SemanticsNode.text() =
        config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }

    private fun type(answer: Int) {
        answer.toString().forEach { compose.onNode(hasText(it.toString()) and hasClickAction()).performClick() }
        compose.onNodeWithContentDescription("Check answer").performClick()
        compose.waitForIdle()
    }

    @Test
    fun `three sums dismiss the alarm, a wrong answer does not`() {
        ringing(Challenge.MATH)
        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Solve to dismiss")
            compose.onNodeWithText("Solve to dismiss").performClick()
            awaitText("Sum 1 of 3")

            type(answerOnScreen() + 1)
            compose.onNodeWithText("Not quite. Try again.").assertExists()
            compose.onNodeWithText("Sum 1 of 3").assertExists()

            // Back to the time and snooze; the sums done stay done.
            type(answerOnScreen())
            awaitText("Sum 2 of 3")
            compose.onNodeWithText("Back").performClick()
            awaitText("Solve to dismiss")
            compose.onNodeWithText("Snooze").assertExists()
            compose.onNodeWithText("Solve to dismiss").performClick()
            awaitText("Sum 2 of 3")
            assertEquals(false, dismissRequested())

            type(answerOnScreen())
            awaitText("Sum 3 of 3")
            type(answerOnScreen())
            assertEquals(true, dismissRequested())
        }
    }

    @Test
    fun `shaking fills the bar and dismisses`() {
        val sensors = application.getSystemService(SensorManager::class.java)
        shadowOf(sensors).addSensor(SensorBuilder.newBuilder().setType(Sensor.TYPE_ACCELEROMETER).build())
        ringing(Challenge.SHAKE)
        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Shake to dismiss")
            compose.onNodeWithText("Slide to dismiss").assertDoesNotExist()

            repeat(14) { shake(sensors, it) }
            assertEquals(false, dismissRequested())
            shake(sensors, 14)
            assertEquals(true, dismissRequested())
        }
    }

    private fun shake(
        sensors: SensorManager,
        index: Int,
    ) {
        val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // 25 m/s² is about 2.5 g; a shake every 300 ms.
        val event = SensorEventBuilder.newBuilder(accelerometer, floatArrayOf(25f, 0f, 0f)).setTimestamp(index * 300_000_000L).build()
        shadowOf(sensors).sendSensorEventToListeners(event)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `without an accelerometer, shaking falls back to the slider`() {
        ringing(Challenge.SHAKE)
        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Slide to dismiss")
            compose.onNodeWithText("Shake to dismiss").assertDoesNotExist()
        }
    }

    @Test
    fun `holding the circle for three seconds dismisses, letting go early does not`() {
        ringing(Challenge.HOLD)
        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Hold to dismiss")
            compose.onNodeWithText("Slide to dismiss").assertDoesNotExist()
            val circle = compose.onNodeWithText("Hold to dismiss")
            compose.mainClock.autoAdvance = false

            circle.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(2_000)
            circle.performTouchInput { up() }
            compose.mainClock.advanceTimeBy(2_000)
            assertEquals(false, dismissRequested())

            // Starts from nothing again: two more seconds are not enough either.
            circle.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(2_000)
            assertEquals(false, dismissRequested())
            compose.mainClock.advanceTimeBy(1_100)
            assertEquals(true, dismissRequested())
        }
    }

    @Test
    fun `TalkBack's double tap and hold dismisses too`() {
        ringing(Challenge.HOLD)
        ActivityScenario.launch(RingingActivity::class.java).use {
            awaitText("Hold to dismiss")
            compose.onNodeWithText("Hold to dismiss").performSemanticsAction(SemanticsActions.OnLongClick)
            assertEquals(true, dismissRequested())
        }
    }

    private fun ringInService(challenge: Challenge): Pair<ServiceController<RingingService>, Long> {
        val id = runBlocking { repository.save(Alarm(time = LocalTime.of(6, 30), challenge = challenge)) }
        val service = Robolectric.buildService(RingingService::class.java).create()
        service.get().onStartCommand(Intent(RingingService.ACTION_RING).putExtra("com.martin_thoma.drift.extra.ALARM_ID", id), 0, 1)
        awaitUntil { ringingState.status.value is RingingStatus.Ringing }
        return service to id
    }

    @Test
    fun `slide and hold keep Dismiss in the notification`() {
        for (challenge in listOf(Challenge.SLIDE, Challenge.HOLD)) {
            val (service, _) = ringInService(challenge)
            val notifications = application.getSystemService(NotificationManager::class.java)
            val ringing = notifications.activeNotifications.single { it.id == 1 }.notification
            assertEquals("$challenge", listOf("Snooze", "Dismiss"), ringing.actions.map { it.title.toString() })
            service.get().onStartCommand(Intent(RingingService.ACTION_DISMISS), 0, 2)
        }
    }

    @Test
    fun `a wake-up challenge leaves no Dismiss in the notifications`() {
        val (service, id) = ringInService(Challenge.MATH)
        val notifications = application.getSystemService(NotificationManager::class.java)
        val ringing = notifications.activeNotifications.single { it.id == 1 }.notification
        assertEquals(listOf("Snooze"), ringing.actions.map { it.title.toString() })

        service.get().onStartCommand(Intent(RingingService.ACTION_SNOOZE), 0, 2)
        awaitUntil { notifications.activeNotifications.any { it.tag == "snoozed:$id" } }
        val snoozed = notifications.activeNotifications.single { it.tag == "snoozed:$id" }.notification
        assertNull(snoozed.actions)
    }

    @Test
    fun `the editor saves the challenge`() {
        runBlocking { repository.save(Alarm(time = LocalTime.of(6, 30))) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(5_000) { compose.onAllNodes(hasText("Once") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText("Once") and hasClickAction()).performClick()
            awaitText("Edit alarm")
            compose.onNodeWithText("Sums").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                shadowOf(Looper.getMainLooper()).idle()
                runBlocking { repository.getAll() }.single().challenge == Challenge.MATH
            }
            awaitText("Solve three short sums.")
        }
    }
}
