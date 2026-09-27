package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * The single product card-to-fullscreen lyrics motion.
 *
 * Style 20 is intentionally no longer user-selectable. Its original deliberate upward travel
 * and shallow scale settle are now the one permanent product motion in both orientations.
 * There is no selector and no alternate fallback profile.
 */
data class LyricsFullscreenMotion(
    val durationMillis: Int,
    val easing: Easing,
    val horizontalPulseDp: Float = 0f,
    val verticalPulseDp: Float = 0f,
    val scaleDip: Float = 0f,
    val tiltDegrees: Float = 0f
)

const val LockedLyricsFullscreenStyle = 20

/** The former experimental style 20, now locked as the permanent fullscreen motion. */
fun lyricsFullscreenMotion(): LyricsFullscreenMotion = LyricsFullscreenMotion(
    durationMillis = 610,
    easing = CubicBezierEasing(.55f, 0f, .28f, 1f),
    verticalPulseDp = 20f,
    scaleDip = .070f
)

/** Compatibility overload for code or persisted records written before style selection was removed. */
@Deprecated("Fullscreen lyrics motion is permanently style 20")
fun lyricsFullscreenMotion(@Suppress("UNUSED_PARAMETER") style: Int): LyricsFullscreenMotion =
    lyricsFullscreenMotion()
