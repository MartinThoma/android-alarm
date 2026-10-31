package com.martin_thoma.drift.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TideLevelTest {
    @Test
    fun `far away alarm is low tide`() {
        assertEquals(TideLevel.LOW, TideLevel.forTimeUntil(TideLevel.HORIZON), DELTA)
        assertEquals(TideLevel.LOW, TideLevel.forTimeUntil(30.hours), DELTA)
    }

    @Test
    fun `alarm ringing now is high tide`() {
        assertEquals(TideLevel.HIGH, TideLevel.forTimeUntil(0.minutes), DELTA)
    }

    @Test
    fun `past alarm counts as now`() {
        assertEquals(TideLevel.HIGH, TideLevel.forTimeUntil((-5).minutes), DELTA)
    }

    @Test
    fun `tide is halfway at half the horizon`() {
        assertEquals((TideLevel.LOW + TideLevel.HIGH) / 2, TideLevel.forTimeUntil(6.hours), DELTA)
    }

    @Test
    fun `water rises as the alarm gets closer`() {
        val levels = listOf(11.hours, 7.hours, 2.hours, 25.minutes).map(TideLevel::forTimeUntil)
        assertTrue(levels.zipWithNext().all { (earlier, later) -> later > earlier })
    }

    @Test
    fun `timer progress maps onto the same range and is clamped`() {
        assertEquals(TideLevel.LOW, TideLevel.forProgress(-1f), DELTA)
        assertEquals(TideLevel.HIGH, TideLevel.forProgress(2f), DELTA)
        assertEquals((TideLevel.LOW + TideLevel.HIGH) / 2, TideLevel.forProgress(0.5f), DELTA)
    }

    private companion object {
        const val DELTA = 1e-6f
    }
}
