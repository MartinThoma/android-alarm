package com.martin_thoma.tide.core.designsystem

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Where the shoreline sits, as a fraction of the screen height from the top.
 * The water rises as the next alarm (or a timer's end) gets closer.
 */
object TideLevel {
    /** Low tide: nothing is due within [HORIZON]. */
    const val LOW = 0.18f

    /** High tide: the alarm rings now. */
    const val HIGH = 0.85f

    /** How far ahead the tide starts to rise. */
    val HORIZON = 12.hours

    /** The level for an event that is [timeUntil] away. Past events count as now. */
    fun forTimeUntil(timeUntil: Duration): Float {
        val clamped = timeUntil.coerceIn(Duration.ZERO, HORIZON)
        val progress = 1f - (clamped / HORIZON).toFloat()
        return LOW + (HIGH - LOW) * progress
    }

    /**
     * The lowest level at which the open water (beyond the shallows) still reaches [bottomPx] on a
     * surface [heightPx] tall. For screens where the tide only sets the scene, not the time.
     */
    fun covering(
        bottomPx: Float,
        heightPx: Float,
        density: Float,
    ): Float {
        if (heightPx <= 0f) return LOW
        val target = bottomPx + COVER_MARGIN_DP * density
        // The shallows are a quarter of the water, at least 44 dp (see shallowsPx).
        val water = maxOf(target + 44f * density, target / 0.75f)
        return (water / heightPx).coerceIn(LOW, HIGH)
    }

    private const val COVER_MARGIN_DP = 8f

    /** The level for a running timer that has used up [elapsedFraction] (0 to 1) of its time. */
    fun forProgress(elapsedFraction: Float): Float = LOW + (HIGH - LOW) * elapsedFraction.coerceIn(0f, 1f)
}
