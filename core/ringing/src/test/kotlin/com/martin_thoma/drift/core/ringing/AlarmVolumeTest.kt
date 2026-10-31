package com.martin_thoma.drift.core.ringing

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmVolumeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

    private fun alarmLevel() = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

    @Test
    fun `levels are a share of the range, and never silent`() {
        assertEquals(7, AlarmVolume.levelFor(percent = 100, minLevel = 1, maxLevel = 7))
        assertEquals(5, AlarmVolume.levelFor(percent = 70, minLevel = 1, maxLevel = 7))
        assertEquals(1, AlarmVolume.levelFor(percent = 0, minLevel = 0, maxLevel = 7))
        assertEquals(2, AlarmVolume.levelFor(percent = 5, minLevel = 2, maxLevel = 7))
    }

    @Test
    fun `ringing sets the alarm's level and restores the user's`() {
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 2, 0)
        val volume = AlarmVolume(context)

        volume.apply(100)
        assertEquals(max, alarmLevel())

        volume.restore()
        assertEquals(2, alarmLevel())
    }

    @Test
    fun `the user's level survives the process dying mid-ring`() {
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 2, 0)
        AlarmVolume(context).apply(100)

        // A new process: the app restores at start.
        AlarmVolume(context).restore()
        assertEquals(2, alarmLevel())
    }

    @Test
    fun `a second alarm replacing the first keeps the user's level`() {
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 2, 0)
        val volume = AlarmVolume(context)

        volume.apply(100)
        volume.apply(50)
        volume.restore()
        assertEquals(2, alarmLevel())
    }

    @Test
    fun `restore without a ring changes nothing`() {
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 3, 0)
        AlarmVolume(context).restore()
        assertEquals(3, alarmLevel())
    }
}
