package com.martin_thoma.tide.feature.alarm

import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime

/** How long until an alarm rings, rounded up to whole minutes so it never reads "in 0 min". */
internal sealed interface TimeUntil {
    data class Minutes(
        val minutes: Long,
    ) : TimeUntil

    data class HoursMinutes(
        val hours: Long,
        val minutes: Long,
    ) : TimeUntil

    data class DaysHours(
        val days: Long,
        val hours: Long,
    ) : TimeUntil
}

internal fun timeUntil(
    now: ZonedDateTime,
    trigger: ZonedDateTime,
): TimeUntil {
    val seconds = Duration.between(now, trigger).seconds.coerceAtLeast(0)
    val totalMinutes = ((seconds + 59) / 60).coerceAtLeast(1)
    val totalHours = totalMinutes / 60
    return when {
        totalHours >= 24 -> TimeUntil.DaysHours(totalHours / 24, totalHours % 24)
        totalHours >= 1 -> TimeUntil.HoursMinutes(totalHours, totalMinutes % 60)
        else -> TimeUntil.Minutes(totalMinutes)
    }
}

/** How an alarm's repeat days read in the list. */
internal sealed interface RepeatLabel {
    data object Once : RepeatLabel

    data object EveryDay : RepeatLabel

    /** Consecutive days, e.g. Monday to Friday. */
    data class Range(
        val first: DayOfWeek,
        val last: DayOfWeek,
    ) : RepeatLabel

    data class Days(
        val days: List<DayOfWeek>,
    ) : RepeatLabel
}

/** Ranges need at least three days; "Sat, Sun" reads better than "Sat–Sun". */
internal fun repeatLabel(days: Set<DayOfWeek>): RepeatLabel {
    // The week starts on Monday until the week-start setting exists (requirements GN-02).
    val sorted = days.sortedBy { it.value }
    return when {
        sorted.isEmpty() -> {
            RepeatLabel.Once
        }

        sorted.size == 7 -> {
            RepeatLabel.EveryDay
        }

        sorted.size >= 3 && sorted.last().value - sorted.first().value == sorted.size - 1 -> {
            RepeatLabel.Range(sorted.first(), sorted.last())
        }

        else -> {
            RepeatLabel.Days(sorted)
        }
    }
}
