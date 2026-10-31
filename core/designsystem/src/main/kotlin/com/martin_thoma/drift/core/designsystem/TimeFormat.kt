package com.martin_thoma.drift.core.designsystem

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle

/**
 * Times as the user reads them: 24-hour or 12-hour as the phone is set, and in the digits of
 * the app's language (e.g. Eastern Arabic digits in Arabic), like every other number on screen.
 */
@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val locale = LocalConfiguration.current.locales[0]
    return remember(is24Hour, locale) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale).withDecimalStyle(DecimalStyle.of(locale))
    }
}

/** Dates in the language's own order and digits, from a skeleton such as "EEEEdMMMM". */
@Composable
fun rememberDateFormatter(skeleton: String): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(skeleton, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).withDecimalStyle(DecimalStyle.of(locale))
    }
}
