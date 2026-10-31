package com.martin_thoma.tide.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

private const val TIDE_CHANGE_MILLIS = 4_000
private const val CONTENT_COLOR_MILLIS = 1_200

/** Where the water is on the nearest [TideSurface], for [TideAware] content. */
@Stable
class TideState internal constructor(
    internal val level: State<Float>,
    private val density: Float,
    internal val motion: TideMotion,
) {
    internal var coordinates: LayoutCoordinates? = null
    internal var size by mutableStateOf(Size.Zero)
    internal val heightPx get() = size.height

    /**
     * Only the deep sea is dark enough for light text. The shallow band and the sand both
     * take dark text, so content near the shoreline stays readable.
     */
    internal fun isUnderwater(centerY: Float) =
        !centerY.isNaN() && centerY < heightPx * level.value - shallowsPx(heightPx * level.value, density)
}

val LocalTideState = staticCompositionLocalOf<TideState?> { null }

/**
 * A screen on the beach. The water moves smoothly to [level] (see [TideLevel]); content
 * inside [TideAware] switches between sea and sand colours as the water passes it.
 */
@Composable
fun TideSurface(
    level: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val animatedLevel = animateFloatAsState(level, tween(TIDE_CHANGE_MILLIS), label = "tide")
    val density = LocalDensity.current.density
    val motion = rememberTideMotion()
    val state = remember(animatedLevel, density, motion) { TideState(animatedLevel, density, motion) }
    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned {
                state.coordinates = it
                state.size = it.size.toSize()
            },
    ) {
        TideBackground(level = animatedLevel.value, motion = motion)
        CompositionLocalProvider(LocalTideState provides state) { content() }
    }
}

/**
 * Content whose colours depend on whether it currently lies under water. It sets
 * [LocalContentColor] and passes `underwater` on for anything else that should follow.
 */
@Composable
fun TideAware(
    modifier: Modifier = Modifier,
    content: @Composable (underwater: Boolean) -> Unit,
) {
    val tide = LocalTideState.current
    var centerY by remember { mutableFloatStateOf(Float.NaN) }
    val underwater by remember(tide) { derivedStateOf { tide?.isUnderwater(centerY) == true } }
    val colors = LocalTideColors.current
    val contentColor by animateColorAsState(
        targetValue = if (underwater) colors.onSea else colors.onSand,
        animationSpec = tween(CONTENT_COLOR_MILLIS),
        label = "tideContent",
    )
    Box(
        modifier =
            modifier.onGloballyPositioned { coordinates ->
                val surface = tide?.coordinates ?: return@onGloballyPositioned
                if (surface.isAttached && coordinates.isAttached) {
                    centerY = surface.localPositionOf(coordinates, Offset.Zero).y + coordinates.size.height / 2f
                }
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content(underwater) }
    }
}

/** The quieter text colour for secondary information, under water or on the sand. */
@Composable
fun tideVariantColor(underwater: Boolean): Color {
    val colors = LocalTideColors.current
    val target = if (underwater) colors.onSeaVariant else colors.onSandVariant
    return animateColorAsState(target, tween(CONTENT_COLOR_MILLIS), label = "tideVariant").value
}
