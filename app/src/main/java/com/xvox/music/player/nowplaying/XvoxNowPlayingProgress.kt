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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.player.playback.XvoxBlendMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    showTime: Boolean = true
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
                val stroke = 2.5.dp.toPx()

                // Base progress track
                drawLine(
                    color = activeColor.copy(alpha = 0.28f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                // Crossfade intro zone
                if (introFraction > 0f) {
                    drawLine(
                        color = XvoxBlendInColor.copy(alpha = 0.9f),
                        start = Offset(0f, y),
                        end = Offset(size.width * introFraction, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                // Crossfade tail zone
                if (tailFraction > 0f) {
                    drawLine(
                        color = XvoxBlendOutColor.copy(alpha = 0.9f),
                        start = Offset(size.width * (1f - tailFraction), y),
                        end = Offset(size.width, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                // Played fraction
                if (visibleFraction > 0f) {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, y),
                        end = Offset(size.width * visibleFraction, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
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
