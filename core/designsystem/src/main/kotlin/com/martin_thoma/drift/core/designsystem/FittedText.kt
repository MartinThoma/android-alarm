package com.martin_thoma.drift.core.designsystem

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * One line of text in a fixed space, such as a round button or a navigation label. It grows with
 * the user's font size until the space is full, then shrinks just enough to fit, instead of
 * breaking mid-word or spilling out.
 */
@Composable
fun FittedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val color = style.color.takeOrElse { LocalContentColor.current }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
    )
}
