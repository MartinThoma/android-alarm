package com.martin_thoma.tide.core.domain

import com.martin_thoma.tide.core.model.Alarm
import java.time.ZonedDateTime

/** An alarm together with the moment it rings next. */
data class UpcomingAlarm(
    val alarm: Alarm,
    val triggerAt: ZonedDateTime,
)

/**
 * When alarms ring next. Pure functions of (alarm, now, time zone of `now`), so scheduling
 * can be rebuilt from the database at any time.
 *
 * Daylight saving time:
 * - An alarm time that does not exist (clocks jump from 02:00 to 03:00, alarm at 02:30)
 *   rings one gap later on the new clock (03:30), so the same amount of sleep passes.
 * - An alarm time that exists twice (clocks fall back from 03:00 to 02:00, alarm at 02:30)
 *   rings once, at the first occurrence.
 */
object NextTrigger {
    /** The next moment strictly after [now] at which [alarm] rings, or `null` if it is off. */
    fun of(
        alarm: Alarm,
        now: ZonedDateTime,
    ): ZonedDateTime? {
        if (!alarm.enabled) return null
        val today = now.toLocalDate()
        // Seven days ahead covers every weekday; one more covers "today, but already passed".
        for (daysAhead in 0L..7L) {
            val date = today.plusDays(daysAhead)
            if (alarm.isRepeating && date.dayOfWeek !in alarm.repeatDays) continue
            // ofLocal with no preferred offset: earlier offset in an overlap, shifted forward in a gap.
            val candidate = ZonedDateTime.ofLocal(date.atTime(alarm.time), now.zone, null)
            if (candidate.isAfter(now)) return candidate
        }
        error("No trigger within 8 days for enabled alarm $alarm")
    }

    /** The alarm that rings first after [now], or `null` if none is on. */
    fun upcoming(
        alarms: Iterable<Alarm>,
        now: ZonedDateTime,
    ): UpcomingAlarm? =
        alarms
            .mapNotNull { alarm -> of(alarm, now)?.let { UpcomingAlarm(alarm, it) } }
            .minByOrNull { it.triggerAt.toInstant() }
}
