package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object XvoxNowPlayingMotion {
    const val DismissThreshold = 110f

    private val fluidEasing =
        CubicBezierEasing(
            0.20f,
            0.72f,
            0.22f,
            1f
        )

    val enter: AnimationSpec<Float>
        get() = tween(
            durationMillis = 330,
            easing = fluidEasing
        )

    val exit: AnimationSpec<Float>
        get() = tween(
            durationMillis = 285,
            easing = fluidEasing
        )

    val returnToRest: AnimationSpec<Float>
        get() = spring(
            dampingRatio = 0.90f,
            stiffness = 500f
        )
}
