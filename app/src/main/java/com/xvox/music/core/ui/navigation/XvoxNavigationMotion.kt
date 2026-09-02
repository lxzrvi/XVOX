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

    val animatedPosition by
        animateFloatAsState(
            targetValue = position,
            animationSpec =
                spring(
                    dampingRatio = 0.72f,
                    stiffness = 900f
                ),
            label = "navPosition"
        )

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
                    dampingRatio = 0.78f,
                    stiffness = 850f
                ),
            label = "navGrow"
        )

    val barScale by
        animateFloatAsState(
            targetValue =
                if (dragging) {
                    1.04f
                } else {
                    1f
                },
            animationSpec =
                spring(
                    dampingRatio = 0.78f,
                    stiffness = 900f
                ),
            label = "navBar"
        )

    return XvoxNavigationMotion(
        position =
            animatedPosition,
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
    val amount =
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
                amount,
        green =
            inactive.green +
                (
                    active.green -
                        inactive.green
                    ) *
                amount,
        blue =
            inactive.blue +
                (
                    active.blue -
                        inactive.blue
                    ) *
                amount,
        alpha =
            inactive.alpha +
                (
                    active.alpha -
                        inactive.alpha
                    ) *
                amount
    )
}
