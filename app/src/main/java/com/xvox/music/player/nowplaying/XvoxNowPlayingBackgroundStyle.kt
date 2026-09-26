package com.xvox.music.player.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.xvox.music.core.design.theme.XvoxTheme

/** The background menu deliberately retains one faithful, artwork-derived Default backdrop. */
data class XvoxNowPlayingBackgroundOption(
    val key: String,
    val title: String
)

object XvoxNowPlayingBackgroundStyles {
    const val DEFAULT = "default"
    val options = listOf(XvoxNowPlayingBackgroundOption(DEFAULT, "Default"))
    fun normalize(value: String?): String = DEFAULT
}

/** Twenty distinct cover-change treatments selected from Now Playing's options sheet. */
data class XvoxCoverTransitionOption(
    val key: String,
    val title: String,
    val subtitle: String
)

object XvoxCoverTransitionStyles {
    const val SLIDE = "slide"
    val options = listOf(
        XvoxCoverTransitionOption(SLIDE, "Slide", "Natural horizontal handoff"),
        XvoxCoverTransitionOption("push", "Push", "Cover pushes the previous one away"),
        XvoxCoverTransitionOption("parallax", "Parallax", "Artwork moves at different depth"),
        XvoxCoverTransitionOption("fade", "Fade", "Soft opacity exchange"),
        XvoxCoverTransitionOption("scale", "Scale", "Subtle shrink and grow"),
        XvoxCoverTransitionOption("zoom", "Zoom", "Forward zoom handoff"),
        XvoxCoverTransitionOption("depth", "Depth", "Receding card depth"),
        XvoxCoverTransitionOption("flip_x", "Flip X", "Horizontal card flip"),
        XvoxCoverTransitionOption("flip_y", "Flip Y", "Vertical card flip"),
        XvoxCoverTransitionOption("rotate_cw", "Rotate CW", "Clockwise turn"),
        XvoxCoverTransitionOption("rotate_ccw", "Rotate CCW", "Counter-clockwise turn"),
        XvoxCoverTransitionOption("tilt", "Tilt", "Gentle side tilt"),
        XvoxCoverTransitionOption("rise", "Rise", "Incoming cover rises"),
        XvoxCoverTransitionOption("drop", "Drop", "Incoming cover drops"),
        XvoxCoverTransitionOption("reveal", "Reveal", "Edge reveal"),
        XvoxCoverTransitionOption("stack", "Stack", "Layered card stack"),
        XvoxCoverTransitionOption("pop", "Pop", "Quick pop-in emphasis"),
        XvoxCoverTransitionOption("drift", "Drift", "Long drifting handoff"),
        XvoxCoverTransitionOption("swing", "Swing", "Swinging pivot"),
        XvoxCoverTransitionOption("glide", "Glide", "Low-friction glide")
    )

    fun normalize(value: String?): String = options.firstOrNull { it.key == value }?.key ?: SLIDE
}

/** How the Default artwork backdrop reacts when the cover changes. */
data class XvoxBackgroundTransitionOption(
    val key: String,
    val title: String,
    val subtitle: String
)

object XvoxBackgroundTransitionStyles {
    const val TRANSITION = "transition"
    const val MORPH = "morph"
    const val CROSSFADE = "crossfade"
    val options = listOf(
        XvoxBackgroundTransitionOption(TRANSITION, "Transition", "Follow cover movement"),
        XvoxBackgroundTransitionOption(MORPH, "Morph", "Blend palette tones smoothly"),
        XvoxBackgroundTransitionOption(CROSSFADE, "Crossfade", "Fade between cover palettes")
    )
    fun normalize(value: String?): String = options.firstOrNull { it.key == value }?.key ?: TRANSITION
}

/** Theme-safe fallback when artwork has not produced a palette colour yet. */
@Composable
private fun backgroundSource(dominant: Color): Color {
    val colors = XvoxTheme.colors
    return dominant.takeIf { it != Color.Unspecified && it != Color.Transparent } ?: colors.primaryAccent
}

/** Default remains the original single dominant-colour backdrop. */
@Composable
fun xvoxNowPlayingBackgroundColor(style: String, dominant: Color): Color = backgroundSource(dominant)

/** Retained for source compatibility; Default intentionally has no alternate gradient treatment. */
@Composable
fun xvoxNowPlayingBackgroundBrush(style: String, dominant: Color): Brush? = null
