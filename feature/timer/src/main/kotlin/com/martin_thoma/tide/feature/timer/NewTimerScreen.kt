package com.martin_thoma.tide.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.tide.core.designsystem.GlassPane
import com.martin_thoma.tide.core.designsystem.LocalTideColors
import com.martin_thoma.tide.core.designsystem.TideIcons
import com.martin_thoma.tide.core.designsystem.TideLevel
import com.martin_thoma.tide.core.designsystem.TideSurface
import com.martin_thoma.tide.core.designsystem.formatDuration
import com.martin_thoma.tide.core.model.Timer
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data object NewTimerRoute : NavKey

private const val MAX_DIGITS = 6

/** A further timer, while others run. */
@Composable
fun NewTimerScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TimerViewModel = hiltViewModel()
    NewTimerContent(
        onStart = { millis, label ->
            viewModel.create(millis, label)
            onDone()
        },
        onBack = onDone,
        modifier = modifier,
    )
}

/** The number pad (requirements TM-01): digits fill from the right, so 1-0-0-0 is 10:00. */
@Composable
internal fun NewTimerContent(
    onStart: (millis: Long, label: String) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var digits by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    val colors = LocalTideColors.current
    val locale = LocalConfiguration.current.locales[0]
    val millis = Timer.durationFromDigits(digits)

    TideSurface(level = TideLevel.LOW, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(TideIcons.ArrowBack),
                            contentDescription = stringResource(R.string.feature_timer_back),
                            tint = colors.onSea,
                        )
                    }
                }
            }
            Display(digits, locale, Modifier.semantics { contentDescription = formatDuration(millis, locale) })
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text(stringResource(R.string.feature_timer_label)) },
                singleLine = true,
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.glass,
                        unfocusedContainerColor = colors.glass,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.glassRim,
                    ),
            )
            Spacer(Modifier.weight(1f))
            Keypad(
                locale = locale,
                onDigits = { typed -> if (digits.length < MAX_DIGITS) digits = (digits + typed).take(MAX_DIGITS).trimStart('0') },
                onBackspace = { digits = digits.dropLast(1) },
            )
            Spacer(Modifier.height(24.dp))
            FilledIconButton(
                onClick = { onStart(millis, label) },
                enabled = millis > 0,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.accent, contentColor = colors.onAccent),
            ) {
                Icon(
                    painterResource(TideIcons.Play),
                    contentDescription = stringResource(R.string.feature_timer_start),
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** hh:mm:ss, the typed digits strong and the zeros before them faint. */
@Composable
private fun Display(
    digits: String,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val padded = digits.padStart(MAX_DIGITS, '0')
    val text = String.format(locale, "%s:%s:%s", padded.substring(0, 2), padded.substring(2, 4), padded.substring(4, 6))
    var digitIndex = 0
    val firstTyped = MAX_DIGITS - digits.length
    Text(
        text =
            buildAnnotatedString {
                for (char in text) {
                    val typed = if (char == ':') digitIndex > firstTyped else digitIndex++ >= firstTyped
                    withStyle(SpanStyle(color = if (typed) colors.onSea else colors.onSea.copy(alpha = 0.4f))) {
                        append(localizeDigit(char, locale))
                    }
                }
            },
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
        modifier = modifier,
    )
}

private fun localizeDigit(
    char: Char,
    locale: Locale,
): String = if (char.isDigit()) String.format(locale, "%d", char.digitToInt()) else char.toString()

@Composable
private fun Keypad(
    locale: Locale,
    onDigits: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", null))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    GlassPane(
                        modifier = Modifier.size(width = 80.dp, height = 64.dp),
                        shape = CircleShape,
                        onClick = { if (key == null) onBackspace() else onDigits(key) },
                    ) {
                        if (key == null) {
                            Icon(
                                painterResource(TideIcons.Backspace),
                                contentDescription = stringResource(R.string.feature_timer_backspace),
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            Text(
                                key.map { localizeDigit(it, locale) }.joinToString(""),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}
