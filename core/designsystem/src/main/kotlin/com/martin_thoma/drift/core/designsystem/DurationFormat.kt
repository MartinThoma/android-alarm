package com.martin_thoma.drift.core.designsystem

import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * A duration as a clock shows it: "04:05", "1:04:05", and with [hundredths] "04:05.27", using
 * the language's digits and decimal separator. Negative durations are shown as their amount.
 */
fun formatDuration(
    millis: Long,
    locale: Locale,
    hundredths: Boolean = false,
): String {
    val total = kotlin.math.abs(millis)
    val hours = total / 3_600_000
    val minutes = total / 60_000 % 60
    val seconds = total / 1_000 % 60
    val clock =
        if (hours >
            0
        ) {
            String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(locale, "%02d:%02d", minutes, seconds)
        }
    if (!hundredths) return clock
    return clock + DecimalFormatSymbols.getInstance(locale).decimalSeparator + String.format(locale, "%02d", total / 10 % 100)
}
