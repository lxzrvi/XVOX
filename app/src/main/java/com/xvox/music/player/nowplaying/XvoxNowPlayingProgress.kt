package com.xvox.music.player.nowplaying

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.player.playback.XvoxBlendMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.PI
import kotlin.math.abs
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
    /** Paused playback must freeze at the exact controller sample, never animate toward a reset. */
    isPlaying: Boolean = true,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    currentSongId: Long? = null,
    showTime: Boolean = true,
    /** classic, pill, or android_wave (the Android-style waveform rail). */
    style: String = "classic"
) {
    val colors = XvoxTheme.colors

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    // A tap/drag seek paints its chosen position immediately and holds it until the controller
    // confirms the seek. It is deliberately not released on a timer: a paused or buffered player
    // may publish slowly, and releasing early was the source of the old reset/repeat bar.
    var pendingSeekFraction by remember(currentSongId) { mutableStateOf<Float?>(null) }
    // A controller confirmation must not restart a second interpolation from a stale sample after
    // the listener has already released at the target. The next confirmed sample is snapped once,
    // then ordinary playback sampling resumes.
    var snapNextControllerSample by remember(currentSongId) { mutableStateOf(false) }
    // Freeze the actual rendered rail on pause. Controller samples can briefly rewind/reconcile
    // while paused; those samples must not visibly move a position the listener has stopped at.
    var pausedFraction by remember(currentSongId) { mutableStateOf<Float?>(null) }
    var lastValidDuration by remember(currentSongId) {
        mutableLongStateOf(duration.takeIf { it > 0L } ?: 0L)
    }
    var lastValidFraction by remember(currentSongId) {
        mutableFloatStateOf(
            if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
        )
    }
    // Tap and drag recognizers share the same rail. A recognizer can report the release of one
    // physical drag as a tap on some devices, so gate that duplicate event before it reaches the
    // player—not merely after the player has already been asked to seek twice.
    var lastSeekGestureUptimeMs by remember(currentSongId) { mutableLongStateOf(0L) }
    var lastSeekGestureFraction by remember(currentSongId) { mutableFloatStateOf(Float.NaN) }

    // Media3 can briefly report an unavailable duration while the same item is paused, buffering,
    // or reconciling a seek. Retain the last valid duration/fraction for that occurrence rather
    // than interpreting the transient as a real position of zero.
    LaunchedEffect(currentSongId, position, duration, pendingSeekFraction) {
        if (duration <= 0L) return@LaunchedEffect
        val controllerFraction = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        lastValidDuration = duration
        val pending = pendingSeekFraction
        if (pending == null) {
            lastValidFraction = controllerFraction
        } else {
            val targetPosition = (duration * pending).toLong()
            val tolerance = max(750L, (duration * .006f).toLong())
            if (abs(position - targetPosition) <= tolerance) {
                lastValidFraction = controllerFraction
                // The rail has already been placed at the user's target. Do not let confirmation
                // animate it back from the old sample and replay that same seek distance.
                snapNextControllerSample = true
                pendingSeekFraction = null
            }
        }
    }

    val stableDuration = duration.takeIf { it > 0L } ?: lastValidDuration
    val realFraction = if (duration > 0L) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        lastValidFraction
    }

    // Playback callbacks arrive in discrete samples. Ordinary playback samples can still receive
    // a short smoothing pass, but user seeks are direct: release paints the exact chosen point and
    // the Media3 acknowledgement snaps in place rather than replaying an old → target animation.
    val renderedFraction = remember(currentSongId) { Animatable(realFraction) }
    val controllerSampleKey = if (pendingSeekFraction == null) realFraction else 0f
    LaunchedEffect(currentSongId, isPlaying, pendingSeekFraction, controllerSampleKey, snapNextControllerSample) {
        val pending = pendingSeekFraction
        when {
            pending != null -> renderedFraction.snapTo(pending)
            snapNextControllerSample -> {
                renderedFraction.snapTo(realFraction)
                snapNextControllerSample = false
            }
            !isPlaying -> renderedFraction.snapTo(realFraction)
            else -> renderedFraction.animateTo(
                realFraction,
                tween(durationMillis = 220, easing = LinearOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(currentSongId, isPlaying) {
        if (isPlaying) {
            pausedFraction = null
        } else {
            // Capture the value the listener actually saw as playback stopped, not a later
            // controller reconciliation sample. A pending paused seek already owns that value.
            pausedFraction = pendingSeekFraction ?: renderedFraction.value
        }
    }

    fun requestSeek(fraction: Float) {
        val now = SystemClock.uptimeMillis()
        val target = fraction.coerceIn(0f, 1f)
        // Tap and drag gesture recognizers can both observe the release of the same finger. Do
        // not make the player replay that same seek; distinct rapid seeks remain allowed.
        if (
            now - lastSeekGestureUptimeMs < 650L &&
            lastSeekGestureFraction.isFinite() &&
            abs(target - lastSeekGestureFraction) < .008f
        ) return
        lastSeekGestureUptimeMs = now
        lastSeekGestureFraction = target
        if (stableDuration <= 0L) return
        pendingSeekFraction = target
        // Seeking while paused must replace the frozen rail immediately and remain there after
        // Media3 confirms. It is intentionally independent of callback timing.
        if (!isPlaying) pausedFraction = target
        onSeek((stableDuration * target).toLong())
    }

    val visibleFraction = when {
        dragging -> dragFraction
        // A user target owns the visible rail immediately in both play and pause states. Keeping
        // it direct until controller confirmation prevents any old-position replay or double move.
        pendingSeekFraction != null -> pendingSeekFraction!!
        !isPlaying -> pausedFraction ?: renderedFraction.value
        else -> renderedFraction.value
    }

    val visiblePosition = if (stableDuration > 0L) {
        (stableDuration * visibleFraction).toLong()
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
    val introFraction = if (stableDuration > 0L) (stableBlendZones.first.toFloat() / stableDuration).coerceIn(0f, 0.5f) else 0f
    val tailFraction = if (stableDuration > 0L) (stableBlendZones.second.toFloat() / stableDuration).coerceIn(0f, 0.5f) else 0f

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showTime) 18.dp else 12.dp)
                .pointerInput(stableDuration) {
                    // A single recognizer owns this rail.  The previous independent tap and drag
                    // detectors could both report one release, producing a second seek sample and
                    // the visible accent-line jump that followed it.
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        if (stableDuration <= 0L || size.width <= 0) {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                            } while (event.changes.any { it.pressed })
                            return@awaitEachGesture
                        }

                        val start = down.position
                        val slop = viewConfiguration.touchSlop
                        var mode = 0 // 0 = tap candidate, 1 = horizontal seek, 2 = another gesture
                        var released = false
                        while (!released) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val dx = change.position.x - start.x
                            val dy = change.position.y - start.y
                            if (mode == 0 && dx * dx + dy * dy > slop * slop) {
                                mode = if (abs(dx) >= abs(dy)) 1 else 2
                                if (mode == 1) {
                                    pendingSeekFraction = null
                                    dragging = true
                                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                }
                            }
                            if (mode == 1) {
                                change.consume()
                                dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                            if (!change.pressed || event.changes.none { it.pressed }) {
                                when (mode) {
                                    0 -> requestSeek((start.x / size.width).coerceIn(0f, 1f))
                                    1 -> requestSeek(dragFraction)
                                }
                                released = true
                            }
                        }
                        dragging = false
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
                    // Android Wave is a stable waveform: playback changes only the filled rail
                    // portion. No phase is tied to time, so it never appears to oscillate.
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
                        // A fixed Android-style waveform is the rail. Progress is expressed by
                        // the filled portion only—no dot, knob, cursor, or other thumb marker.
                        drawAndroidWave(activeColor, .24f)
                        clipRect(right = progressX) {
                            drawAndroidWave(activeColor, .94f)
                        }
                        drawWaveCrossfadeZones()
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
                    }

                    "pill" -> {
                        // The capsule is deliberately thumb-free; blend zones remain visible on
                        // both inactive and active portions without adding a progress dot.
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
                    text = formatPlayerTime(stableDuration),
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
