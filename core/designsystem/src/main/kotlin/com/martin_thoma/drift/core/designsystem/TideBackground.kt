package com.martin_thoma.drift.core.designsystem

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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val LAP_MILLIS = 7_000
private const val SWELL_MILLIS = 11_000
private const val WAVE_SEGMENTS = 64

private const val MIN_SHALLOWS_DP = 44f
private const val SHALLOWS_SHARE = 0.25f

/** The bright shallows between the deep sea and the shore: a quarter of the water, never less than 44 dp. */
internal fun shallowsPx(
    waterPx: Float,
    density: Float,
): Float = maxOf(MIN_SHALLOWS_DP * density, SHALLOWS_SHARE * waterPx)

/** The slow lap and swell of the water, shared by the background and every glass pane over it. */
@Stable
class TideMotion internal constructor(
    internal val lap: State<Float>,
    internal val swell: State<Float>,
)

/** Waves that move, or stand still when the system's animations are turned off. */
@Composable
fun rememberTideMotion(): TideMotion {
    val context = LocalContext.current
    val animate =
        remember(context) {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
    if (!animate) return remember { TideMotion(mutableFloatStateOf(0f), mutableFloatStateOf(0f)) }
    val transition = rememberInfiniteTransition(label = "waves")
    val lap = transition.animateFloat(0f, 1f, waveSpec(LAP_MILLIS), label = "lap")
    val swell = transition.animateFloat(0f, 1f, waveSpec(SWELL_MILLIS), label = "swell")
    return remember(lap, swell) { TideMotion(lap, swell) }
}

/**
 * A lagoon seen from above: water at the top, sand below. [level] is the shoreline's
 * position as a fraction of the height (see [TideLevel]).
 */
@Composable
fun TideBackground(
    level: Float,
    modifier: Modifier = Modifier,
    motion: TideMotion = rememberTideMotion(),
) {
    val colors = LocalTideColors.current
    Canvas(modifier = modifier.fillMaxSize()) {
        drawTide(colors, level, motion, size)
    }
}

/**
 * Draws the beach for an [area] (the whole [TideSurface]); a [GlassPane] draws the same
 * beach, shifted to its own position, to blur it.
 */
internal fun DrawScope.drawTide(
    colors: TideColors,
    level: Float,
    motion: TideMotion,
    area: Size,
) {
    val waterline = area.height * level + motion.lap.value * 5f * density
    val water = area.height * level
    val deepEdge = water - shallowsPx(water, density) - motion.swell.value * 3f * density
    val night = colors.glow != Color.Transparent

    drawRect(colors.sand, size = area)
    drawWater(colors.wetSand, area.width, waterline + 22f * density, amplitude = 6f, wavelength = 230f, phase = 0.9f)
    // The shallows brighten towards the shore, where the sand shows through.
    drawWater(
        Brush.verticalGradient(0f to colors.shallowFar, 1f to colors.shallow, startY = deepEdge, endY = waterline),
        area.width,
        waterline,
        amplitude = 6f,
        wavelength = 195f,
        phase = 0f,
    )
    // Foam lace: a soft band, lines the last waves left behind, and the edge. By night it glows.
    val halo = if (night) colors.glow else colors.foam
    val shore = waveLine(area.width, waterline, amplitude = 6f, wavelength = 195f, phase = 0f, closed = false)
    drawPath(
        shore,
        halo.copy(alpha = if (night) 0.22f else 0.35f),
        style = Stroke(width = (if (night) 24f else 16f) * density, cap = StrokeCap.Round),
    )
    val lace = waveLine(area.width, waterline - 9f * density, amplitude = 4f, wavelength = 120f, phase = 1.3f, closed = false)
    if (night) drawPath(lace, halo.copy(alpha = 0.18f), style = Stroke(width = 10f * density, cap = StrokeCap.Round))
    drawPath(lace, colors.foam.copy(alpha = 0.7f), style = Stroke(width = 2.5f * density, cap = StrokeCap.Round))
    if (night) {
        val far = waveLine(area.width, waterline - 26f * density, amplitude = 5f, wavelength = 160f, phase = 2.6f, closed = false)
        drawPath(far, halo.copy(alpha = 0.12f), style = Stroke(width = 12f * density, cap = StrokeCap.Round))
        drawPath(far, colors.foam.copy(alpha = 0.35f), style = Stroke(width = 1.5f * density, cap = StrokeCap.Round))
    }
    drawPath(shore, colors.foam, style = Stroke(width = 3.5f * density, cap = StrokeCap.Round))

    // The open water darkens with depth, away from the shore.
    drawWater(
        Brush.verticalGradient(0f to colors.deepSea, 1f to colors.sea, startY = 0f, endY = deepEdge),
        area.width,
        deepEdge,
        amplitude = 5f,
        wavelength = 260f,
        phase = 2.1f,
    )
    val drop = waveLine(area.width, deepEdge, amplitude = 5f, wavelength = 260f, phase = 2.1f, closed = false)
    if (night) {
        drawPath(drop, colors.glow.copy(alpha = 0.08f), style = Stroke(width = 10f * density, cap = StrokeCap.Round))
        drawPath(drop, colors.glow.copy(alpha = 0.35f), style = Stroke(width = 1.2f * density, cap = StrokeCap.Round))
        drawJellyfish(colors.glow, area.width, deepEdge, motion)
    } else {
        drawPath(drop, colors.foam.copy(alpha = 0.22f), style = Stroke(width = 1.5f * density, cap = StrokeCap.Round))
    }
}

/**
 * Two moon jellyfish (Aurelia aurita) seen from above, drifting faintly in the deep water at
 * night: low in the deep water and to the right, clear of the big times (left-aligned on the alarm
 * list, centred on the ringing screen) and of the corner where actions sit.
 */
private fun DrawScope.drawJellyfish(
    glow: Color,
    width: Float,
    deepEdge: Float,
    motion: TideMotion,
) {
    if (deepEdge < 200f * density) return
    val drift = motion.swell.value
    val pulse = motion.lap.value
    moonJelly(glow, Offset(width * (0.84f - 0.03f * drift), deepEdge * (0.68f + 0.03f * drift)), 30f * density, 12f + 14f * drift, pulse)
    moonJelly(glow, Offset(width * (0.62f + 0.03f * drift), deepEdge * (0.90f - 0.03f * drift)), 21f * density, -30f - 10f * drift, pulse)
}

/** A soft, translucent bell with four faint horseshoe rings, as a moon jellyfish looks from above. */
private fun DrawScope.moonJelly(
    glow: Color,
    center: Offset,
    radius: Float,
    rotation: Float,
    pulse: Float,
) {
    val r = radius * (1f + 0.04f * pulse)
    // No outline: the bell fades out into a feathered fringe.
    drawCircle(
        Brush.radialGradient(
            0f to glow.copy(alpha = 0.10f),
            0.7f to glow.copy(alpha = 0.07f),
            0.9f to glow.copy(alpha = 0.10f),
            1f to Color.Transparent,
            center = center,
            radius = r * 1.15f,
        ),
        r * 1.15f,
        center,
    )
    rotate(rotation, center) {
        for (k in 0 until 4) {
            val angle = PI / 4 + PI / 2 * k
            val ring = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (0.3f * r)
            drawCircle(
                Brush.radialGradient(
                    0.45f to Color.Transparent,
                    0.75f to glow.copy(alpha = 0.22f),
                    1f to Color.Transparent,
                    center = ring,
                    radius = 0.2f * r,
                ),
                0.2f * r,
                ring,
            )
        }
    }
}

private fun waveSpec(millis: Int) = infiniteRepeatable<Float>(tween(millis, easing = FastOutSlowInEasing), RepeatMode.Reverse)

private fun DrawScope.drawWater(
    color: Color,
    width: Float,
    y: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float,
) = drawPath(waveLine(width, y, amplitude, wavelength, phase, closed = true), color)

private fun DrawScope.drawWater(
    brush: Brush,
    width: Float,
    y: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float,
) = drawPath(waveLine(width, y, amplitude, wavelength, phase, closed = true), brush)

/** A gently wavy line across [width] at height [y]; closed, it encloses the water above it. */
private fun DrawScope.waveLine(
    width: Float,
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
            val x = width * i / WAVE_SEGMENTS
            lineTo(x, waveY(x))
        }
        if (closed) {
            lineTo(width, 0f)
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
