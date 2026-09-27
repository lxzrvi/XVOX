package com.xvox.music.player.nowplaying.lyrics

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Temporary, selectable card-to-fullscreen motion studies for lyrics. Every numbered choice has
 * its own timing curve and in-flight geometry pulse; style 0 is the stable product default.
 */
data class LyricsFullscreenMotion(
    val durationMillis: Int,
    val easing: Easing,
    /** Horizontal excursion used only between the two resting endpoints. */
    val horizontalPulseDp: Float = 0f,
    /** Vertical excursion used only between the two resting endpoints. */
    val verticalPulseDp: Float = 0f,
    /** Small temporary scale dip used only between the two resting endpoints. */
    val scaleDip: Float = 0f,
    /** Small temporary rotation used only between the two resting endpoints. */
    val tiltDegrees: Float = 0f
)

fun lyricsFullscreenMotion(style: Int): LyricsFullscreenMotion = when (style.coerceIn(0, 30)) {
    0 -> LyricsFullscreenMotion(460, CubicBezierEasing(.20f, 0f, 0f, 1f))
    1 -> LyricsFullscreenMotion(280, CubicBezierEasing(.12f, .82f, .20f, 1f), verticalPulseDp = -8f)
    2 -> LyricsFullscreenMotion(320, CubicBezierEasing(.34f, 1.34f, .64f, 1f), verticalPulseDp = -13f, scaleDip = .025f)
    3 -> LyricsFullscreenMotion(360, CubicBezierEasing(.15f, .05f, .25f, 1f), horizontalPulseDp = -12f)
    4 -> LyricsFullscreenMotion(400, CubicBezierEasing(.25f, .80f, .25f, 1f), verticalPulseDp = 12f, scaleDip = .035f)
    5 -> LyricsFullscreenMotion(430, CubicBezierEasing(.48f, 0f, .20f, 1f), horizontalPulseDp = 14f, tiltDegrees = 1.2f)
    6 -> LyricsFullscreenMotion(470, CubicBezierEasing(.16f, 1.08f, .30f, 1f), verticalPulseDp = -18f, scaleDip = .050f)
    7 -> LyricsFullscreenMotion(510, CubicBezierEasing(.42f, 0f, .16f, 1f), horizontalPulseDp = -18f, tiltDegrees = -1.6f)
    8 -> LyricsFullscreenMotion(540, CubicBezierEasing(.22f, .72f, .18f, 1f), verticalPulseDp = 18f, scaleDip = .040f)
    9 -> LyricsFullscreenMotion(580, CubicBezierEasing(.40f, .02f, .08f, 1f), horizontalPulseDp = 20f, verticalPulseDp = -8f)
    10 -> LyricsFullscreenMotion(620, CubicBezierEasing(.18f, 1.12f, .35f, 1f), verticalPulseDp = -22f, scaleDip = .065f)
    11 -> LyricsFullscreenMotion(660, CubicBezierEasing(.60f, 0f, .18f, 1f), horizontalPulseDp = -22f, tiltDegrees = 2f)
    12 -> LyricsFullscreenMotion(700, CubicBezierEasing(.10f, .92f, .18f, 1f), verticalPulseDp = 23f, scaleDip = .055f)
    13 -> LyricsFullscreenMotion(300, CubicBezierEasing(.40f, 1.22f, .45f, 1f), horizontalPulseDp = 9f, tiltDegrees = .8f)
    14 -> LyricsFullscreenMotion(350, CubicBezierEasing(.18f, .98f, .30f, 1f), horizontalPulseDp = -10f, verticalPulseDp = -11f)
    15 -> LyricsFullscreenMotion(390, CubicBezierEasing(.32f, 0f, .12f, 1f), verticalPulseDp = 15f, tiltDegrees = -1f)
    16 -> LyricsFullscreenMotion(440, CubicBezierEasing(.21f, 1.16f, .45f, 1f), horizontalPulseDp = 15f, scaleDip = .060f)
    17 -> LyricsFullscreenMotion(490, CubicBezierEasing(.52f, 0f, .24f, 1f), horizontalPulseDp = -15f, verticalPulseDp = 9f)
    18 -> LyricsFullscreenMotion(530, CubicBezierEasing(.09f, .73f, .12f, 1f), verticalPulseDp = -16f, scaleDip = .030f, tiltDegrees = 1.4f)
    19 -> LyricsFullscreenMotion(570, CubicBezierEasing(.28f, .98f, .28f, 1f), horizontalPulseDp = 18f, verticalPulseDp = -14f)
    20 -> LyricsFullscreenMotion(610, CubicBezierEasing(.55f, 0f, .28f, 1f), verticalPulseDp = 20f, scaleDip = .070f)
    21 -> LyricsFullscreenMotion(650, CubicBezierEasing(.14f, 1.20f, .38f, 1f), horizontalPulseDp = -20f, tiltDegrees = -2.1f)
    22 -> LyricsFullscreenMotion(690, CubicBezierEasing(.24f, .64f, .10f, 1f), verticalPulseDp = -25f, scaleDip = .045f)
    23 -> LyricsFullscreenMotion(740, CubicBezierEasing(.44f, .04f, .12f, 1f), horizontalPulseDp = 24f, verticalPulseDp = 10f)
    24 -> LyricsFullscreenMotion(780, CubicBezierEasing(.16f, .88f, .14f, 1f), verticalPulseDp = 26f, scaleDip = .075f, tiltDegrees = 1.7f)
    25 -> LyricsFullscreenMotion(330, CubicBezierEasing(.30f, 1.28f, .58f, 1f), horizontalPulseDp = -8f, verticalPulseDp = -17f)
    26 -> LyricsFullscreenMotion(410, CubicBezierEasing(.46f, 0f, .14f, 1f), horizontalPulseDp = 13f, scaleDip = .032f, tiltDegrees = -1.3f)
    27 -> LyricsFullscreenMotion(500, CubicBezierEasing(.11f, 1.03f, .36f, 1f), verticalPulseDp = -20f, scaleDip = .058f)
    28 -> LyricsFullscreenMotion(590, CubicBezierEasing(.36f, .03f, .10f, 1f), horizontalPulseDp = -24f, verticalPulseDp = 14f, tiltDegrees = 2.3f)
    29 -> LyricsFullscreenMotion(680, CubicBezierEasing(.20f, .86f, .10f, 1f), horizontalPulseDp = 21f, verticalPulseDp = -21f, scaleDip = .070f)
    else -> LyricsFullscreenMotion(820, CubicBezierEasing(.18f, 1.18f, .40f, 1f), horizontalPulseDp = -26f, verticalPulseDp = 25f, scaleDip = .085f, tiltDegrees = -2.5f)
}
