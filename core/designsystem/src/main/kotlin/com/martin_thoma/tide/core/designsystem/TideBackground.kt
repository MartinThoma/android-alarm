package com.martin_thoma.tide.core.designsystem

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.sin

private const val LAP_MILLIS = 7_000
private const val SWELL_MILLIS = 11_000
private const val WAVE_SEGMENTS = 64

/**
 * A beach seen from above: sea at the top, sand below. [level] is the shoreline's
 * position as a fraction of the height (see [TideLevel]). The water laps slowly,
 * and stands still when the system's animations are turned off.
 */
@Composable
fun TideBackground(
    level: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    val context = LocalContext.current
    val animate =
        remember(context) {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }

    var lap = 0f
    var swell = 0f
    if (animate) {
        val transition = rememberInfiniteTransition(label = "waves")
        lap = transition.animateFloat(0f, 1f, waveSpec(LAP_MILLIS), label = "lap").value
        swell = transition.animateFloat(0f, 1f, waveSpec(SWELL_MILLIS), label = "swell").value
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val y0 = size.height * level
        val lapOffset = lap * 5f * density
        val swellOffset = -swell * 3f * density
        drawRect(colors.sand)
        drawWater(colors.wetSand, y0 + 16f * density + lapOffset, amplitude = 5f, wavelength = 230f, phase = 0.9f)
        drawWater(colors.shallow, y0 + lapOffset, amplitude = 6f, wavelength = 195f, phase = 0f)
        drawPath(
            path = shoreline(y0 + lapOffset, amplitude = 6f, wavelength = 195f, phase = 0f, closed = false),
            color = colors.foam,
            style = Stroke(width = 2f * density, cap = StrokeCap.Round),
        )
        drawWater(colors.sea, y0 - 30f * density + swellOffset, amplitude = 5f, wavelength = 260f, phase = 2.1f)
    }
}

private fun waveSpec(millis: Int) = infiniteRepeatable<Float>(tween(millis, easing = FastOutSlowInEasing), RepeatMode.Reverse)

private fun DrawScope.drawWater(
    color: Color,
    y: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float,
) = drawPath(shoreline(y, amplitude, wavelength, phase, closed = true), color)

/** A gently wavy line across the width at height [y]; closed, it fills the water above it. */
private fun DrawScope.shoreline(
    y: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float,
    closed: Boolean,
): Path {
    val a = amplitude * density
    val l = wavelength * density

    fun waveY(x: Float): Float =
        y + a * sin(2 * PI * x / l + phase).toFloat() + 0.4f * a * sin(2 * PI * x / (0.47f * l) + 1.7f * phase).toFloat()

    return Path().apply {
        moveTo(0f, waveY(0f))
        for (i in 1..WAVE_SEGMENTS) {
            val x = size.width * i / WAVE_SEGMENTS
            lineTo(x, waveY(x))
        }
        if (closed) {
            lineTo(size.width, 0f)
            lineTo(0f, 0f)
            close()
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TideBackgroundPreview() {
    TideTheme(darkTheme = false) { TideBackground(level = 0.45f) }
}
