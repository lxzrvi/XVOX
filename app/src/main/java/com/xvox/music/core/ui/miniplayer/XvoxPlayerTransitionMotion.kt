package com.xvox.music.core.ui.miniplayer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

object XvoxPlayerTransitionMotion {
    const val Duration = 320

    val spec: AnimationSpec<Float>
        get() = tween(
            durationMillis = Duration,
            easing = FastOutSlowInEasing
        )
}
