package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * The single product card-to-fullscreen lyrics motion.
 *
 * Style 20 is intentionally no longer user-selectable.  Its restrained easing has no overshoot,
 * pulse, scale dip, or rotation, so the same card geometry stays smooth on portrait and landscape
 * displays and never exposes a transient edge strip while the surface expands.
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

fun lyricsFullscreenMotion(): LyricsFullscreenMotion = LyricsFullscreenMotion(
    durationMillis = 440,
    easing = CubicBezierEasing(.22f, 0f, .12f, 1f)
)

/** Compatibility overload for code or persisted records written before style selection was removed. */
@Deprecated("Fullscreen lyrics motion is permanently style 20")
fun lyricsFullscreenMotion(@Suppress("UNUSED_PARAMETER") style: Int): LyricsFullscreenMotion =
    lyricsFullscreenMotion()
