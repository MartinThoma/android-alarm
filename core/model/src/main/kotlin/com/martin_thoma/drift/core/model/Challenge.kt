package com.martin_thoma.drift.core.model

/**
 * What it takes to turn a ringing alarm off (requirements AL-21). Snooze is always a single tap;
 * only dismissing asks for more, so a sleepy hand cannot end the alarm by accident.
 */
enum class Challenge(
    /**
     * Whether the challenge is there to wake the user up, not only to prevent an accidental touch.
     * Such a challenge can only be met on the ringing screen: the notifications offer no Dismiss.
     */
    val wakesUp: Boolean,
) {
    /** Slide the handle to the end. */
    SLIDE(wakesUp = false),

    /** Press and hold a big circle in the middle of the screen. */
    HOLD(wakesUp = false),

    /** Shake the phone until the bar is full. */
    SHAKE(wakesUp = true),

    /** Solve a few short sums. */
    MATH(wakesUp = true),
}
