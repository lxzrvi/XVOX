package com.xvox.music.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.player.playback.XvoxBlendMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private data class XvoxProgressBlend(
    val enabled: Boolean,
    val belongsToCurrentSong: Boolean,
    val introZoneMs: Long,
    val tailZoneMs: Long
)

@Composable
fun XvoxNowPlayingProgress(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    currentSongId: Long? = null,
    showTime: Boolean = true,
    /** classic, pill, or android_wave (the Android-style waveform rail). */
    style: String = "classic"
) {
    val colors = XvoxTheme.colors

    var dragging by remember {
        mutableStateOf(false)
    }

    var dragFraction by remember {
        mutableFloatStateOf(0f)
    }

    var dragAnchorFraction by remember {
        mutableFloatStateOf(0f)
    }
    var dragAnchorX by remember {
        mutableFloatStateOf(0f)
    }

    val realFraction =
        if (duration > 0L) {
            (position.toFloat() / duration.toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val latestRealFraction by rememberUpdatedState(realFraction)

    val visibleFraction =
        if (dragging) dragFraction else realFraction

    val visiblePosition =
        if (dragging && duration > 0L) {
            (duration * visibleFraction).toLong()
        } else {
            position
        }

    val activeColor = colors.primaryAccent

    val blendProjection = remember(currentSongId) {
        XvoxBlendMonitor.state.map { visual ->
            val belongsToCurrentSong = visual.currentId == currentSongId && currentSongId != null
            XvoxProgressBlend(
                enabled = visual.enabled,
                belongsToCurrentSong = belongsToCurrentSong,
                introZoneMs = if (visual.enabled && belongsToCurrentSong) visual.introZoneMs else 0L,
                tailZoneMs = if (visual.enabled && belongsToCurrentSong) visual.tailZoneMs else 0L
            )
        }.distinctUntilChanged()
    }
    val rawBlend by blendProjection.collectAsState(
        initial = XvoxProgressBlend(false, false, 0L, 0L)
    )
    // Rebuilding the queue for Shuffle briefly reports no next item.  Hold the last valid zones
    // for a tiny grace window so the progress rail does not flash off and back on.
    var stableBlendZones by remember(currentSongId) { mutableStateOf(0L to 0L) }
    LaunchedEffect(rawBlend, currentSongId) {
        val next = rawBlend.introZoneMs to rawBlend.tailZoneMs
        when {
            rawBlend.enabled && rawBlend.belongsToCurrentSong && next != (0L to 0L) -> {
                stableBlendZones = next
            }
            stableBlendZones != (0L to 0L) -> {
                // Queue rebuilds (especially with Shuffle) may emit a disabled/no-owner frame
                // before the same song receives its new valid blend window. Keep the last visual
                // zone briefly; a new valid emission cancels this effect before it can clear.
                delay(180)
                stableBlendZones = 0L to 0L
            }
            else -> stableBlendZones = 0L to 0L
        }
    }
    val introFraction = if (duration > 0L) (stableBlendZones.first.toFloat() / duration).coerceIn(0f, 0.5f) else 0f
    val tailFraction = if (duration > 0L) (stableBlendZones.second.toFloat() / duration).coerceIn(0f, 0.5f) else 0f

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showTime) 18.dp else 12.dp)
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (duration > 0L && size.width > 0) {
                                dragging = true
                                dragAnchorFraction = latestRealFraction
                                dragAnchorX = offset.x
                                dragFraction = latestRealFraction
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (dragging && size.width > 0) {
                                dragFraction =
                                    (dragAnchorFraction +
                                        (change.position.x - dragAnchorX) / size.width)
                                        .coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            if (duration > 0L && dragging) {
                                onSeek(
                                    (duration * dragFraction)
                                        .toLong()
                                )
                            }
                            dragging = false
                        },
                        onDragCancel = {
                            dragging = false
                        }
                    )
                }
                .pointerInput(duration) {
                    detectTapGestures { point ->
                        if (
                            duration > 0L &&
                            size.width > 0
                        ) {
                            val fraction =
                                (point.x / size.width)
                                    .coerceIn(0f, 1f)

                            onSeek(
                                (duration * fraction)
                                    .toLong()
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (showTime) 18.dp else 12.dp)
            ) {
                val y = size.height / 2f
                val normalizedStyle = when (style) {
                    "pill", "android_wave", "pulse", "aurora" -> style
                    else -> "classic"
                }
                val progressX = (size.width * visibleFraction).coerceIn(0f, size.width)
                val phase = visiblePosition / 210f

                fun signalWave(amplitude: Float, offsetPhase: Float = 0f, compression: Float = 1f): Path {
                    val path = Path()
                    val step = max(3.dp.toPx(), size.width / 96f)
                    var x = 0f
                    while (x <= size.width + step) {
                        val normalizedX = if (size.width <= 0f) 0f else x / size.width
                        // A gently changing envelope produces Android-style signal movement
                        // without a permanent animation coroutine or per-pixel work.
                        val envelope = .48f + .52f * sin((normalizedX * PI * 3.1f) + phase * .16f + offsetPhase).toFloat().let { kotlin.math.abs(it) }
                        val signal = sin(normalizedX * PI * 15.5f * compression + phase + offsetPhase).toFloat()
                        val yy = y + signal * amplitude * envelope
                        if (x == 0f) path.moveTo(x, yy) else path.lineTo(x, yy)
                        x += step
                    }
                    return path
                }

                fun drawAndroidWave(color: androidx.compose.ui.graphics.Color, alpha: Float, phaseOffset: Float = 0f) {
                    drawPath(
                        path = signalWave(3.4.dp.toPx(), phaseOffset),
                        color = color.copy(alpha = alpha),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.25.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                when (normalizedStyle) {
                    "android_wave" -> {
                        // A continuous, traveling Android-style signal wave rather than the old
                        // static bar pattern. The active portion is clipped cleanly to progress.
                        drawAndroidWave(activeColor, .27f)
                        clipRect(right = progressX) { drawAndroidWave(activeColor, 1f) }
                        drawCircle(activeColor, radius = 3.dp.toPx(), center = Offset(progressX, y))
                    }

                    "pulse" -> {
                        val bars = 42
                        val segment = (size.width / bars).coerceAtLeast(1f)
                        repeat(bars) { index ->
                            val x = (index + .5f) * segment
                            val energy = .42f + .58f * kotlin.math.abs(
                                sin(index * .86f + phase * 1.18f).toFloat()
                            )
                            val height = 1.8.dp.toPx() + energy * 4.4.dp.toPx()
                            val color = if (x <= progressX) activeColor else activeColor.copy(alpha = .25f)
                            drawLine(
                                color = color,
                                start = Offset(x, y - height),
                                end = Offset(x, y + height),
                                strokeWidth = 1.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(activeColor, radius = 2.8.dp.toPx(), center = Offset(progressX, y))
                    }

                    "aurora" -> {
                        // Two lightweight signal waves create a cool, readable alternative while
                        // preserving a normal single-accent progress meaning.
                        drawAndroidWave(colors.secondaryText, .18f, phaseOffset = .75f)
                        drawAndroidWave(activeColor, .30f)
                        clipRect(right = progressX) {
                            drawAndroidWave(activeColor, .92f, phaseOffset = .75f)
                            drawAndroidWave(activeColor, 1f)
                        }
                        drawCircle(activeColor, radius = 3.dp.toPx(), center = Offset(progressX, y))
                    }

                    "pill" -> {
                        // Redesigned Capsule: a calm filled rail with a compact vertical capsule
                        // handle instead of the former thin line plus circular knob.
                        val railHeight = 8.dp.toPx()
                        val top = y - railHeight / 2f
                        drawRoundRect(
                            color = activeColor.copy(alpha = .22f),
                            topLeft = Offset(0f, top),
                            size = androidx.compose.ui.geometry.Size(size.width, railHeight),
                            cornerRadius = CornerRadius(railHeight / 2f, railHeight / 2f)
                        )
                        if (progressX > 0f) {
                            drawRoundRect(
                                color = activeColor,
                                topLeft = Offset(0f, top),
                                size = androidx.compose.ui.geometry.Size(progressX, railHeight),
                                cornerRadius = CornerRadius(railHeight / 2f, railHeight / 2f)
                            )
                        }
                        val handleHeight = 14.dp.toPx()
                        val handleWidth = 5.dp.toPx()
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(progressX - handleWidth / 2f, y - handleHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(handleWidth, handleHeight),
                            cornerRadius = CornerRadius(handleWidth / 2f, handleWidth / 2f)
                        )
                    }

                    else -> {
                        val stroke = 2.5.dp.toPx()
                        drawLine(
                            color = activeColor.copy(alpha = .28f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        if (introFraction > 0f) {
                            drawLine(
                                color = XvoxBlendInColor.copy(alpha = .9f),
                                start = Offset(0f, y),
                                end = Offset(size.width * introFraction, y),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                        if (tailFraction > 0f) {
                            drawLine(
                                color = XvoxBlendOutColor.copy(alpha = .9f),
                                start = Offset(size.width * (1f - tailFraction), y),
                                end = Offset(size.width, y),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                        if (progressX > 0f) {
                            drawLine(
                                color = activeColor,
                                start = Offset(0f, y),
                                end = Offset(progressX, y),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        if (showTime) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPlayerTime(visiblePosition),
                    color = colors.secondaryText,
                    fontSize = 10.sp
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatPlayerTime(duration),
                    color = colors.secondaryText,
                    fontSize = 10.sp
                )
            }
        }
    }
}

fun formatPlayerTime(
    millis: Long
): String {
    val total =
        millis.coerceAtLeast(0L) / 1000L

    return buildString {
        append(total / 60L)
        append(':')
        append(
            (total % 60L)
                .toString()
                .padStart(2, '0')
        )
    }
}
