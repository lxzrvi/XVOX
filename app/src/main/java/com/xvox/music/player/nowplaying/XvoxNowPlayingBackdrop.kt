package com.xvox.music.player.nowplaying

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Artwork-derived Now Playing backdrop. Style changes crossfade; Default remains the original
 * dominant-colour fill while the nine optional treatments derive their tones from that cover.
 */
@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    style: String = XvoxNowPlayingBackgroundStyles.DEFAULT,
    modifier: Modifier = Modifier
) {
    val normalizedStyle = XvoxNowPlayingBackgroundStyles.normalize(style)
    val target = normalizedStyle to dominant.toArgb()
    Crossfade(
        targetState = target,
        animationSpec = tween(280),
        label = "nowPlayingBackgroundStyleCrossfade"
    ) { (targetStyle, targetArgb) ->
        val targetDominant = Color(targetArgb)
        val brush = xvoxNowPlayingBackgroundBrush(targetStyle, targetDominant)
        val fill = xvoxNowPlayingBackgroundColor(targetStyle, targetDominant)
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(if (brush != null) Modifier.background(brush) else Modifier.background(fill))
        )
    }
}
