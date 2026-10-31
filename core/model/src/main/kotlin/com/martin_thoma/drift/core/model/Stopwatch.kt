package com.martin_thoma.drift.core.model

/**
 * A stopwatch, kept as timestamps rather than a counter, so it keeps the right time while the
 * app is closed or its process is killed (requirements SW-03). All times are epoch milliseconds.
 */
data class Stopwatch(
    /** Time on the stopwatch before the current run started. */
    val accumulatedMillis: Long = 0,
    /** When the current run started, or `null` while paused or reset. */
    val runningSince: Long? = null,
    /** The total time at each lap mark, oldest first. */
    val lapMarks: List<Long> = emptyList(),
) {
    val isRunning: Boolean get() = runningSince != null

    val isReset: Boolean get() = this == Stopwatch()

    fun elapsed(now: Long): Long = accumulatedMillis + (runningSince?.let { (now - it).coerceAtLeast(0) } ?: 0)

    fun start(now: Long): Stopwatch = if (isRunning) this else copy(runningSince = now)

    fun pause(now: Long): Stopwatch = if (isRunning) copy(accumulatedMillis = elapsed(now), runningSince = null) else this

    fun lap(now: Long): Stopwatch = if (isRunning) copy(lapMarks = lapMarks + elapsed(now)) else this

    fun reset(): Stopwatch = Stopwatch()

    /** Completed laps, oldest first. */
    fun laps(): List<Lap> =
        lapMarks.mapIndexed {
            i,
            mark,
            ->
            Lap(number = i + 1, millis = mark - (lapMarks.getOrNull(i - 1) ?: 0), total = mark)
        }

    data class Lap(
        val number: Int,
        val millis: Long,
        val total: Long,
    )
}

/** The fastest and slowest of [laps], or `null` while there are fewer than two, when there is nothing to compare. */
fun fastestAndSlowest(laps: List<Stopwatch.Lap>): Pair<Stopwatch.Lap, Stopwatch.Lap>? =
    if (laps.size < 2) null else laps.minBy { it.millis } to laps.maxBy { it.millis }
