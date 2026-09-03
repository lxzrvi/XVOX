package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring

object XvoxNowPlayingMotion {

    const val DismissThreshold = 110f
    const val ArtworkSwipeThreshold = 72f

    val enter:
        AnimationSpec<Float>
        get() =
            spring(
                dampingRatio = 0.88f,
                stiffness = 310f
            )

    val exit:
        AnimationSpec<Float>
        get() =
            spring(
                dampingRatio = 0.90f,
                stiffness = 340f
            )

    val returnToRest:
        AnimationSpec<Float>
        get() =
            spring(
                dampingRatio = 0.88f,
                stiffness = 520f
            )
}
