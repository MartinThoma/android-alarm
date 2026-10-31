package com.martin_thoma.tide.core.domain

import com.martin_thoma.tide.core.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.random.Random

class NextTriggerTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

    private fun at(
        text: String,
        zone: ZoneId = berlin,
    ): ZonedDateTime = LocalDateTime.parse(text).atZone(zone)

    private fun alarm(
        time: String,
        vararg days: java.time.DayOfWeek,
        enabled: Boolean = true,
    ) = Alarm(id = 1, time = LocalTime.parse(time), repeatDays = days.toSet(), enabled = enabled)

    @Test
    fun `one-time alarm later today rings today`() {
        // Saturday 31 October 2026
        assertEquals(at("2026-10-31T22:00"), NextTrigger.of(alarm("22:00"), at("2026-10-31T21:15")))
    }

    @Test
    fun `one-time alarm earlier today rings tomorrow`() {
        assertEquals(at("2026-11-01T06:30"), NextTrigger.of(alarm("06:30"), at("2026-10-31T21:15")))
    }

    @Test
    fun `alarm exactly now rings tomorrow, not now`() {
        assertEquals(at("2026-11-01T06:30"), NextTrigger.of(alarm("06:30"), at("2026-10-31T06:30")))
    }

    @Test
    fun `weekday alarm on Saturday evening rings on Monday`() {
        assertEquals(at("2026-11-02T06:30"), NextTrigger.of(alarm("06:30", *weekdays.toTypedArray()), at("2026-10-31T23:18")))
    }

    @Test
    fun `repeating alarm later today rings today when today is a repeat day`() {
        assertEquals(at("2026-10-31T08:15"), NextTrigger.of(alarm("08:15", SATURDAY, SUNDAY), at("2026-10-31T07:00")))
    }

    @Test
    fun `repeating alarm on one weekday that has passed today rings in a week`() {
        assertEquals(at("2026-11-07T08:15"), NextTrigger.of(alarm("08:15", SATURDAY), at("2026-10-31T09:00")))
    }

    @Test
    fun `disabled alarm never rings`() {
        assertNull(NextTrigger.of(alarm("06:30", enabled = false), at("2026-10-31T21:15")))
    }

    @Test
    fun `alarm in the spring-forward gap rings one gap later`() {
        // 28 March 2027: clocks jump from 02:00 to 03:00 in Berlin.
        val trigger = NextTrigger.of(alarm("02:30"), at("2027-03-28T01:00"))!!
        assertEquals(LocalDateTime.parse("2027-03-28T03:30"), trigger.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(2), trigger.offset)
    }

    @Test
    fun `alarm in the fall-back overlap rings once, at the first occurrence`() {
        // 25 October 2026: clocks fall back from 03:00 to 02:00 in Berlin, so 02:30 happens twice.
        val first = NextTrigger.of(alarm("02:30"), at("2026-10-25T01:00"))!!
        assertEquals(ZoneOffset.ofHours(2), first.offset)

        // Just after it rang, the next trigger is the following day, not the second 02:30.
        val next = NextTrigger.of(alarm("02:30"), first.plusSeconds(1))!!
        assertEquals(at("2026-10-26T02:30"), next)
    }

    @Test
    fun `daily alarm keeps its wall-clock time across a DST change`() {
        val trigger = NextTrigger.of(alarm("06:30", *weekdays.plus(setOf(SATURDAY, SUNDAY)).toTypedArray()), at("2026-10-25T01:00"))!!
        assertEquals(LocalDateTime.parse("2026-10-25T06:30"), trigger.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(1), trigger.offset)
    }

    @Test
    fun `time zone change moves the alarm to local time in the new zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val now = at("2026-10-31T21:00").withZoneSameInstant(tokyo) // 05:00 on 1 November in Tokyo
        assertEquals(at("2026-11-01T06:30", tokyo), NextTrigger.of(alarm("06:30"), now))
    }

    @Test
    fun `upcoming picks the alarm that rings first`() {
        val work = alarm("06:30", *weekdays.toTypedArray()).copy(id = 1)
        val weekend = alarm("08:15", SATURDAY, SUNDAY).copy(id = 2)
        val off = alarm("05:10", enabled = false).copy(id = 3)
        val upcoming = NextTrigger.upcoming(listOf(work, weekend, off), at("2026-10-31T23:18"))!!
        assertEquals(2L, upcoming.alarm.id)
        assertEquals(at("2026-11-01T08:15"), upcoming.triggerAt)
    }

    @Test
    fun `upcoming is null when every alarm is off`() {
        assertNull(NextTrigger.upcoming(listOf(alarm("06:30", enabled = false)), at("2026-10-31T23:18")))
    }

    @Test
    fun `a snooze rings before the regular trigger`() {
        val snoozed = alarm("06:30", *weekdays.toTypedArray()).copy(snoozedUntil = at("2026-11-02T06:40").toInstant())
        assertEquals(at("2026-11-02T06:40").toInstant(), NextTrigger.of(snoozed, at("2026-11-02T06:31"))?.toInstant())
    }

    @Test
    fun `a snoozed one-time alarm rings although it is off`() {
        val snoozed = alarm("06:30", enabled = false).copy(snoozedUntil = at("2026-11-02T06:40").toInstant())
        assertEquals(at("2026-11-02T06:40").toInstant(), NextTrigger.of(snoozed, at("2026-11-02T06:31"))?.toInstant())
    }

    @Test
    fun `a snooze that passed while the phone was off still rings`() {
        val snoozed = alarm("06:30", enabled = false).copy(snoozedUntil = at("2026-11-02T06:40").toInstant())
        assertEquals(at("2026-11-02T06:40").toInstant(), NextTrigger.of(snoozed, at("2026-11-02T07:15"))?.toInstant())
    }

    @Test
    fun `random alarms always ring after now, within a week, on a repeat day, at their time`() {
        val random = Random(42)
        val zones = listOf(berlin, ZoneId.of("America/New_York"), ZoneId.of("Australia/Lord_Howe"), ZoneId.of("UTC"))
        repeat(10_000) {
            val zone = zones.random(random)
            val days =
                java.time.DayOfWeek.entries
                    .filter { random.nextBoolean() }
                    .toTypedArray()
            val time = LocalTime.of(random.nextInt(24), random.nextInt(60))
            val now = at("2026-01-01T00:00", zone).plusMinutes(random.nextLong(60L * 24 * 730))
            val trigger = NextTrigger.of(alarm(time.toString(), *days), now)!!

            assertTrue("$trigger after $now", trigger.isAfter(now))
            assertTrue("$trigger within a week of $now", trigger.isBefore(now.plusDays(8)))
            if (days.isNotEmpty()) assertTrue(trigger.dayOfWeek in days)
            // Only a DST gap may move the wall-clock time, and only forward.
            val wallClockMatches = trigger.toLocalTime() == time
            val shiftedByGap = zone.rules.isValidOffset(trigger.toLocalDate().atTime(time), trigger.offset).not()
            assertTrue("$time -> $trigger", wallClockMatches || shiftedByGap)
        }
    }
}
