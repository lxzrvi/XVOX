package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object XvoxPlayerTransitionMotion {
    const val Duration = 245

    private val easing =
        CubicBezierEasing(
            0.22f,
            0.61f,
            0.36f,
            1f
        )

    val spec: AnimationSpec<Float>
        get() = tween(
            durationMillis = Duration,
            easing = easing
        )
}
