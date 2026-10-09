package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    // Playback callbacks arrive in discrete samples. Interpolate the visual rail between samples
    // so every style travels continuously, while a finger drag always remains exact/direct.
    val animatedFraction by animateFloatAsState(
        targetValue = realFraction,
        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
        label = "nowPlayingPremiumProgress"
    )
    val visibleFraction = if (dragging) dragFraction else animatedFraction

    val visiblePosition = if (duration > 0L) {
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

                fun signalWaveY(
                    normalizedX: Float,
                    amplitude: Float,
                    offsetPhase: Float = 0f,
                    compression: Float = 1f
                ): Float {
                    // Android Wave is a stable waveform: playback moves only the progress beam
                    // through it. No phase is tied to time, so it never appears to oscillate.
                    val envelope = .44f + .56f * kotlin.math.abs(
                        sin((normalizedX * PI * 3.1f) + offsetPhase).toFloat()
                    )
                    val signal = sin(normalizedX * PI * 15.5f * compression + offsetPhase).toFloat()
                    return y + signal * amplitude * envelope
                }

                fun signalWave(amplitude: Float, offsetPhase: Float = 0f, compression: Float = 1f): Path {
                    val path = Path()
                    val step = max(3.dp.toPx(), size.width / 108f)
                    var x = 0f
                    while (x <= size.width + step) {
                        val normalizedX = if (size.width <= 0f) 0f else (x / size.width).coerceIn(0f, 1f)
                        val yy = signalWaveY(normalizedX, amplitude, offsetPhase, compression)
                        if (x == 0f) path.moveTo(x, yy) else path.lineTo(x, yy)
                        x += step
                    }
                    return path
                }

                fun drawAndroidWave(color: androidx.compose.ui.graphics.Color, alpha: Float, phaseOffset: Float = 0f) {
                    drawPath(
                        path = signalWave(3.8.dp.toPx(), phaseOffset),
                        color = color.copy(alpha = alpha),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                val introEndX = size.width * introFraction
                val tailStartX = size.width * (1f - tailFraction)
                fun crossfadeColorAt(x: Float): androidx.compose.ui.graphics.Color = when {
                    introFraction > 0f && x <= introEndX -> XvoxBlendInColor
                    tailFraction > 0f && x >= tailStartX -> XvoxBlendOutColor
                    else -> activeColor
                }

                /** Paint zones last so blend colours remain visible in every style, even over progress. */
                fun drawWaveCrossfadeZones(alpha: Float = .94f, phaseOffset: Float = 0f) {
                    if (introFraction > 0f) {
                        clipRect(right = introEndX) { drawAndroidWave(XvoxBlendInColor, alpha, phaseOffset) }
                    }
                    if (tailFraction > 0f) {
                        clipRect(left = tailStartX) { drawAndroidWave(XvoxBlendOutColor, alpha, phaseOffset) }
                    }
                }

                when (normalizedStyle) {
                    "android_wave" -> {
                        // A fixed Android-style waveform is the rail. The single beam is the only
                        // moving element, so the track stays visually calm while progress travels.
                        drawAndroidWave(activeColor, .24f)
                        drawWaveCrossfadeZones()
                        val beamY = signalWaveY(
                            normalizedX = if (size.width <= 0f) 0f else progressX / size.width,
                            amplitude = 3.8.dp.toPx()
                        )
                        val beamColor = crossfadeColorAt(progressX)
                        drawLine(
                            color = beamColor,
                            start = Offset(progressX, beamY - 5.5.dp.toPx()),
                            end = Offset(progressX, beamY + 5.5.dp.toPx()),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
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
                            val zoneColor = crossfadeColorAt(x)
                            val color = if (x <= progressX) zoneColor else zoneColor.copy(alpha = .25f)
                            drawLine(
                                color = color,
                                start = Offset(x, y - height),
                                end = Offset(x, y + height),
                                strokeWidth = 1.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(crossfadeColorAt(progressX), radius = 2.8.dp.toPx(), center = Offset(progressX, y))
                    }

                    "aurora" -> {
                        // The two layered rails retain an elegant moving energy in colour/active
                        // state while the crossfade zones stay explicit and consistent.
                        drawAndroidWave(colors.secondaryText, .18f, phaseOffset = .75f)
                        drawAndroidWave(activeColor, .30f)
                        clipRect(right = progressX) {
                            drawAndroidWave(activeColor, .92f, phaseOffset = .75f)
                            drawAndroidWave(activeColor, 1f)
                        }
                        drawWaveCrossfadeZones(phaseOffset = .75f)
                        drawWaveCrossfadeZones()
                        drawCircle(crossfadeColorAt(progressX), radius = 3.dp.toPx(), center = Offset(progressX, y))
                    }

                    "pill" -> {
                        // Capsule retains its tactile vertical handle while blend zones visibly
                        // colour the relevant portions of both inactive and active rail.
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
                        if (introFraction > 0f) {
                            drawRect(XvoxBlendInColor.copy(alpha = .92f), Offset(0f, top), androidx.compose.ui.geometry.Size(introEndX, railHeight))
                        }
                        if (tailFraction > 0f) {
                            drawRect(XvoxBlendOutColor.copy(alpha = .92f), Offset(tailStartX, top), androidx.compose.ui.geometry.Size(size.width - tailStartX, railHeight))
                        }
                        val handleHeight = 14.dp.toPx()
                        val handleWidth = 5.dp.toPx()
                        drawRoundRect(
                            color = crossfadeColorAt(progressX),
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
                        if (progressX > 0f) {
                            drawLine(
                                color = activeColor,
                                start = Offset(0f, y),
                                end = Offset(progressX, y),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                        // Zone colours render last, as on every alternate treatment. They remain
                        // readable through the active rail rather than disappearing after seek.
                        if (introFraction > 0f) {
                            drawLine(
                                color = XvoxBlendInColor.copy(alpha = .92f),
                                start = Offset(0f, y),
                                end = Offset(introEndX, y),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                        if (tailFraction > 0f) {
                            drawLine(
                                color = XvoxBlendOutColor.copy(alpha = .92f),
                                start = Offset(tailStartX, y),
                                end = Offset(size.width, y),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(crossfadeColorAt(progressX), radius = 2.3.dp.toPx(), center = Offset(progressX, y))
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
