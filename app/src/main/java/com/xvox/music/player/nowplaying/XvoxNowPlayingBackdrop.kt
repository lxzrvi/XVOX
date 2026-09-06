package com.xvox.music.player.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    modifier: Modifier = Modifier
) {
    val targetColor = if (dominant != Color.Unspecified && dominant != Color.Transparent) {
        dominant
    } else {
        Color(0xFF16161E)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 350),
        label = "np_backdrop_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedColor)
    )
}
