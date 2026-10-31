package com.martin_thoma.tide.core.model

import java.time.DayOfWeek
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
) {
    init {
        require(time.second == 0 && time.nano == 0) { "Alarms are set to the minute: $time" }
        require(volumePercent in 0..100) { "volumePercent out of range: $volumePercent" }
        require(snoozeMinutes in 1..60) { "snoozeMinutes out of range: $snoozeMinutes" }
    }

    val isRepeating: Boolean get() = repeatDays.isNotEmpty()

    companion object {
        const val DEFAULT_VOLUME_PERCENT = 70
        const val DEFAULT_SNOOZE_MINUTES = 10
    }
}
