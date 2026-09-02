package com.xvox.music.core.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

data class XvoxNavigationMotion(
    val position: Float,
    val grow: Float,
    val barScale: Float
)

@Composable
fun rememberXvoxNavigationMotion(
    position: Float,
    dragging: Boolean,
    holding: Boolean
): XvoxNavigationMotion {

    /*
     * When actively dragging, the selector follows
     * the finger directly.
     *
     * Tap / release selection uses a short spring.
     */
    val animatedPosition by
        animateFloatAsState(
            targetValue = position,
            animationSpec =
                spring(
                    dampingRatio = 0.90f,
                    stiffness = 1400f
                ),
            label = "navPosition"
        )

    val finalPosition =
        if (dragging) {
            position
        } else {
            animatedPosition
        }

    /*
     * IMPORTANT:
     *
     * Dragging does NOT grow the selector.
     *
     * Only a real stationary hold can grow it.
     */
    val grow by
        animateFloatAsState(
            targetValue =
                if (holding) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                spring(
                    dampingRatio = 0.86f,
                    stiffness = 1250f
                ),
            label = "navGrow"
        )

    /*
     * Parent navigation stays physically fixed.
     *
     * No tap zoom.
     * No drag zoom.
     * No hold zoom of the entire navbar.
     */
    val barScale = 1f

    return XvoxNavigationMotion(
        position =
            finalPosition,
        grow =
            grow,
        barScale =
            barScale
    )
}

fun navigationProximity(
    position: Float,
    index: Int
): Float {
    return (
        1f -
            abs(
                position -
                    index.toFloat()
            )
        )
        .coerceIn(
            0f,
            1f
        )
}

fun navigationColor(
    inactive: Color,
    active: Color,
    proximity: Float
): Color {
    val value =
        proximity.coerceIn(
            0f,
            1f
        )

    return Color(
        red =
            inactive.red +
                (
                    active.red -
                        inactive.red
                    ) *
                value,

        green =
            inactive.green +
                (
                    active.green -
                        inactive.green
                    ) *
                value,

        blue =
            inactive.blue +
                (
                    active.blue -
                        inactive.blue
                    ) *
                value,

        alpha =
            inactive.alpha +
                (
                    active.alpha -
                        inactive.alpha
                    ) *
                value
    )
}
