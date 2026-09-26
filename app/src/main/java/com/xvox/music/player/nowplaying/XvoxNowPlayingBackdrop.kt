package com.xvox.music.player.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * The artwork background remains one Default surface.  Its two persisted choices deliberately do
 * different jobs: BG Method derives the target colour from the cover, while Background Transition
 * chooses how consecutive target colours hand off.
 */
@Composable
fun XvoxNowPlayingBackdrop(
    dominant: Color,
    style: String = XvoxNowPlayingBackgroundStyles.DEFAULT,
    transition: String = XvoxBackgroundTransitionStyles.DISSOLVE,
    method: String = XvoxBackgroundMethodStyles.DOMINANT,
    modifier: Modifier = Modifier
) {
    val base = xvoxNowPlayingBackgroundColor(style, dominant)
    val target = xvoxNowPlayingBackgroundMethodColor(method, base)
    val selectedTransition = XvoxBackgroundTransitionStyles.normalize(transition)

    // Morph is deliberately continuous rather than content-swapping, preserving its own identity
    // among the twenty effects.
    if (selectedTransition == "morph") {
        val morphed by animateColorAsState(
            targetValue = target,
            animationSpec = tween<Color>(520, easing = CubicBezierEasing(.2f, 0f, 0f, 1f)),
            label = "nowPlayingBackdropMorph"
        )
        DefaultBackdrop(color = morphed, modifier = modifier)
        return
    }

    AnimatedContent(
        targetState = target.toArgb(),
        transitionSpec = {
            when (selectedTransition) {
                "crossfade" -> fadeIn(tween(360)) togetherWith fadeOut(tween(320))
                "drift_left" ->
                    (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(430, easing = FastOutSlowInEasing)) + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(410, easing = FastOutSlowInEasing)) + fadeOut(tween(240)))
                "drift_right" ->
                    (slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(430, easing = FastOutSlowInEasing)) + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(410, easing = FastOutSlowInEasing)) + fadeOut(tween(240)))
                "rise" ->
                    (slideInVertically(initialOffsetY = { it }, animationSpec = tween(410, easing = FastOutSlowInEasing)) + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(390, easing = FastOutSlowInEasing)) + fadeOut(tween(230)))
                "fall" ->
                    (slideInVertically(initialOffsetY = { -it }, animationSpec = tween(410, easing = FastOutSlowInEasing)) + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(390, easing = FastOutSlowInEasing)) + fadeOut(tween(230)))
                "zoom_in" ->
                    (scaleIn(initialScale = .78f, animationSpec = tween(420, easing = FastOutSlowInEasing)) + fadeIn(tween(250)))
                        .togetherWith(scaleOut(targetScale = 1.12f, animationSpec = tween(340)) + fadeOut(tween(300)))
                "zoom_out" ->
                    (scaleIn(initialScale = 1.20f, animationSpec = tween(420, easing = FastOutSlowInEasing)) + fadeIn(tween(230)))
                        .togetherWith(scaleOut(targetScale = .82f, animationSpec = tween(340)) + fadeOut(tween(300)))
                "depth" ->
                    (scaleIn(initialScale = .70f, animationSpec = tween(470, easing = FastOutSlowInEasing)) + fadeIn(tween(290)))
                        .togetherWith(scaleOut(targetScale = .90f, animationSpec = tween(390)) + fadeOut(tween(310)))
                "sweep" ->
                    (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(250)) + fadeIn(tween(130)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(250)) + fadeOut(tween(160)))
                "curtain" ->
                    (scaleIn(initialScale = .55f, animationSpec = tween(380, easing = CubicBezierEasing(.16f, 1f, .3f, 1f))) + fadeIn(tween(170)))
                        .togetherWith(scaleOut(targetScale = .96f, animationSpec = tween(300)) + fadeOut(tween(220)))
                "pulse" ->
                    (scaleIn(initialScale = 1.10f, animationSpec = tween(180)) + fadeIn(tween(150)))
                        .togetherWith(scaleOut(targetScale = .95f, animationSpec = tween(240)) + fadeOut(tween(210)))
                "bloom" ->
                    (scaleIn(initialScale = .40f, animationSpec = tween(650, easing = CubicBezierEasing(.12f, .8f, .2f, 1f))) + fadeIn(tween(420)))
                        .togetherWith(fadeOut(tween(460)))
                "ripple" ->
                    (scaleIn(initialScale = .16f, animationSpec = tween(530, easing = FastOutSlowInEasing)) + fadeIn(tween(300)))
                        .togetherWith(scaleOut(targetScale = 1.05f, animationSpec = tween(420)) + fadeOut(tween(350)))
                "orbit" ->
                    (slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(480, easing = CubicBezierEasing(.2f, .9f, .25f, 1f))) +
                        slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(480, easing = CubicBezierEasing(.2f, .9f, .25f, 1f))) + fadeIn(tween(260)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(430)) + fadeOut(tween(290)))
                "tilt" ->
                    (slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(360, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = .88f, animationSpec = tween(360)) + fadeIn(tween(220)))
                        .togetherWith(slideOutVertically(targetOffsetY = { it / 8 }, animationSpec = tween(330)) + fadeOut(tween(240)))
                "flicker" -> fadeIn(tween(95)) togetherWith fadeOut(tween(95))
                "breathe" -> fadeIn(tween(760, easing = CubicBezierEasing(.18f, 0f, .12f, 1f))) togetherWith fadeOut(tween(640))
                "snap" -> fadeIn(tween(130)) togetherWith fadeOut(tween(80))
                else -> fadeIn(tween(300, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(260))
            }
        },
        label = "nowPlayingBackdrop_${selectedTransition}"
    ) { argb ->
        DefaultBackdrop(color = Color(argb), modifier = modifier)
    }
}

@Composable
private fun DefaultBackdrop(color: Color, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize().background(color))
}
