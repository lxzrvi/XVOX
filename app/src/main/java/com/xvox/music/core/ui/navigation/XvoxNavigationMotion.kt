package com.xvox.music.core.ui.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private val ContentEasing =
    CubicBezierEasing(
        0.22f,
        1f,
        0.36f,
        1f
    )

private const val ContentDuration = 400

data class XvoxNavigationItemMotion(
    val progress: Float,
    val easedProgress: Float,
    val labelAlpha: Float,
    val pressScale: Float
)

@Composable
fun rememberNavigationItemMotion(
    active: Boolean,
    pressed: Boolean
): XvoxNavigationItemMotion {

    val progress by
        animateFloatAsState(
            targetValue =
                if (active) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                tween(
                    durationMillis =
                        ContentDuration,
                    easing =
                        ContentEasing
                ),
            label = "navContent"
        )

    val pressScale by
        animateFloatAsState(
            targetValue =
                if (pressed) {
                    0.94f
                } else {
                    1f
                },
            animationSpec =
                tween(
                    durationMillis =
                        if (pressed) {
                            70
                        } else {
                            180
                        },
                    easing =
                        ContentEasing
                ),
            label = "navPress"
        )

    val easedProgress =
        smoothStep(
            progress.coerceIn(
                0f,
                1f
            )
        )

    val labelAlpha =
        smoothStep(
            (
                (progress - 0.08f) /
                    0.72f
                )
                .coerceIn(
                    0f,
                    1f
                )
        )

    return XvoxNavigationItemMotion(
        progress =
            progress,
        easedProgress =
            easedProgress,
        labelAlpha =
            labelAlpha,
        pressScale =
            pressScale
    )
}

fun navigationColor(
    inactive: Color,
    active: Color,
    progress: Float
): Color {
    val amount =
        progress.coerceIn(
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

private fun smoothStep(
    value: Float
): Float {
    return value *
        value *
        (
            3f -
                2f *
                value
            )
}
