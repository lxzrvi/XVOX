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
// Keep the opening dots delicately smaller than the finished ring/rail. Accent changes colour
// one fixed dot at a time; no carrier dot travels between positions.
private const val StartupDotDiameter = 5f
// Slightly tighter than the prior ring, matching the compact loader reference without making the
// subsequent rail feel disconnected.
private const val StartupRingRadius = 20f
private const val StartupBarLength = 210f
private val StartupPi = PI.toFloat()
private val StartupRingStart = -StartupPi / 5f
private val StartupRingOrder = intArrayOf(2, 3, 4, 1, 0)

// Row, gather, stretch, spin, unroll, then fill. A completed fill exits into the app; it never
// loops back to the initial dots or makes the finished ring replay.
private val StartupPhaseDurations = floatArrayOf(
    2300f, 560f, 680f, 2200f, 1200f, 2400f
)
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

private fun startupMixColor(first: Color, second: Color, amount: Float) = Color(
    red = startupMix(first.red, second.red, amount),
    green = startupMix(first.green, second.green, amount),
    blue = startupMix(first.blue, second.blue, amount),
    alpha = startupMix(first.alpha, second.alpha, amount)
)

private fun startupRingPoint(segment: Float): Offset {
    val angle = StartupRingStart + 2f * StartupPi * segment
    return Offset(StartupRingRadius * cos(angle), StartupRingRadius * sin(angle))
}

private fun startupRowX(index: Int) = (index - 2) * 16f

private fun startupPoints(count: Int, point: (Float) -> Offset): List<Offset> =
    List(count + 1) { index -> point(index / count.toFloat()) }

private fun startupSegmentCenter(index: Int) = startupRingPoint((index + .5f) * .2f)

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

/**
 * Accent transfers strictly between fixed dots. It has no rest interval, no travelling carrier,
 * and is shared by the gather/stretch phases so the handoff never flashes back to gray.
 */
private fun startupAccentAmount(index: Int, absoluteTime: Float): Float {
    val cadence = 118f
    val progress = (absoluteTime / cadence).coerceAtLeast(0f)
    val current = progress.toInt().floorMod(5)
    val next = (current + 1) % 5
    val handoff = (progress - progress.toInt()).coerceIn(0f, 1f)
    return when (index) {
        current -> 1f - handoff
        next -> handoff
        else -> 0f
    }
}

/** Five fixed dots accent one by one in one continuous, no-pause handoff. */
private fun DrawScope.drawStartupRow(time: Float, palette: StartupPalette) {
    repeat(5) { index ->
        startupDot(
            x = startupRowX(index),
            y = 0f,
            color = startupMixColor(palette.dot, palette.accent, startupAccentAmount(index, time)),
            radius = StartupDotDiameter / 2f
        )
    }
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus

/** Dots gather into their own ring positions together, retaining the in-flight accent handoff. */
private fun DrawScope.drawStartupGather(time: Float, palette: StartupPalette) {
    val progress = startupEase(startupClamp01(time / StartupPhaseDurations[1]))
    val absoluteTime = StartupPhaseDurations[0] + time
    repeat(5) { index ->
        val target = startupSegmentCenter(StartupRingOrder[index])
        startupDot(
            x = startupMix(startupRowX(index), target.x, progress),
            y = startupMix(0f, target.y, progress),
            color = startupMixColor(palette.dot, palette.accent, startupAccentAmount(index, absoluteTime)),
            radius = StartupDotDiameter / 2f
        )
    }
}

/** Each gathering dot fluidly stretches into its own ring segment without a visual pause. */
private fun DrawScope.drawStartupStretch(progress: Float, palette: StartupPalette) {
    val easedProgress = startupEase(progress)
    val absoluteTime = StartupPhaseDurations[0] + StartupPhaseDurations[1] +
        progress.coerceIn(0f, 1f) * StartupPhaseDurations[2]
    repeat(5) { index ->
        val segment = StartupRingOrder[index]
        val color = startupMixColor(palette.dot, palette.accent, startupAccentAmount(index, absoluteTime))
        val width = startupMix(StartupDotDiameter, StartupStrokeWidth, easedProgress)
        if (easedProgress < .004f) {
            val center = startupSegmentCenter(segment)
            startupDot(center.x, center.y, color, width / 2f)
        } else {
            startupLine(
                points = startupPoints(16) { step ->
                    startupRingPoint((segment + .5f + (step - .5f) * easedProgress) * .2f)
                },
                color = color,
                width = width
            )
        }
    }
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
        startAngle = Math.toDegrees((StartupRingStart + rotation).toDouble()).toFloat(),
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
 * The sequence moves once from dots to ring to rail. Its gray/background/accent colors always
 * come from the active XVOX theme, and the completed accent rail releases Home as soon as the
 * real startup work is ready—there is no return-to-dot or replay phase.
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
                    drawStartupLoadBar(
                        progress = time / 2600f,
                        complete = time >= 2600f,
                        palette = palette
                    )
                } else {
                    when {
                        time < StartupPhaseBounds[1] -> drawStartupRow(time, palette)
                        time < StartupPhaseBounds[2] -> drawStartupGather(time - StartupPhaseBounds[1], palette)
                        time < StartupPhaseBounds[3] -> drawStartupStretch(
                            (time - StartupPhaseBounds[2]) / StartupPhaseDurations[2],
                            palette
                        )
                        time < StartupPhaseBounds[4] -> drawStartupSpin(
                            (time - StartupPhaseBounds[3]) / StartupPhaseDurations[3],
                            palette
                        )
                        time < StartupPhaseBounds[5] -> drawStartupUnroll(
                            (time - StartupPhaseBounds[4]) / StartupPhaseDurations[4],
                            palette
                        )
                        time < StartupPhaseBounds[6] -> drawStartupLoadBar(
                            (time - StartupPhaseBounds[5]) / StartupPhaseDurations[5],
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
