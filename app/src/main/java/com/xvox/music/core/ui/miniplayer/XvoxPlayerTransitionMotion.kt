package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

object XvoxPlayerTransitionMotion {

    const val Duration = 280

    val easing: Easing =
        CubicBezierEasing(
            0.20f,
            0.0f,
            0.20f,
            1.0f
        )

    val spec: AnimationSpec<Float>
        get() = tween(
            durationMillis = Duration,
            easing = easing
        )
}
