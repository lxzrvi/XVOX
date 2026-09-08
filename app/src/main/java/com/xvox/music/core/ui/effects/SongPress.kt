package com.xvox.music.core.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PressIn = spring<Float>(dampingRatio = 0.9f, stiffness = 2600f)
private val PressOut = spring<Float>(dampingRatio = 0.72f, stiffness = 900f)

/**
 * Press feedback that lands on the FIRST touch, even while the library is still settling.
 *
 * The scale lives in an [Animatable] read only inside the `graphicsLayer` lambda, so a press
 * animates entirely in the draw phase. Nothing recomposes the card, which is why the very first
 * tap is no longer swallowed by a busy composition and why the grid stops jittering under load.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.xvoxSongPress(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val release = remember { mutableStateOf<Job?>(null) }

    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .pointerInput(Unit) {
            awaitEachGesture {
                // Initial pass: the touch is seen before the list's scroll handler can claim it.
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                release.value?.cancel()
                scope.launch { scale.animateTo(0.94f, PressIn) }
                try {
                    waitForUpOrCancellation(PointerEventPass.Initial)
                } finally {
                    // Hold the pulse a beat so a flick-fast tap is still visible.
                    release.value = scope.launch { delay(70); scale.animateTo(1f, PressOut) }
                }
            }
        }
        .combinedClickable(
            hapticFeedbackEnabled = false,
            interactionSource = interaction,
            indication = null,
            onClick = { click() }, // Dispatch immediately; the visual pulse never delays audio.
            onLongClick = if (onLongClick != null) ({ longClick?.invoke() }) else null
        )
}
