package com.xvox.music.core.ui.effects

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.xvoxPressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    onClick: () -> Unit
): Modifier = composed {

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val scope =
        rememberCoroutineScope()

    val held by
        interactionSource.collectIsPressedAsState()

    var tapPulse by
        remember {
            mutableStateOf(false)
        }

    val pressed =
        held || tapPulse

    val scale by animateFloatAsState(
        targetValue =
            if (pressed) {
                pressedScale
            } else {
                1f
            },
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 850f
        ),
        label = "xvoxPress"
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        enabled = enabled,
        interactionSource =
            interactionSource,
        indication = null
    ) {
        scope.launch {
            tapPulse = true
            delay(75)
            tapPulse = false
        }

        onClick()
    }
}
