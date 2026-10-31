package com.martin_thoma.tide.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

class AlarmTest {
    private val snooze = Instant.parse("2026-11-02T05:40:00Z")
    private val once = Alarm(id = 1, time = LocalTime.of(6, 30))
    private val weekly = once.copy(repeatDays = setOf(DayOfWeek.MONDAY))

    @Test
    fun `a one-time alarm turns off once it rings`() {
        assertFalse(once.afterRinging().enabled)
    }

    @Test
    fun `a repeating alarm stays on once it rings`() {
        assertTrue(weekly.afterRinging().enabled)
    }

    @Test
    fun `ringing uses up a snooze`() {
        assertEquals(null, weekly.copy(snoozedUntil = snooze).afterRinging().snoozedUntil)
        assertEquals(once.afterRinging(), once.copy(enabled = false, snoozedUntil = snooze).afterRinging())
    }

    @Test
    fun `an alarm that is off is active while it is snoozed`() {
        assertFalse(once.copy(enabled = false).isActive)
        assertTrue(once.copy(enabled = false, snoozedUntil = snooze).isActive)
    }
}
