package com.xvox.music.player.nowplaying

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * The Now Playing backdrop has one Default surface.  Its persisted transition setting controls
 * only how the cover-derived palette hands off between songs; it never reintroduces alternate
 * background styles.
 */
@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    style: String = XvoxNowPlayingBackgroundStyles.DEFAULT,
    transition: String = XvoxBackgroundTransitionStyles.TRANSITION,
    modifier: Modifier = Modifier
) {
    val target = Color(xvoxNowPlayingBackgroundColor(style, dominant).toArgb())
    when (XvoxBackgroundTransitionStyles.normalize(transition)) {
        XvoxBackgroundTransitionStyles.MORPH -> {
            val morphed by animateColorAsState(
                targetValue = target,
                animationSpec = tween(520),
                label = "nowPlayingBackdropMorph"
            )
            DefaultBackdrop(color = morphed, modifier = modifier)
        }
        XvoxBackgroundTransitionStyles.CROSSFADE -> {
            Crossfade(
                targetState = target.toArgb(),
                animationSpec = tween(300),
                label = "nowPlayingBackdropCrossfade"
            ) { argb ->
                DefaultBackdrop(color = Color(argb), modifier = modifier)
            }
        }
        else -> DefaultBackdrop(color = target, modifier = modifier)
    }
}

@Composable
private fun DefaultBackdrop(color: Color, modifier: Modifier) {
    // style remains normalised to Default by XvoxNowPlayingBackgroundStyles; the argument is kept
    // at the public call site for backwards-compatible saved data.
    Box(modifier = modifier.fillMaxSize().background(color))
}
