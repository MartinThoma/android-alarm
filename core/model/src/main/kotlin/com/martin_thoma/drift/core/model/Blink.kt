package com.martin_thoma.drift.core.model

/**
 * How fast the ringing screen pulses with light, as a soft wave rather than a hard flash. At most
 * twice a second: WCAG 2.3.1 warns that more than three flashes a second can cause seizures.
 */
enum class Blink(
    val pulsesPerSecond: Float,
) {
    OFF(0f),
    SLOW(0.5f),
    MEDIUM(1f),
    FAST(2f),
}
