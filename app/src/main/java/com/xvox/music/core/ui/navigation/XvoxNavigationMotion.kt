package com.xvox.music.core.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

data class XvoxNavigationMotion(
    val position: Float
)

@Composable
fun rememberXvoxNavigationMotion(
    position: Float
): XvoxNavigationMotion {
    val animatedPosition by
        animateFloatAsState(
            targetValue =
                position,
            animationSpec =
                spring(
                    dampingRatio = 0.88f,
                    stiffness = 620f
                ),
            label =
                "navPosition"
        )

    return XvoxNavigationMotion(
        position =
            animatedPosition
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
