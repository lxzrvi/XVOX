package com.xvox.music.core.ui

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// All geometry is expressed in density-independent canvas units and converted at draw time.
private const val StartupStrokeWidth = 3.2f
// The loader begins directly with a complete ring; there is no opening/progress dot rail.
private const val StartupRingRadius = 21f
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
// Home/media work begins behind this complete ring-to-bar sequence. The visible clock is never
// cut short by a fast Home bootstrap; if Home is exceptionally slow, the completed bar simply
// holds its finished state until the already-running bootstrap is ready to reveal.

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

/** The accent arc makes two full clockwise circuits around the ring. */
private fun DrawScope.drawStartupSpin(progress: Float, palette: StartupPalette) {
    val scale = density
    val rotation = 4f * StartupPi * startupEase(progress)
    val stroke = Stroke(StartupStrokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Keep the original ring that unrolls into the rail, but remove the extra outer halo that
    // was added around it. There is exactly one loader outline on screen.
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
    // Keep a short accent lead alive all the way into the rail. Without this floor, the final
    // unroll frame and the first fill frame read as a completely empty gray bar for one beat.
    val terminalAccentLength = .055f
    val accentLength = terminalAccentLength + (.2f - terminalAccentLength) * (1f - startupEase(progress))
    startupLine(startupPoints(24) { step -> point(step * accentLength) }, palette.accent)
}

/** The rail fills with the active XVOX accent. */
private fun DrawScope.drawStartupLoadBar(progress: Float, complete: Boolean, palette: StartupPalette) {
    startupLine(
        points = listOf(Offset(-StartupBarLength / 2f, 0f), Offset(StartupBarLength / 2f, 0f)),
        color = palette.dot
    )
    // Match the unroll's retained accent lead, then continue the same full fill clock. The bar
    // therefore never presents a fully empty track during the ring → bar handoff.
    val retainedLead = .055f
    val fill = if (complete) 1f else retainedLead + (1f - retainedLead) * startupEase(startupClamp01(progress))
    val endX = -StartupBarLength / 2f + fill * StartupBarLength
    startupLine(
        points = listOf(Offset(-StartupBarLength / 2f, 0f), Offset(endX, 0f)),
        color = palette.accent
    )
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
    /** Starts the unchanged ring → rail → fill timeline. */
    readyToEnter: Boolean,
    /** Allows Home only after both the visible bar and background Home bootstrap are complete. */
    canEnterHome: Boolean = readyToEnter,
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
    val latestCanEnterHome by rememberUpdatedState(canEnterHome)
    val latestSequenceComplete by rememberUpdatedState(onSequenceComplete)
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(reducedMotion) {
        // Do not let preference/setup work consume the animation clock. Home and playback begin
        // together with this timeline, so even a slow first preference read still shows the full
        // ring → rail → fill treatment rather than flashing straight to Home.
        var sequenceStartNanos: Long? = null
        var finished = false

        while (!finished) {
            withFrameNanos { nowNanos ->
                if (!latestReadyToEnter) {
                    sequenceStartNanos = null
                    time = 0f
                } else {
                    val startNanos = sequenceStartNanos ?: nowNanos.also { sequenceStartNanos = it }
                    val elapsedMs = (nowNanos - startNanos) / 1_000_000f
                    val sequenceDuration = if (reducedMotion) {
                        StartupReducedMotionDuration
                    } else {
                        StartupTotalDuration
                    }
                    time = elapsedMs.coerceAtMost(sequenceDuration)
                    // Background Home work runs concurrently with the whole animation. Never
                    // stretch a phase for it: after the rail is visibly complete, hold that stable
                    // terminal frame until Home is ready rather than revealing a still-loading UI.
                    if (elapsedMs >= sequenceDuration && latestCanEnterHome) finished = true
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
        // The loader stays intentionally quiet: the platform splash owns the mark, while this
        // screen keeps only a small, bottom-centred Cinzel wordmark.  Nothing competes with the
        // ring-to-rail progress sequence in the middle of the canvas.
        Text(
            text = "XVOX",
            color = colors.secondaryText.copy(alpha = .74f),
            fontFamily = XvoxLogoFont,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 2.2.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
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
