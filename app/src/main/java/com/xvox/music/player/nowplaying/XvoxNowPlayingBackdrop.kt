package com.xvox.music.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(targetColor)
    )
}
