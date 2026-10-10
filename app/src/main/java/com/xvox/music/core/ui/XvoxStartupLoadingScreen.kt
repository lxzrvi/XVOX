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
// The last revealed row dot owns segment zero. It remains the accent through gather/stretch, and
// the spinning arc starts on that very same completed segment with no carrier or extra dot.
private const val StartupTerminalDotIndex = 4
private const val StartupTerminalRingSegment = 0
private val StartupRingOrder = intArrayOf(2, 3, 4, 1, StartupTerminalRingSegment)
private const val StartupDotRevealCadence = 210f
private const val StartupDotRevealDuration = 170f
private const val StartupRingSegmentStagger = .105f
private val StartupRingBuildOrder = intArrayOf(0, 1, 2, 3, 4)

// Row, gather, stretch, spin, unroll, then fill. The opening is deliberately compact: five dots
// reveal and pass one accent pulse left-to-right before their terminal accent becomes the ring.
private val StartupPhaseDurations = floatArrayOf(
    1720f, 560f, 840f, 2200f, 1200f, 2400f
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

/** A dot is born once, in left-to-right order; it never has a travelling carrier twin. */
private fun startupDotReveal(index: Int, time: Float): Float = startupEase(
    startupClamp01((time - index * StartupDotRevealCadence) / StartupDotRevealDuration)
)

/**
 * Accent moves with each newly revealed dot. Earlier dots settle to the neutral rail; the fifth
 * dot keeps the accent, so it can become segment zero without a colour or position discontinuity.
 */
private fun startupRowDotColor(index: Int, time: Float, palette: StartupPalette): Color {
    val activeIndex = (time / StartupDotRevealCadence).toInt().coerceIn(0, StartupTerminalDotIndex)
    val accent = when {
        index == StartupTerminalDotIndex && time >=
            StartupTerminalDotIndex * StartupDotRevealCadence + StartupDotRevealDuration -> 1f
        // The active dot is already the accent as it fades in. Startup therefore begins in the
        // fresh-install red/accent instead of briefly flashing the neutral rail first.
        index == activeIndex -> 1f
        else -> 0f
    }
    return startupMixColor(palette.dot, palette.accent, accent)
}

private fun startupRingColorForDot(index: Int, palette: StartupPalette): Color =
    if (index == StartupTerminalDotIndex) palette.accent else palette.dot

/** Five dots appear one-by-one with a single travelling accent; there is never a carrier sixth dot. */
private fun DrawScope.drawStartupRow(time: Float, palette: StartupPalette) {
    repeat(5) { index ->
        val reveal = startupDotReveal(index, time)
        if (reveal > .001f) {
            startupDot(
                x = startupRowX(index),
                y = 0f,
                color = startupRowDotColor(index, time, palette).copy(alpha = reveal),
                radius = StartupDotDiameter / 2f * reveal.coerceAtLeast(.42f)
            )
        }
    }
}

/** Each fully revealed dot moves to its own ring-segment centre; no sixth/pre-ring dot exists. */
private fun DrawScope.drawStartupGather(time: Float, palette: StartupPalette) {
    val progress = startupEase(startupClamp01(time / StartupPhaseDurations[1]))
    repeat(5) { index ->
        val target = startupSegmentCenter(StartupRingOrder[index])
        startupDot(
            x = startupMix(startupRowX(index), target.x, progress),
            y = startupMix(0f, target.y, progress),
            color = startupRingColorForDot(index, palette),
            radius = StartupDotDiameter / 2f
        )
    }
}

/**
 * Ring segments unfurl in a clockwise sequence from the terminal accent seed. This preserves the
 * exact terminal dot at segment zero while giving the ring a deliberate premium build rather than
 * five simultaneous strokes.
 */
private fun DrawScope.drawStartupStretch(progress: Float, palette: StartupPalette) {
    repeat(5) { index ->
        val segment = StartupRingOrder[index]
        val sequence = StartupRingBuildOrder.indexOf(segment).coerceAtLeast(0)
        val local = startupEase(startupClamp01(
            (progress - sequence * StartupRingSegmentStagger) /
                (1f - (StartupRingBuildOrder.lastIndex * StartupRingSegmentStagger))
        ))
        val color = startupRingColorForDot(index, palette)
        val width = startupMix(StartupDotDiameter, StartupStrokeWidth, local)
        if (local < .004f) {
            val center = startupSegmentCenter(segment)
            startupDot(center.x, center.y, color, width / 2f)
        } else {
            startupLine(
                points = startupPoints(18) { step ->
                    startupRingPoint((segment + .5f + (step - .5f) * local) * .2f)
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
        // At rotation zero this is the terminal dot's completed segment (zero), exactly
        // matching drawStartupStretch's final [Start, Start + 72°] accent span.
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
