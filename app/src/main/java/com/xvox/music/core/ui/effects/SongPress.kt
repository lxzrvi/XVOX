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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PressIn = spring<Float>(dampingRatio = 0.92f, stiffness = 3600f)
private val PressOut = spring<Float>(dampingRatio = 0.72f, stiffness = 900f)

/**
 * Press feedback for song cards: responsive low haptics on tap, heavy haptic on long click,
 * and immediate audio dispatch.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.xvoxSongPress(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    /** How far the tile itself dips. 1f leaves the frame still. */
    pressedScale: Float = 0.96f,
    /** Optional hook for cards. */
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

    val modifier = if (pressedScale != 1f) {
        graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val pressJob = scope.launch {
                        delay(14)
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
                            release.value = scope.launch { delay(46); scale.animateTo(1f, PressOut) }
                        }
                    }
                }
            }
    } else Modifier

    modifier.combinedClickable(
        hapticFeedbackEnabled = false,
        interactionSource = interaction,
        indication = null,
        onClick = {
            if (hapticOnTap) haptics.tap()
            click()
        },
        onLongClick = if (onLongClick != null) ({
            haptics.heavy()
            longClick?.invoke()
        }) else null
    )
}
