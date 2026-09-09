package com.xvox.music.core.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TapIn = spring<Float>(dampingRatio = 0.9f, stiffness = 2400f)
private val TapOut = spring<Float>(dampingRatio = 0.72f, stiffness = 850f)

/**
 * One gesture for a button that also repeats while held: a short tap calls [onTap]; pressing and
 * holding fires [onHoldFire] after [longPressDelay] and again every [repeatEvery] until release.
 * A release right after a hold never fires the tap, so holding Next skips fast and releasing it
 * does not skip one extra track.
 */
fun Modifier.xvoxTapOrHold(
    enabled: Boolean = true,
    onTap: () -> Unit,
    onHoldFire: () -> Unit = onTap,
    longPressDelay: Long = 480,
    repeatEvery: Long = 360
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val currentTap by rememberUpdatedState(onTap)
    val currentFire by rememberUpdatedState(onHoldFire)
    pointerInput(enabled, longPressDelay, repeatEvery) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var held = false
            val job = scope.launch {
                delay(longPressDelay)
                held = true
                currentFire()
                while (true) {
                    delay(repeatEvery)
                    currentFire()
                }
            }
            try {
                waitForUpOrCancellation(PointerEventPass.Initial)
            } finally {
                job.cancel()
            }
            if (!held) currentTap()
        }
    }
}

/**
 * Same first-touch guarantee as [xvoxSongPress], for buttons, pills and rows: the scale is
 * animated off-composition so the response is instant no matter what else is on screen.
 */
fun Modifier.xvoxPressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val release = remember { mutableStateOf<Job?>(null) }
    val click by rememberUpdatedState(onClick)

    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .then(
            if (!enabled) Modifier else Modifier.pointerInput(pressedScale) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    release.value?.cancel()
                    scope.launch { scale.animateTo(pressedScale, TapIn) }
                    try {
                        waitForUpOrCancellation(PointerEventPass.Initial)
                    } finally {
                        release.value = scope.launch { delay(60); scale.animateTo(1f, TapOut) }
                    }
                }
            }
        )
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null
        ) { click() }
}
