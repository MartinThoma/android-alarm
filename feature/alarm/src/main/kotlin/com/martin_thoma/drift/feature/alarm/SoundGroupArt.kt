package com.martin_thoma.drift.feature.alarm

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.martin_thoma.drift.core.designsystem.LocalTideColors
import com.martin_thoma.drift.core.ringing.SoundCategory

/** The kinds of sound in the picker, each with its own small picture. */
internal enum class SoundGroup(
    @param:StringRes val heading: Int,
    /** The built-in sounds of this group; [MINE] holds the user's own files instead. */
    val category: SoundCategory?,
) {
    DEFAULT(R.string.feature_alarm_sound_default, null),
    NATURE(R.string.feature_alarm_sound_nature, SoundCategory.NATURE),
    NOISY(R.string.feature_alarm_sound_noisy, SoundCategory.NOISY),
    CLASSICS(R.string.feature_alarm_sound_classics, SoundCategory.CLASSICS),
    MINE(R.string.feature_alarm_sound_custom, null),
}

/** The colours of one picture: a tile with a gentle gradient, a light main shape and two detail colours. */
private data class ArtColors(
    val tileTop: Color,
    val tileBottom: Color,
    val shape: Color,
    val detail: Color,
    val accent: Color,
)

private fun artColors(
    group: SoundGroup,
    night: Boolean,
): ArtColors =
    if (night) {
        // By night every picture glows, like the sea.
        val glow = Color(0xFF5FD4FF)
        val soft = Color(0xFFCFEFFF)
        when (group) {
            SoundGroup.DEFAULT -> ArtColors(Color(0xFF1A2531), Color(0xFF0F161E), soft, glow, Color(0xFF8FB4C8))
            SoundGroup.NATURE -> ArtColors(Color(0xFF0D2E45), Color(0xFF071A28), soft, glow, glow)
            SoundGroup.NOISY -> ArtColors(Color(0xFF2B1E2A), Color(0xFF1A1219), soft, Color(0xFFFF9E86), glow)
            SoundGroup.CLASSICS -> ArtColors(Color(0xFF13294D), Color(0xFF0A1730), soft, glow, glow)
            SoundGroup.MINE -> ArtColors(Color(0xFF1F242B), Color(0xFF14171B), Color(0xFF2E5A73), soft, glow)
        }
    } else {
        when (group) {
            SoundGroup.DEFAULT -> ArtColors(Color(0xFFF7E6C2), Color(0xFFE8C98C), Color(0xFFFFF8EC), Color(0xFFD9A86A), Color(0xFFE88E6E))
            SoundGroup.NATURE -> ArtColors(Color(0xFF4FC9D6), Color(0xFF1FA2B8), Color.White, Color(0xFFBFF3F7), Color(0xFF0B6E80))
            SoundGroup.NOISY -> ArtColors(Color(0xFFF8BE98), Color(0xFFEC8A66), Color.White, Color(0xFF8A3B2A), Color(0xFF0B2F37))
            SoundGroup.CLASSICS -> ArtColors(Color(0xFF3A8FC4), Color(0xFF1C5C93), Color.White, Color(0xFFBFE6FF), Color(0xFFFFD58A))
            SoundGroup.MINE -> ArtColors(Color(0xFFEDD7A8), Color(0xFFDDBB7E), Color(0xFF006A73), Color(0xFFFFF4DC), Color(0xFF0B2F37))
        }
    }

/** A small round picture for a group of sounds, drawn in the beach's colours by day and glowing by night. */
@Composable
internal fun SoundGroupArt(
    group: SoundGroup,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val night = LocalTideColors.current.glow != Color.Transparent
    val colors = artColors(group, night)
    Canvas(modifier.size(size).clip(CircleShape)) {
        val u = this.size.minDimension / 48f
        drawCircle(Brush.verticalGradient(listOf(colors.tileTop, colors.tileBottom)))
        if (night) drawCircle(colors.detail.copy(alpha = 0.35f), style = Stroke(width = 1f * density))
        when (group) {
            SoundGroup.DEFAULT -> shell(u, colors)
            SoundGroup.NATURE -> rainOverWave(u, colors)
            SoundGroup.NOISY -> alarmClock(u, colors)
            SoundGroup.CLASSICS -> notes(u, colors)
            SoundGroup.MINE -> cassette(u, colors)
        }
    }
}

private fun at(
    u: Float,
    x: Float,
    y: Float,
) = Offset(x * u, y * u)

/** A scallop shell: a fan of ribs above a small hinge. */
private fun DrawScope.shell(
    u: Float,
    c: ArtColors,
) {
    val fan =
        Path().apply {
            moveTo(24f * u, 36f * u)
            lineTo(11f * u, 23f * u)
            cubicTo(12f * u, 9f * u, 36f * u, 9f * u, 37f * u, 23f * u)
            close()
        }
    drawPath(fan, c.shape)
    for (k in 0..6) {
        val t = k / 6f
        val tip = Offset((12.5f + 23f * t) * u, (21f - 8f * (1 - (2 * t - 1) * (2 * t - 1))) * u)
        drawLine(c.detail, at(u, 24f, 35f), tip, strokeWidth = 1.3f * u, cap = StrokeCap.Round)
    }
    drawPath(fan, c.detail, style = Stroke(width = 1.2f * u))
    drawRoundRect(c.shape, at(u, 19.5f, 35f), Size(9f * u, 4f * u), CornerRadius(1.5f * u))
    drawRoundRect(c.detail, at(u, 19.5f, 35f), Size(9f * u, 4f * u), CornerRadius(1.5f * u), style = Stroke(width = 1.1f * u))
    drawCircle(c.accent, 1.6f * u, at(u, 34f, 12f))
}

/** A soft cloud with falling drops over a curling wave. */
private fun DrawScope.rainOverWave(
    u: Float,
    c: ArtColors,
) {
    for (x in listOf(19f, 25.5f, 32f)) {
        drawLine(c.detail, at(u, x, 22f), at(u, x - 1.5f, 26f), strokeWidth = 1.6f * u, cap = StrokeCap.Round)
    }
    drawCircle(c.shape, 5f * u, at(u, 20f, 16f))
    drawCircle(c.shape, 6.2f * u, at(u, 26f, 13.5f))
    drawCircle(c.shape, 4.5f * u, at(u, 31.5f, 16.5f))
    drawRoundRect(c.shape, at(u, 15f, 15f), Size(21f * u, 6f * u), CornerRadius(3f * u))
    val wave =
        Path().apply {
            moveTo(4f * u, 36f * u)
            cubicTo(10f * u, 28f * u, 18f * u, 28f * u, 21f * u, 33f * u)
            cubicTo(22f * u, 35f * u, 19f * u, 36f * u, 18f * u, 34f * u)
            cubicTo(23f * u, 37f * u, 30f * u, 30f * u, 38f * u, 31f * u)
            cubicTo(41f * u, 31.5f * u, 43f * u, 33f * u, 44f * u, 35f * u)
            lineTo(44f * u, 48f * u)
            lineTo(4f * u, 48f * u)
            close()
        }
    drawPath(wave, c.shape.copy(alpha = 0.92f))
    drawPath(
        Path().apply {
            moveTo(8f * u, 40f * u)
            cubicTo(16f * u, 37f * u, 26f * u, 41f * u, 40f * u, 38f * u)
        },
        c.accent.copy(alpha = 0.5f),
        style = Stroke(width = 1.4f * u, cap = StrokeCap.Round),
    )
}

/** A twin-bell alarm clock, just ringing. */
private fun DrawScope.alarmClock(
    u: Float,
    c: ArtColors,
) {
    drawLine(c.detail, at(u, 17f, 35f), at(u, 14.5f, 39f), strokeWidth = 2f * u, cap = StrokeCap.Round)
    drawLine(c.detail, at(u, 31f, 35f), at(u, 33.5f, 39f), strokeWidth = 2f * u, cap = StrokeCap.Round)
    drawArc(c.detail, 180f, 180f, true, at(u, 10f, 11f), Size(10f * u, 10f * u))
    drawArc(c.detail, 180f, 180f, true, at(u, 28f, 11f), Size(10f * u, 10f * u))
    ringMarks(u, c)
    drawCircle(c.shape, 11f * u, at(u, 24f, 26f))
    drawCircle(c.detail, 11f * u, at(u, 24f, 26f), style = Stroke(width = 1.4f * u))
    drawLine(c.accent, at(u, 24f, 26f), at(u, 24f, 19.5f), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
    drawLine(c.accent, at(u, 24f, 26f), at(u, 28.5f, 28f), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
    drawCircle(c.detail, 1.4f * u, at(u, 24f, 26f))
}

/** Little ringing marks beside the bells. */
private fun DrawScope.ringMarks(
    u: Float,
    c: ArtColors,
) {
    drawArc(c.shape, 200f, 50f, false, at(u, 6f, 8f), Size(12f * u, 12f * u), style = Stroke(width = 1.4f * u, cap = StrokeCap.Round))
    drawArc(c.shape, 290f, 50f, false, at(u, 30f, 8f), Size(12f * u, 12f * u), style = Stroke(width = 1.4f * u, cap = StrokeCap.Round))
}

/** Two beamed eighth notes and a small spark. */
private fun DrawScope.notes(
    u: Float,
    c: ArtColors,
) {
    val beam =
        Path().apply {
            moveTo(19.6f * u, 14.5f * u)
            lineTo(33.6f * u, 11f * u)
            lineTo(33.6f * u, 15f * u)
            lineTo(19.6f * u, 18.5f * u)
            close()
        }
    drawPath(beam, c.shape)
    drawLine(c.shape, at(u, 20.4f, 15f), at(u, 20.4f, 32f), strokeWidth = 1.8f * u)
    drawLine(c.shape, at(u, 32.8f, 12f), at(u, 32.8f, 29f), strokeWidth = 1.8f * u)
    noteHead(u, c.shape, 17.2f, 32.5f)
    noteHead(u, c.shape, 29.6f, 29.5f)
    drawLine(c.detail.copy(alpha = 0.6f), at(u, 12f, 38.5f), at(u, 38f, 38.5f), strokeWidth = 0.9f * u, cap = StrokeCap.Round)
    drawLine(c.detail.copy(alpha = 0.6f), at(u, 14f, 41.5f), at(u, 36f, 41.5f), strokeWidth = 0.9f * u, cap = StrokeCap.Round)
    drawCircle(c.accent, 1.5f * u, at(u, 12f, 14f))
    drawLine(c.accent, at(u, 12f, 10.5f), at(u, 12f, 17.5f), strokeWidth = 0.8f * u, cap = StrokeCap.Round)
    drawLine(c.accent, at(u, 8.5f, 14f), at(u, 15.5f, 14f), strokeWidth = 0.8f * u, cap = StrokeCap.Round)
}

private fun DrawScope.noteHead(
    u: Float,
    color: Color,
    x: Float,
    y: Float,
) {
    // A slightly tilted oval, as engraved notes have.
    val head =
        Path().apply {
            moveTo((x - 3.6f) * u, (y + 1.2f) * u)
            cubicTo((x - 3.8f) * u, (y - 2.2f) * u, (x + 3.4f) * u, (y - 3.4f) * u, (x + 3.6f) * u, (y - 1.2f) * u)
            cubicTo((x + 3.8f) * u, (y + 2.2f) * u, (x - 3.4f) * u, (y + 3.4f) * u, (x - 3.6f) * u, (y + 1.2f) * u)
            close()
        }
    drawPath(head, color)
}

/** A cassette for the user's own recordings. */
private fun DrawScope.cassette(
    u: Float,
    c: ArtColors,
) {
    drawRoundRect(c.shape, at(u, 9f, 14f), Size(30f * u, 20f * u), CornerRadius(3f * u))
    drawRoundRect(c.detail, at(u, 12f, 17f), Size(24f * u, 9f * u), CornerRadius(1.5f * u))
    drawLine(c.accent.copy(alpha = 0.5f), at(u, 15f, 19.5f), at(u, 27f, 19.5f), strokeWidth = 1f * u, cap = StrokeCap.Round)
    drawCircle(c.shape, 2.6f * u, at(u, 19f, 22.5f))
    drawCircle(c.shape, 2.6f * u, at(u, 29f, 22.5f))
    drawCircle(c.detail, 1f * u, at(u, 19f, 22.5f))
    drawCircle(c.detail, 1f * u, at(u, 29f, 22.5f))
    val foot =
        Path().apply {
            moveTo(15f * u, 34f * u)
            lineTo(17f * u, 29.5f * u)
            lineTo(31f * u, 29.5f * u)
            lineTo(33f * u, 34f * u)
            close()
        }
    drawPath(foot, c.detail.copy(alpha = 0.85f))
}
