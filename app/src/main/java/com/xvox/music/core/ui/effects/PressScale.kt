package com.xvox.music.core.ui.effects

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.xvoxPressScale(
    onClick: () -> Unit
): Modifier =
    androidx.compose.ui.composed {
        val interactionSource =
            remember {
                MutableInteractionSource()
            }

        val pressed by
            interactionSource.collectIsPressedAsState()

        val scale by animateFloatAsState(
            targetValue =
                if (pressed) 0.965f else 1f,
            animationSpec = spring(
                stiffness = 700f,
                dampingRatio = 0.78f
            ),
            label = "xvoxPress"
        )

        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    }
