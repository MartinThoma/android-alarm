package com.martin_thoma.drift.core.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/**
 * A user's alarm. An empty [repeatDays] means a one-time alarm: it rings at the next
 * occurrence of [time] and is then turned off.
 */
data class Alarm(
    val id: Long = 0,
    val time: LocalTime,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
    val label: String = "",
    /** Level of the alarm stream while ringing, 0 to 100 (requirements AL-05, approach A). */
    val volumePercent: Int = DEFAULT_VOLUME_PERCENT,
    /** A content URI chosen by the user, or `null` for the bundled default sound. */
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    /** Set while the alarm is snoozed: it rings again at this moment, even if it is turned off. */
    val snoozedUntil: Instant? = null,
    /** Screen brightness of the ringing screen, 1 to 100, or `null` to keep the phone's own. */
    val brightnessPercent: Int? = null,
    /** Whether, and how fast, the ringing screen pulses with light. */
    val blink: Blink = Blink.OFF,
) {
    init {
        require(time.second == 0 && time.nano == 0) { "Alarms are set to the minute: $time" }
        require(volumePercent in 0..100) { "volumePercent out of range: $volumePercent" }
        require(snoozeMinutes in 1..60) { "snoozeMinutes out of range: $snoozeMinutes" }
        require(brightnessPercent == null || brightnessPercent in 1..100) { "brightnessPercent out of range: $brightnessPercent" }
    }

    val isRepeating: Boolean get() = repeatDays.isNotEmpty()

    /** Whether the alarm will ring again: it is on, or a snooze is pending. */
    val isActive: Boolean get() = enabled || snoozedUntil != null

    /** The alarm once it has started ringing: a pending snooze is used up, a one-time alarm turns off. */
    fun afterRinging(): Alarm = copy(enabled = enabled && isRepeating, snoozedUntil = null)

    companion object {
        const val DEFAULT_VOLUME_PERCENT = 70
        const val DEFAULT_SNOOZE_MINUTES = 10
    }
}
