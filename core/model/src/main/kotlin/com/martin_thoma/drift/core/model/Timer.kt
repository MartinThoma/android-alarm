package com.martin_thoma.drift.core.model

/**
 * A countdown timer, kept as timestamps like [Stopwatch], so it keeps counting while the app is
 * closed (requirements TM-02, TM-03). All times are epoch milliseconds.
 */
data class Timer(
    val id: Long = 0,
    val label: String = "",
    /** The duration the user set; reset returns to it. */
    val setMillis: Long,
    /** The full length of the current run, including added minutes. The tide shows progress against it. */
    val durationMillis: Long = setMillis,
    /** Time left while paused or not started. */
    val remainingMillis: Long = setMillis,
    /** When the current run ends, or `null` while paused or not started. */
    val endsAt: Long? = null,
    val createdAt: Long = 0,
) {
    init {
        require(setMillis > 0) { "A timer needs a duration: $setMillis" }
    }

    val isRunning: Boolean get() = endsAt != null

    /** Not started, or reset: showing the set duration. */
    val isIdle: Boolean get() = !isRunning && remainingMillis == setMillis && durationMillis == setMillis

    /** Time left; negative once it has run out (the overtime). */
    fun remaining(now: Long): Long = endsAt?.let { it - now } ?: remainingMillis

    fun hasExpired(now: Long): Boolean = isRunning && remaining(now) <= 0

    /** How much of the run has passed, from 0 to 1. */
    fun progress(now: Long): Float = (1f - remaining(now).toFloat() / durationMillis).coerceIn(0f, 1f)

    fun start(now: Long): Timer = if (isRunning) this else copy(endsAt = now + remainingMillis)

    fun pause(now: Long): Timer = if (isRunning && !hasExpired(now)) copy(remainingMillis = remaining(now), endsAt = null) else this

    fun reset(): Timer = copy(durationMillis = setMillis, remainingMillis = setMillis, endsAt = null)

    /** One more minute (requirements TM-02). A timer that has run out starts again with one minute. */
    fun addMinute(now: Long): Timer =
        when {
            hasExpired(now) -> copy(durationMillis = MINUTE, remainingMillis = MINUTE, endsAt = now + MINUTE)
            isRunning -> copy(durationMillis = durationMillis + MINUTE, endsAt = endsAt!! + MINUTE)
            else -> copy(durationMillis = durationMillis + MINUTE, remainingMillis = remainingMillis + MINUTE)
        }

    companion object {
        const val MINUTE = 60_000L

        /** The duration typed on the number pad: "1000" is 10:00, "13000" is 1:30:00 (requirements TM-01). */
        fun durationFromDigits(digits: String): Long {
            val padded = digits.takeLast(6).padStart(6, '0')
            val hours = padded.substring(0, 2).toLong()
            val minutes = padded.substring(2, 4).toLong()
            val seconds = padded.substring(4, 6).toLong()
            return ((hours * 60 + minutes) * 60 + seconds) * 1_000
        }
    }
}
