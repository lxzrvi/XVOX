package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

object XvoxPlayerTransitionMotion {

    /** Shared deck motion: the mini player and Now Playing deliberately move at one calm pace. */
    const val Duration = 320

    val easing: Easing =
        CubicBezierEasing(
            0.22f,
            0.0f,
            0.0f,
            1.0f
        )

    val spec: AnimationSpec<Float>
        get() = tween(
            durationMillis = Duration,
            easing = easing
        )
}
