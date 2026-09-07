package com.xvox.music.core.ui.effects

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Down feedback is immediate (also inside a scrolling list); a quick tap gets one visible pulse. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.xvoxSongPress(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var held by remember { mutableStateOf(false) }
    var pulse by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (held || pulse) 0.955f else 1f,
        spring(dampingRatio = 0.8f, stiffness = 1500f), label = "songPress")
    graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                held = true
                try { waitForUpOrCancellation() } finally { held = false }
            }
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                scope.launch { pulse = true; delay(90); pulse = false }
                click()
            },
            onLongClick = if (onLongClick != null) ({ longClick?.invoke() }) else null
        )
}
