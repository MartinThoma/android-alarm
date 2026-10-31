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

    /** The level for a running timer that has used up [elapsedFraction] (0 to 1) of its time. */
    fun forProgress(elapsedFraction: Float): Float = LOW + (HIGH - LOW) * elapsedFraction.coerceIn(0f, 1f)
}
