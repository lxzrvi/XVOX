package com.xvox.music.core.ui

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// All geometry is expressed in density-independent canvas units and converted at draw time.
private const val StartupStrokeWidth = 3f
// The loader now begins directly with a complete ring; there is no opening dot rail.
private const val StartupRingRadius = 20f
private const val StartupBarLength = 210f
private val StartupPi = PI.toFloat()
private val StartupRingStart = -StartupPi / 5f
private const val StartupTerminalRingSegment = 0

// Ring, unroll, then fill. Starting on the completed ring avoids any pre-loader dot state.
private val StartupPhaseDurations = floatArrayOf(2200f, 1200f, 2400f)
private val StartupPhaseBounds = FloatArray(StartupPhaseDurations.size + 1).also { bounds ->
    for (index in StartupPhaseDurations.indices) {
        bounds[index + 1] = bounds[index] + StartupPhaseDurations[index]
    }
}
private val StartupTotalDuration = StartupPhaseBounds.last()
private const val StartupReducedMotionDuration = 2600f

private data class StartupPalette(
    val dot: Color,
    val accent: Color,
    val background: Color
)

private fun startupClamp01(value: Float) = value.coerceIn(0f, 1f)

private fun startupEase(value: Float): Float = if (value < .5f) {
    4f * value * value * value
} else {
    1f - (-2f * value + 2f).pow(3) / 2f
}

private fun startupMix(first: Float, second: Float, amount: Float) = first + (second - first) * amount

private fun startupRingPoint(segment: Float): Offset {
    val angle = StartupRingStart + 2f * StartupPi * segment
    return Offset(StartupRingRadius * cos(angle), StartupRingRadius * sin(angle))
}

private fun startupPoints(count: Int, point: (Float) -> Offset): List<Offset> =
    List(count + 1) { index -> point(index / count.toFloat()) }

private fun DrawScope.startupLine(
    points: List<Offset>,
    color: Color,
    width: Float = StartupStrokeWidth
) {
    val scale = density
    val path = Path()
    points.forEachIndexed { index, point ->
        if (index == 0) path.moveTo(point.x * scale, point.y * scale)
        else path.lineTo(point.x * scale, point.y * scale)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.startupDot(
    x: Float,
    y: Float,
    color: Color,
    radius: Float = StartupStrokeWidth / 2f
) {
    val scale = density
    drawCircle(color = color, radius = radius * scale, center = Offset(x * scale, y * scale))
}

/** The accent arc makes two full clockwise circuits around the ring. */
private fun DrawScope.drawStartupSpin(progress: Float, palette: StartupPalette) {
    val scale = density
    val rotation = 4f * StartupPi * startupEase(progress)
    val stroke = Stroke(StartupStrokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawCircle(
        color = palette.dot,
        radius = StartupRingRadius * scale,
        center = Offset.Zero,
        style = stroke
    )
    drawArc(
        color = palette.accent,
        // At rotation zero the accent begins on the ring's first 72-degree segment, keeping the
        // first visible frame complete and stable before its clockwise travel.
        startAngle = Math.toDegrees(
            (StartupRingStart + StartupTerminalRingSegment * (2f * StartupPi / 5f) + rotation).toDouble()
        ).toFloat(),
        sweepAngle = 72f,
        useCenter = false,
        topLeft = Offset(-StartupRingRadius * scale, -StartupRingRadius * scale),
        size = Size(2f * StartupRingRadius * scale, 2f * StartupRingRadius * scale),
        style = stroke
    )
}

/** The ring opens sequentially into the thin loading rail. */
private fun DrawScope.drawStartupUnroll(progress: Float, palette: StartupPalette) {
    fun point(segment: Float): Offset {
        val unroll = startupEase(startupClamp01((progress - .45f * segment) / .55f))
        val ringPoint = startupRingPoint(segment)
        return Offset(
            startupMix(ringPoint.x, -StartupBarLength / 2f + StartupBarLength * segment, unroll),
            startupMix(ringPoint.y, 0f, unroll)
        )
    }

    startupLine(startupPoints(100) { point(it) }, palette.dot)
    val accentLength = .2f * (1f - startupEase(progress))
    if (accentLength < .004f) {
        val start = point(0f)
        startupDot(start.x, start.y, palette.accent)
    } else {
        startupLine(startupPoints(24) { step -> point(step * accentLength) }, palette.accent)
    }
}

/** The rail fills with the active XVOX accent. */
private fun DrawScope.drawStartupLoadBar(progress: Float, complete: Boolean, palette: StartupPalette) {
    startupLine(
        points = listOf(Offset(-StartupBarLength / 2f, 0f), Offset(StartupBarLength / 2f, 0f)),
        color = palette.dot
    )
    val fill = if (complete) 1f else startupEase(startupClamp01(progress))
    val endX = -StartupBarLength / 2f + fill * StartupBarLength
    if (endX + StartupBarLength / 2f < .5f) {
        startupDot(-StartupBarLength / 2f, 0f, palette.accent)
    } else {
        startupLine(
            points = listOf(Offset(-StartupBarLength / 2f, 0f), Offset(endX, 0f)),
            color = palette.accent
        )
    }
}

/**
 * Theme-aware XVOX startup animation.
 *
 * The sequence begins on a complete ring, then unrolls once into a rail. Its
 * gray/background/accent colors always come from the active XVOX theme, and the completed accent
 * rail releases Home as soon as real startup work is ready—there is no dot or replay phase.
 */
@Composable
fun XvoxStartupLoadingScreen(
    readyToEnter: Boolean,
    onSequenceComplete: () -> Unit,
    progress: Float = 0f
) {
    val colors = XvoxTheme.colors
    val palette = StartupPalette(
        dot = colors.secondaryText.copy(alpha = .78f),
        accent = colors.primaryAccent,
        background = colors.background
    )
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
    val latestReadyToEnter by rememberUpdatedState(readyToEnter)
    val latestSequenceComplete by rememberUpdatedState(onSequenceComplete)
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(reducedMotion) {
        val startNanos = withFrameNanos { it }
        var finished = false

        while (!finished) {
            withFrameNanos { nowNanos ->
                val elapsedMs = (nowNanos - startNanos) / 1_000_000f
                if (reducedMotion) {
                    // Reduced motion retains a quiet linear rail and never runs ring/spin motion.
                    time = elapsedMs.coerceAtMost(StartupReducedMotionDuration)
                    if (latestReadyToEnter && elapsedMs >= StartupReducedMotionDuration) finished = true
                } else {
                    // A completed rail stays full only while real bootstrap work is still pending.
                    // It never rewinds to dots, so startup has one clean forward handoff.
                    time = elapsedMs.coerceAtMost(StartupTotalDuration)
                    if (latestReadyToEnter && elapsedMs >= StartupTotalDuration) finished = true
                }
            }
        }
        latestSequenceComplete()
    }

    val progressPercent = (progress.coerceIn(0f, 1f) * 100f).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(300.dp, 120.dp)
                .semantics { contentDescription = "Loading $progressPercent percent" }
        ) {
            translate(left = size.width / 2f, top = size.height / 2f) {
                if (reducedMotion) {
                    // Reduced motion keeps the first frame as a calm finished ring, then makes
                    // only the essential ring-to-rail transition and fill visible.
                    when {
                        time < 500f -> drawStartupSpin(0f, palette)
                        time < 1100f -> drawStartupUnroll((time - 500f) / 600f, palette)
                        else -> drawStartupLoadBar(
                            progress = (time - 1100f) / (StartupReducedMotionDuration - 1100f),
                            complete = time >= StartupReducedMotionDuration,
                            palette = palette
                        )
                    }
                } else {
                    when {
                        time < StartupPhaseBounds[1] -> drawStartupSpin(
                            time / StartupPhaseDurations[0],
                            palette
                        )
                        time < StartupPhaseBounds[2] -> drawStartupUnroll(
                            (time - StartupPhaseBounds[1]) / StartupPhaseDurations[1],
                            palette
                        )
                        time < StartupPhaseBounds[3] -> drawStartupLoadBar(
                            (time - StartupPhaseBounds[2]) / StartupPhaseDurations[2],
                            complete = false,
                            palette = palette
                        )
                        else -> drawStartupLoadBar(1f, complete = true, palette = palette)
                    }
                }
            }
        }
    }
}
