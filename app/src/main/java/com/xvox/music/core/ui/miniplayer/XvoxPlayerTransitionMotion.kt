package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

object XvoxPlayerTransitionMotion {

    const val Duration = 140

    val easing: Easing =
        CubicBezierEasing(
            0.10f,
            0.90f,
            0.20f,
            1.0f
        )

    val spec: AnimationSpec<Float>
        get() = tween(
            durationMillis = Duration,
            easing = easing
        )
}
