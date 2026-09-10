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
import android.os.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
 * Tap = [onTap]; press-and-hold = a continuous ±[rate]× position scrub from wherever playback was
 * when the hold began. While held, [onScrubTo] is fed the target position every [tickEvery] ms,
 * advancing at [rate]× real time (e.g. 2× forward for Next, 2× backward for Previous). A release
 * right after a hold never fires the tap.
 */
fun Modifier.xvoxTapOrScrub(
    enabled: Boolean = true,
    onTap: () -> Unit,
    onScrubTo: (Long) -> Unit,
    direction: Int = 1,
    positionMs: () -> Long,
    durationMs: () -> Long,
    rate: Float = 2f,
    longPressDelay: Long = 460,
    tickEvery: Long = 40
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val currentTap by rememberUpdatedState(onTap)
    val currentScrub by rememberUpdatedState(onScrubTo)
    val currentPos by rememberUpdatedState(positionMs)
    val currentDur by rememberUpdatedState(durationMs)
    pointerInput(enabled, direction, rate, longPressDelay, tickEvery) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var held = false
            val job = scope.launch {
                delay(longPressDelay)
                held = true
                val anchor = currentPos().coerceAtLeast(0L)
                val started = SystemClock.uptimeMillis()
                while (isActive) {
                    val dur = currentDur()
                    val elapsed = SystemClock.uptimeMillis() - started
                    var target = anchor + (direction * rate * elapsed).toLong()
                    if (dur > 0L) target = target.coerceIn(0L, dur)
                    currentScrub(target.coerceAtLeast(0L))
                    delay(tickEvery)
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

/**
 * Tap = [onTap]; press-and-hold = [onBoostChange] true until release, then false.
 *
 * Unlike a scrub this never moves the playhead, so the deck keeps streaming: holding Next plays
 * faster through the same audio pipeline, with no cut, gap or voice break.
 */
fun Modifier.xvoxTapOrBoost(
    enabled: Boolean = true,
    onTap: () -> Unit,
    onBoostChange: (Boolean) -> Unit,
    longPressDelay: Long = 420
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val currentTap by rememberUpdatedState(onTap)
    val currentBoost by rememberUpdatedState(onBoostChange)
    pointerInput(enabled, longPressDelay) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var held = false
            val job = scope.launch {
                delay(longPressDelay)
                held = true
                currentBoost(true)
            }
            try {
                waitForUpOrCancellation(PointerEventPass.Initial)
            } finally {
                job.cancel()
                // Always drop the boost, even if the gesture was cancelled mid-hold.
                if (held) currentBoost(false)
                held = false
            }
            if (!held) currentTap()
        }
    }
}
