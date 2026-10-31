package com.martin_thoma.drift.core.data

import com.martin_thoma.drift.core.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

class AlarmMappingTest {
    @Test
    fun `every combination of repeat days survives a round trip`() {
        for (mask in 0 until (1 shl 7)) {
            val days = DayOfWeek.entries.filterTo(mutableSetOf()) { mask and (1 shl (it.value - 1)) != 0 }
            val alarm = Alarm(id = 7, time = LocalTime.of(6, 30), repeatDays = days, label = "Work", soundUri = "content://x")
            assertEquals(alarm, alarm.toEntity().toModel())
        }
    }

    @Test
    fun `Monday is bit 0 and Sunday is bit 6`() {
        val alarm = Alarm(time = LocalTime.of(6, 30), repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY))
        assertEquals(0b1000001, alarm.toEntity().repeatDays)
    }

    @Test
    fun `a pending snooze survives a round trip`() {
        val alarm = Alarm(id = 3, time = LocalTime.of(6, 30), snoozedUntil = Instant.parse("2026-11-02T05:40:12Z"))
        assertEquals(alarm, alarm.toEntity().toModel())
    }
}
