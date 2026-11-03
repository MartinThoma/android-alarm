package com.martin_thoma.drift.feature.ringing

import kotlin.random.Random

/**
 * A short sum for the maths challenge: hard enough to need a moment's thought, easy enough
 * to do half asleep. Answers are whole numbers from 0 to 99: never negative, at most two digits.
 */
internal data class MathProblem(
    val left: Int,
    val operator: Operator,
    val right: Int,
) {
    enum class Operator(
        val symbol: String,
    ) {
        PLUS("+"),
        MINUS("−"),
        TIMES("×"),
    }

    val answer: Int
        get() =
            when (operator) {
                Operator.PLUS -> left + right
                Operator.MINUS -> left - right
                Operator.TIMES -> left * right
            }

    companion object {
        /** How many sums dismiss the alarm. The alarm editor says "three" (`feature_alarm_challenge_math_hint`). */
        const val COUNT = 3

        /** Two-digit addition and subtraction, and the times tables from 3 to 9 (at most 81). */
        fun random(random: Random): MathProblem =
            when (Operator.entries.random(random)) {
                Operator.PLUS -> {
                    // The second number is at most 99 minus the first, so the sum stays below 100.
                    val left = random.nextInt(12, 88)
                    MathProblem(left, Operator.PLUS, random.nextInt(12, 100 - left))
                }

                Operator.MINUS -> {
                    // At least 10 left over: never negative, and never a trivial 0.
                    val left = random.nextInt(31, 100)
                    MathProblem(left, Operator.MINUS, random.nextInt(12, left - 9))
                }

                Operator.TIMES -> {
                    MathProblem(random.nextInt(3, 10), Operator.TIMES, random.nextInt(3, 10))
                }
            }

        /** The [COUNT] sums for one ring, the same for the same [seed] (so they survive a rotation). */
        fun series(seed: Long): List<MathProblem> {
            val random = Random(seed)
            return List(COUNT) { random(random) }
        }
    }
}
