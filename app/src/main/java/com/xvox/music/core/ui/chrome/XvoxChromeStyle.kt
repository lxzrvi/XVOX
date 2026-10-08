package com.xvox.music.core.ui.chrome

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Per-surface chrome knobs from Settings (Appearance). Everything is stored as one encoded
 * preference so the whole set travels through [SettingsState] in a single field and is offered
 * to every composable through [LocalXvoxChromeStyle].
 *
 * Border colour fields are hex like "#7A5CFF" or empty when the user wants the theme's own
 * border colour; each border/alpha pair lets them tune fills and outlines separately.
 */
data class XvoxChromeStyle(
    // Option boxes (every options popup opened over the app).
    val optionBoxBgAlpha: Float = 1f,
    val optionBoxBorder: String = "",
    val optionBoxBorderAlpha: Float = 1f,
    // Home / top header strip.
    val headerBgAlpha: Float = 1f,
    val headerBorder: String = "",
    val headerBorderAlpha: Float = 0f,
    // Mini player bar.
    val miniBgAlpha: Float = .94f,
    val miniBorder: String = "",
    val miniBorderAlpha: Float = 0.62f,
    // Floating navigation bar.
    val navBgAlpha: Float = .94f,
    val navBorder: String = "",
    val navBorderAlpha: Float = 0.62f,
    // Navigation selector "pill".
    val pillColor: String = "",
    val pillAlpha: Float = 1f,
    // Colour of the icon sitting inside the pill ("" = the theme accent).
    val pillIconColor: String = "",
    // Cards everywhere (border only; the card fill transparency lives in Theme's card alpha).
    val cardBorder: String = "",
    val cardBorderAlpha: Float = 1f,
    // Mini player cover mode ("default" or "full").
    val miniCoverStyle: String = "default",
    /** Rounded corner radius for the Mini Player only, in dp. */
    val miniCornerRadius: Float = 15f,
    /** Visual floating-navigation height, in dp. */
    val navigationBarHeight: Float = 62f,
    /** Visual floating-navigation width, in dp. The three fixed destinations share it evenly. */
    val navigationBarWidth: Float = 246f,
    /**
     * Image used exclusively inside the floating navigation bar.  This intentionally never falls
     * back to the Header image: Header/status-bar artwork and navigation artwork are separate
     * choices.
     */
    val navigationImageUri: String = "",
    /** A dim overlay is opt-in so a custom header remains bright by default. */
    val headerDimEnabled: Boolean = false,
    val headerDimAmount: Float = .50f,
    /** Per-surface manual placement offsets in dp; positive X = right and positive Y = down. */
    val miniPlayerOffsetX: Float = 0f,
    val miniPlayerOffsetY: Float = 0f,
    /** Legacy encoded slot. Keyboard-open placement is now the fixed product offset of −12 dp. */
    val miniPlayerImeOffsetY: Float = -12f,
    val navigationBarOffsetX: Float = 0f,
    val navigationBarOffsetY: Float = 0f,
    /** Fixed Mini Player → Now Playing handoff timing, in milliseconds. */
    val miniPlayerTransitionDuration: Int = 300,
    /** Default or the locked non-expanding Scale Mini Player animation. */
    val miniPlayerTransitionStyle: String = "default",
    /** Now Playing seek rail: classic, pill, android_wave, pulse, or aurora. */
    val nowPlayingSeekStyle: String = "classic",
    /** Legacy encoded placement fields retained only to read old preferences; layouts ignore them. */
    val nowPlayingPillSide: String = "left",
    val nowPlayingChangingActionsSide: String = "right",
    val nowPlayingShuffleRepeatSide: String = "left",
    val nowPlayingPlaySide: String = "right",
    val nowPlayingOptionsGroupSide: String = "right"
) {
    fun encode(): String = listOf(
        optionBoxBgAlpha, optionBoxBorder, optionBoxBorderAlpha,
        headerBgAlpha, headerBorder, headerBorderAlpha,
        miniBgAlpha, miniBorder, miniBorderAlpha,
        navBgAlpha, navBorder, navBorderAlpha,
        pillColor, pillAlpha,
        pillIconColor,
        cardBorder, cardBorderAlpha,
        miniCoverStyle,
        miniCornerRadius, navigationBarHeight,
        headerDimEnabled, headerDimAmount,
        navigationBarWidth, navigationImageUri,
        miniPlayerOffsetX, miniPlayerOffsetY,
        navigationBarOffsetX, navigationBarOffsetY,
        // New values are always appended so all existing chrome_style_v1 placements remain exact.
        miniPlayerImeOffsetY,
        miniPlayerTransitionDuration, miniPlayerTransitionStyle,
        nowPlayingSeekStyle,
        nowPlayingPillSide, nowPlayingChangingActionsSide,
        nowPlayingShuffleRepeatSide, nowPlayingPlaySide, nowPlayingOptionsGroupSide
    ).joinToString("|")

    companion object {
        fun decode(raw: String): XvoxChromeStyle {
            val parts = raw.split("|")
            fun str(i: Int): String = parts.getOrNull(i).orEmpty().trim()
            fun flt(i: Int, fallback: Float): Float =
                parts.getOrNull(i)?.trim()?.toFloatOrNull()?.coerceIn(0f, 1f) ?: fallback
            fun number(i: Int, fallback: Float): Float =
                parts.getOrNull(i)?.trim()?.toFloatOrNull() ?: fallback
            fun side(i: Int, fallback: String): String =
                if (str(i) == "left" || str(i) == "right") str(i) else fallback
            fun seekStyle(i: Int): String = when (str(i)) {
                "pill", "android_wave", "pulse", "aurora" -> str(i)
                else -> "classic"
            }
            if (parts.size == 16) {
                return XvoxChromeStyle(
                    optionBoxBgAlpha = flt(0, 1f), optionBoxBorder = str(1), optionBoxBorderAlpha = flt(2, 1f),
                    headerBgAlpha = flt(3, 1f), headerBorder = str(4), headerBorderAlpha = flt(5, 0f),
                    miniBgAlpha = flt(6, .94f), miniBorder = str(7), miniBorderAlpha = flt(8, 0.62f),
                    navBgAlpha = flt(9, .94f), navBorder = str(10), navBorderAlpha = flt(11, 0.62f),
                    pillColor = str(12), pillAlpha = flt(13, 1f),
                    cardBorder = str(14), cardBorderAlpha = flt(15, 1f)
                )
            }
            if (parts.size == 17) {
                return XvoxChromeStyle(
                    optionBoxBgAlpha = flt(0, 1f), optionBoxBorder = str(1), optionBoxBorderAlpha = flt(2, 1f),
                    headerBgAlpha = flt(3, 1f), headerBorder = str(4), headerBorderAlpha = flt(5, 0f),
                    miniBgAlpha = flt(6, .94f), miniBorder = str(7), miniBorderAlpha = flt(8, 0.62f),
                    navBgAlpha = flt(9, .94f), navBorder = str(10), navBorderAlpha = flt(11, 0.62f),
                    pillColor = str(12), pillAlpha = flt(13, 1f),
                    pillIconColor = str(14),
                    cardBorder = str(15), cardBorderAlpha = flt(16, 1f)
                )
            }
            if (parts.size < 18) return XvoxChromeStyle()
            return XvoxChromeStyle(
                optionBoxBgAlpha = flt(0, 1f), optionBoxBorder = str(1), optionBoxBorderAlpha = flt(2, 1f),
                headerBgAlpha = flt(3, 1f), headerBorder = str(4), headerBorderAlpha = flt(5, 0f),
                miniBgAlpha = flt(6, .94f), miniBorder = str(7), miniBorderAlpha = flt(8, 0.62f),
                navBgAlpha = flt(9, .94f), navBorder = str(10), navBorderAlpha = flt(11, 0.62f),
                pillColor = str(12), pillAlpha = flt(13, 1f),
                pillIconColor = str(14),
                cardBorder = str(15), cardBorderAlpha = flt(16, 1f),
                miniCoverStyle = if (str(17).isNotBlank()) str(17) else "default",
                miniCornerRadius = number(18, 15f).coerceIn(6f, 32f),
                navigationBarHeight = number(19, 62f).coerceIn(52f, 88f),
                headerDimEnabled = parts.getOrNull(20)?.toBooleanStrictOrNull() ?: false,
                headerDimAmount = flt(21, .50f),
                // Appended fields keep every earlier chrome_style_v1 record readable.
                navigationBarWidth = number(22, 246f).coerceIn(190f, 380f),
                navigationImageUri = str(23),
                miniPlayerOffsetX = number(24, 0f).coerceIn(-220f, 220f),
                miniPlayerOffsetY = number(25, 0f).coerceIn(-260f, 260f),
                // Kept only to read revision-8 records. The keyboard-open offset is permanently
                // fixed at −12 dp and no longer follows a saved experimental slider value.
                miniPlayerImeOffsetY = -12f,
                navigationBarOffsetX = number(26, 0f).coerceIn(-220f, 220f),
                navigationBarOffsetY = number(27, 0f).coerceIn(-260f, 260f),
                miniPlayerTransitionDuration = 300,
                miniPlayerTransitionStyle = com.xvox.music.core.ui.miniplayer.XvoxPlayerTransitionMotion.normalizedStyle(str(30)),
                nowPlayingSeekStyle = seekStyle(31),
                nowPlayingPillSide = side(32, "left"),
                nowPlayingChangingActionsSide = side(33, "right"),
                nowPlayingShuffleRepeatSide = side(34, "left"),
                nowPlayingPlaySide = side(35, "right"),
                nowPlayingOptionsGroupSide = side(36, "right")
            )
        }
    }
}

/** Effective rendering chrome after an optional app-wide appearance experiment is applied. */
val LocalXvoxChromeStyle = staticCompositionLocalOf { XvoxChromeStyle() }

/** Durable chrome before temporary Blur UI derives its visual-only surface values. */
val LocalXvoxBaseChromeStyle = staticCompositionLocalOf { XvoxChromeStyle() }

/**
 * Frame-immediate chrome preview, analogous to the custom-accent preview. Slider/toggle changes
 * should repaint Mini Player, Navbar, and Header in the same composition frame while DataStore
 * persists the value in the background.
 */
object XvoxChromePreview {
    private val state = mutableStateOf<XvoxChromeStyle?>(null)
    val value: XvoxChromeStyle? get() = state.value

    fun publish(style: XvoxChromeStyle) {
        state.value = style
    }

    fun clearWhenPersisted(style: XvoxChromeStyle) {
        if (state.value == style) state.value = null
    }
}

/** A non-persistent Profile-editor presentation reflected by the real app Header immediately. */
data class XvoxHeaderPresentationPreview(
    val imageUri: String?,
    val dimEnabled: Boolean,
    val dimAmount: Float
)

object XvoxHeaderPreview {
    private val state = mutableStateOf<XvoxHeaderPresentationPreview?>(null)
    val value: XvoxHeaderPresentationPreview? get() = state.value

    fun publish(preview: XvoxHeaderPresentationPreview) {
        state.value = preview
    }

    fun clear() {
        state.value = null
    }

    fun clearWhenPersisted(
        imageUri: String?,
        dimEnabled: Boolean,
        dimAmount: Float
    ) {
        val current = state.value ?: return
        if (
            current.imageUri == imageUri &&
            current.dimEnabled == dimEnabled &&
            kotlin.math.abs(current.dimAmount - dimAmount) < .001f
        ) {
            state.value = null
        }
    }
}

/** Parses "#RRGGBB" (or short "#RGB") into a colour; null when blank/invalid. */
fun parseHexColor(hex: String): Color? {
    var value = hex.trim().removePrefix("#")
    if (value.length == 3) value = value.map { "$it$it" }.joinToString("")
    if (value.length != 6) return null
    val rgb = value.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or rgb)
}
