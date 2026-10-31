package com.martin_thoma.tide.feature.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.ZonedDateTime

class AlarmFormattingTest {
    private val now = ZonedDateTime.parse("2026-11-01T23:18:00+01:00[Europe/Berlin]")

    @Test
    fun `hours and minutes`() {
        assertEquals(TimeUntil.HoursMinutes(7, 12), timeUntil(now, now.plusHours(7).plusMinutes(12)))
    }

    @Test
    fun `partial minutes round up`() {
        assertEquals(TimeUntil.Minutes(1), timeUntil(now, now.plusSeconds(5)))
        assertEquals(TimeUntil.HoursMinutes(7, 13), timeUntil(now, now.plusHours(7).plusMinutes(12).plusSeconds(1)))
    }

    @Test
    fun `never reads zero minutes`() {
        assertEquals(TimeUntil.Minutes(1), timeUntil(now, now))
    }

    @Test
    fun `a day or more reads in days and hours`() {
        assertEquals(TimeUntil.DaysHours(6, 9), timeUntil(now, now.plusDays(6).plusHours(9)))
    }

    @Test
    fun `repeat labels`() {
        assertEquals(RepeatLabel.Once, repeatLabel(emptySet()))
        assertEquals(
            RepeatLabel.EveryDay,
            repeatLabel(
                java.time.DayOfWeek.entries
                    .toSet(),
            ),
        )
        assertEquals(RepeatLabel.Range(MONDAY, FRIDAY), repeatLabel(setOf(FRIDAY, MONDAY, WEDNESDAY, TUESDAY, THURSDAY)))
        assertEquals(RepeatLabel.Days(listOf(SATURDAY, SUNDAY)), repeatLabel(setOf(SUNDAY, SATURDAY)))
        assertEquals(RepeatLabel.Days(listOf(MONDAY, WEDNESDAY, FRIDAY)), repeatLabel(setOf(MONDAY, WEDNESDAY, FRIDAY)))
    }
}
