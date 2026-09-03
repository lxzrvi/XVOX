package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object XvoxNowPlayingMotion {
    const val DismissThreshold = 110f

    private const val TransitionDuration = 390

    private val fluidEasing =
        CubicBezierEasing(
            0.16f,
            0.78f,
            0.20f,
            1f
        )

    val enter: AnimationSpec<Float>
        get() = tween(
            durationMillis = TransitionDuration,
            easing = fluidEasing
        )

    val exit: AnimationSpec<Float>
        get() = tween(
            durationMillis = TransitionDuration,
            easing = fluidEasing
        )

    val returnToRest: AnimationSpec<Float>
        get() = spring(
            dampingRatio = 0.90f,
            stiffness = 460f
        )
}
