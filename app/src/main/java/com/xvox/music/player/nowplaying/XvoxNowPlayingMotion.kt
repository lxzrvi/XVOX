package com.xvox.music.player.nowplaying

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object XvoxNowPlayingMotion {
    const val DismissThreshold = 110f
    const val FullDuration = 345
    const val MinimumExitDuration = 120

    val easing: Easing =
        CubicBezierEasing(
            0.25f,
            0.10f,
            0.25f,
            1f
        )

    fun exitDuration(
        currentOffset: Float,
        screenHeight: Float
    ): Int {
        if (screenHeight <= 0f) {
            return FullDuration
        }

        val remaining =
            (
                (screenHeight - currentOffset) /
                    screenHeight
                )
                .coerceIn(0f, 1f)

        return (
            FullDuration * remaining
            )
            .toInt()
            .coerceAtLeast(
                MinimumExitDuration
            )
    }
}
