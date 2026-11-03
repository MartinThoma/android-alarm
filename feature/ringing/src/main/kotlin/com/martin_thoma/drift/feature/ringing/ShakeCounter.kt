package com.martin_thoma.drift.feature.ringing

import kotlin.math.sqrt

/**
 * Counts shakes from accelerometer readings. A phone at rest measures 1 g (gravity); a shake
 * is a jolt above [thresholdG]. Jolts closer together than [minGapNanos] are one shake, so a
 * single swing that rattles the sensor counts once. The vibrating phone stays far below.
 */
internal class ShakeCounter(
    shakes: Int = 0,
    private val thresholdG: Float = THRESHOLD_G,
    private val minGapNanos: Long = MIN_GAP_NANOS,
) {
    /** Shakes so far; starts at the count before a rotation. */
    var shakes = shakes
        private set

    private var lastShakeNanos: Long? = null

    /** Feeds one reading in m/s²; returns whether it counted as a new shake. */
    fun onReading(
        x: Float,
        y: Float,
        z: Float,
        timestampNanos: Long,
    ): Boolean {
        val g = sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY
        if (g < thresholdG) return false
        val last = lastShakeNanos
        if (last != null && timestampNanos - last < minGapNanos) return false
        lastShakeNanos = timestampNanos
        shakes++
        return true
    }

    companion object {
        /** How many shakes dismiss the alarm: a few seconds of shaking. */
        const val NEEDED = 15

        /** A firm shake reaches 2 to 3 g; walking or a dropped pillow stays below 2 g. To tune on the S25. */
        const val THRESHOLD_G = 2.0f
        const val MIN_GAP_NANOS = 250_000_000L
        private const val STANDARD_GRAVITY = 9.80665f
    }
}
