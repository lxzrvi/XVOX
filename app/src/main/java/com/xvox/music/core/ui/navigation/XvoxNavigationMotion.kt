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
    dragging: Boolean
): XvoxNavigationMotion {

    /*
     * DURING DRAG:
     *
     * Do NOT animate position.
     *
     * The pill follows the finger immediately.
     *
     * This removes the "behind my finger" feeling.
     *
     * AFTER DRAG:
     *
     * Keep a tiny spring only for settling.
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

    /*
     * Direct while dragging.
     * Slightly animated when releasing.
     */
    val finalPosition =
        if (dragging) {
            position
        } else {
            animatedPosition
        }

    /*
     * Grow animation.
     *
     * Fast enough that it doesn't feel delayed.
     */
    val grow by
        animateFloatAsState(
            targetValue =
                if (dragging) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                spring(
                    dampingRatio = 0.88f,
                    stiffness = 1500f
                ),
            label = "navGrow"
        )

    /*
     * Keep navbar scale extremely subtle.
     *
     * Your previous 1.04f made the whole bar
     * feel like it was bouncing.
     */
    val barScale by
        animateFloatAsState(
            targetValue =
                if (dragging) {
                    1.018f
                } else {
                    1f
                },
            animationSpec =
                spring(
                    dampingRatio = 0.88f,
                    stiffness = 1500f
                ),
            label = "navBarScale"
        )

    return XvoxNavigationMotion(
        position = finalPosition,
        grow = grow,
        barScale = barScale
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
