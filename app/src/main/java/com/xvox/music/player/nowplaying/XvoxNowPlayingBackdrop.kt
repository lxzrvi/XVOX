package com.xvox.music.player.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * One fixed, calm background handoff. There is no user-facing transition mode: only the cover's
 * adaptive dominant colour changes, and it eases into place with the familiar Default behavior.
 */
@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    style: String = XvoxNowPlayingBackgroundStyles.DEFAULT,
    modifier: Modifier = Modifier
) {
    val target = xvoxNowPlayingBackgroundColor(style, dominant)
    val displayed by animateColorAsState(
        targetValue = target,
        animationSpec = tween<Color>(460, easing = CubicBezierEasing(.2f, 0f, 0f, 1f)),
        label = "nowPlayingAdaptiveBackground"
    )
    Box(modifier = modifier.fillMaxSize().background(displayed))
}
