package com.martin_thoma.drift.feature.ringing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathProblemTest {
    @Test
    fun `every answer is from 0 to 99`() {
        val random = Random(1)
        repeat(100_000) {
            val problem = MathProblem.random(random)
            assertTrue("$problem", problem.answer in 0..99)
        }
    }

    @Test
    fun `subtraction never goes below ten`() {
        val random = Random(2)
        repeat(10_000) {
            val problem = MathProblem.random(random)
            if (problem.operator == MathProblem.Operator.MINUS) assertTrue("$problem", problem.answer >= 10)
        }
    }

    @Test
    fun `all three kinds of sum come up`() {
        val random = Random(3)
        val operators = List(100) { MathProblem.random(random).operator }.toSet()
        assertEquals(MathProblem.Operator.entries.toSet(), operators)
    }

    @Test
    fun `the same seed gives the same sums, so they survive a rotation`() {
        assertEquals(MathProblem.series(42), MathProblem.series(42))
        assertEquals(MathProblem.COUNT, MathProblem.series(42).size)
    }

    @Test
    fun `answers are computed by the operator`() {
        assertEquals(85, MathProblem(47, MathProblem.Operator.PLUS, 38).answer)
        assertEquals(54, MathProblem(83, MathProblem.Operator.MINUS, 29).answer)
        assertEquals(56, MathProblem(7, MathProblem.Operator.TIMES, 8).answer)
    }
}
