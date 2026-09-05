package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

object XvoxPlayerTransitionMotion {

    const val Duration = 320

    val easing: Easing =
        CubicBezierEasing(
            0.20f,
            0.90f,
            0.10f,
            1f
        )

    val spec: AnimationSpec<Float>
        get() = tween(
            durationMillis = Duration,
            easing = easing
        )
}
