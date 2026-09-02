package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import kotlin.math.abs
import kotlin.math.sign

enum class XvoxMiniAxis {
    NONE,
    HORIZONTAL,
    VERTICAL
}

object XvoxMiniPlayerMotion {

    const val AxisThreshold = 9f

    const val HorizontalThreshold = 48f

    const val OpenThreshold = -46f
    const val CloseThreshold = 44f

    const val PreviewDelay = 320L

    fun horizontalResistance(
        value: Float
    ): Float {
        val free = 76f
        val distance = abs(value)

        if (distance <= free) {
            return value
        }

        return (
            free +
                (distance - free) *
                0.07f
            ) *
            sign(value)
    }

    fun verticalResistance(
        value: Float
    ): Float {
        val free =
            if (value < 0f) {
                66f
            } else {
                44f
            }

        val distance = abs(value)

        if (distance <= free) {
            return value
        }

        return (
            free +
                (distance - free) *
                0.075f
            ) *
            sign(value)
    }

    val riseSpec:
        AnimationSpec<Float>
        get() =
            spring(
                dampingRatio = 0.86f,
                stiffness = 320f
            )

    val exitSpec:
        AnimationSpec<Float>
        get() =
            spring(
                dampingRatio = 0.86f,
                stiffness = 320f
            )

    val horizontalReturnSpec:
        AnimationSpec<Float>
        get() =
            spring(
                dampingRatio = 0.80f,
                stiffness = 470f
            )

    val verticalReturnSpec:
        AnimationSpec<Float>
        get() =
            spring(
                dampingRatio = 0.84f,
                stiffness = 440f
            )

    fun metadataChange(
        direction: Int
    ): ContentTransform {
        val normalized =
            direction.coerceIn(
                -1,
                1
            )

        if (normalized == 0) {
            return fadeIn(
                tween(120)
            ).togetherWith(
                fadeOut(
                    tween(100)
                )
            )
        }

        val enter =
            if (normalized > 0) {
                { height: Int ->
                    -height
                }
            } else {
                { height: Int ->
                    height
                }
            }

        val exit =
            if (normalized > 0) {
                { height: Int ->
                    height
                }
            } else {
                { height: Int ->
                    -height
                }
            }

        return (
            fadeIn(
                tween(115)
            ) +
                slideInVertically(
                    initialOffsetY = enter,
                    animationSpec =
                        tween(
                            durationMillis = 155,
                            easing =
                                FastOutSlowInEasing
                        )
                )
            ).togetherWith(
                fadeOut(
                    tween(95)
                ) +
                    slideOutVertically(
                        targetOffsetY = exit,
                        animationSpec =
                            tween(
                                durationMillis = 140,
                                easing =
                                    FastOutSlowInEasing
                            )
                    )
            )
    }

    fun artworkChange():
        ContentTransform =
        fadeIn(
            tween(135)
        ).togetherWith(
            fadeOut(
                tween(110)
            )
        )
}
