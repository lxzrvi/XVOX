package com.xvox.music.player.nowplaying

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xvox.music.core.design.theme.LocalXvoxExperimentalAppearance
import com.xvox.music.core.design.theme.XvoxExperimentalAppearance

/**
 * Cover-matched two-colour Now Playing backdrop.
 *
 * During a cover drag, [dominant] tracks the pager under the finger. Between covers (including
 * next/previous while lyrics are open), it eases directly to the next palette. Default keeps the
 * original single adaptive cover colour; Blur alone opts into the richer, fully opaque gradient.
 * The main player therefore remains an intentional solid cover surface rather than exposing the
 * Home screen through its backdrop.
 */
@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    style: String = XvoxNowPlayingBackgroundStyles.DEFAULT,
    isCoverTransitionInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val target = xvoxNowPlayingBackgroundColor(style, dominant)
    val displayed = remember { Animatable(target, Color.VectorConverter(target.colorSpace)) }
    val liquidBlur = LocalXvoxExperimentalAppearance.current == XvoxExperimentalAppearance.BLUR

    LaunchedEffect(target, isCoverTransitionInProgress) {
        if (isCoverTransitionInProgress) {
            displayed.snapTo(target)
        } else {
            displayed.animateTo(
                targetValue = target,
                animationSpec = tween<Color>(320, easing = CubicBezierEasing(.2f, 0f, 0f, 1f))
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (liquidBlur) {
                    // Blur mode owns the premium two-cover-colour gradient. It is deliberately
                    // opaque: Home must not appear behind the actual Now Playing artwork canvas.
                    Modifier.background(xvoxNowPlayingBackgroundBrush(style, displayed.value))
                } else {
                    // Preserve the established Default appearance exactly: one cover-matched
                    // adaptive colour, without a Blur-only liquid gradient leaking into it.
                    Modifier.background(displayed.value)
                }
            )
    )
}
