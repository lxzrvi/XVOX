package com.xvox.music.core.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PressIn = spring<Float>(dampingRatio = 0.90f, stiffness = 1200f)
private val PressOut = spring<Float>(dampingRatio = 0.75f, stiffness = 900f)

/**
 * Press feedback for song cards:
 * - Clean tap with immediate audio playback.
 * - Smooth push/scale effect on long press across library lists.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.xvoxSongPress(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    /** How far the tile itself dips on long press. */
    pressedScale: Float = 0.96f,
    onPressedChange: ((Boolean) -> Unit)? = null,
    hapticOnTap: Boolean = true
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalXvoxHaptics.current
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val release = remember { mutableStateOf<Job?>(null) }
    val pressChange by rememberUpdatedState(onPressedChange)

    // A caller can request press-state feedback without scaling its content. Search uses this to
    // shrink only a card's drawn surface while its cover and labels retain their exact size.
    val modifier = if (pressedScale != 1f || onPressedChange != null) {
        (if (pressedScale != 1f) {
            Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        } else {
            Modifier
        }).pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val pressJob = scope.launch {
                        delay(120) // Only animate scale on deliberate press/hold
                        release.value?.cancel()
                        pressChange?.invoke(true)
                        scale.animateTo(pressedScale, PressIn)
                    }
                    var becameScroll = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!becameScroll) {
                                val dx = change.position.x - down.position.x
                                val dy = change.position.y - down.position.y
                                val moved = dx * dx + dy * dy > viewConfiguration.touchSlop * viewConfiguration.touchSlop
                                if (moved) {
                                    becameScroll = true
                                    pressJob.cancel()
                                    pressChange?.invoke(false)
                                    release.value?.cancel()
                                    release.value = scope.launch { scale.animateTo(1f, PressOut) }
                                }
                            }
                            if (!change.pressed) break
                            if (event.changes.none { it.pressed }) break
                        }
                    } finally {
                        if (!becameScroll) {
                            pressJob.cancel()
                            pressChange?.invoke(false)
                            release.value = scope.launch { delay(30); scale.animateTo(1f, PressOut) }
                        }
                    }
                }
            }
    } else Modifier

    val capture = remember { XvoxHoldCapture() }
    // The lifted copy is drawn by the menu; the original steps aside so there is never a twin.
    val stepAside = Modifier.graphicsLayer {
        val lifted = XvoxLongPressOrigin.lifted
        val at = capture.coordinates?.takeIf { it.isAttached }?.positionInWindow()
        alpha = if (lifted != null && at != null &&
            kotlin.math.abs(lifted.left - at.x) < 3f && kotlin.math.abs(lifted.top - at.y) < 3f) 0f else 1f
    }
    val graphicsContext = LocalGraphicsContext.current
    val density = LocalDensity.current

    val clickable = stepAside.then(modifier).combinedClickable(
        hapticFeedbackEnabled = false,
        interactionSource = interaction,
        indication = null,
        onClick = {
            if (hapticOnTap) haptics.tap()
            click()
        },
        onLongClick = if (onLongClick != null) ({
            haptics.heavy()
            // Record where the card is and ask the next frame to photograph it, then open the menu.
            capture.coordinates?.takeIf { it.isAttached }?.let { coordinates ->
                val item = XvoxHeldItem(Rect(coordinates.positionInWindow(), coordinates.size.toSize()))
                XvoxLongPressOrigin.record(item)
                XvoxLongPressOrigin.capturing = true
                capture.pending = item
                capture.requests++
            }
            longClick?.invoke()
        }) else null
    )
    if (onLongClick == null) clickable else clickable
        .onPlaced { capture.coordinates = it }
        .drawWithContent {
            drawContent()
            // Reading the counter subscribes this draw to a hold.
            val request = capture.requests
            if (request == capture.handled) return@drawWithContent
            capture.handled = request
            val item = capture.pending ?: return@drawWithContent
            capture.pending = null
            val layer = graphicsContext.createGraphicsLayer()
            layer.record(size = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))) {
                this@drawWithContent.drawContent()
            }
            scope.launch {
                try {
                    item.image = layer.toImageBitmap()
                } finally {
                    graphicsContext.releaseGraphicsLayer(layer)
                    XvoxLongPressOrigin.capturing = false
                }
            }
        }
}

private class XvoxHoldCapture {
    var coordinates: LayoutCoordinates? = null
    var pending: XvoxHeldItem? = null
    var requests by mutableIntStateOf(0)
    var handled = 0
}

/** Hides a card's own ⋮ while the card's picture is being taken for the lifted menu. */
fun Modifier.hiddenWhileHeld(): Modifier = graphicsLayer { alpha = if (XvoxLongPressOrigin.capturing) 0f else 1f }
