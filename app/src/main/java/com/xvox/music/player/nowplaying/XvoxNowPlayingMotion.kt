package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion

object XvoxNowPlayingMotion {
    const val DismissThreshold = 110f

    val enter: AnimationSpec<Float>
        get() = XvoxPlayerTransitionMotion.spec

    val exit: AnimationSpec<Float>
        get() = XvoxPlayerTransitionMotion.spec

    val returnToRest: AnimationSpec<Float>
        get() = spring(
            dampingRatio = 0.88f,
            stiffness = 520f
        )
}
