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
    /** User-selectable Now Playing lanes. Play itself remains physically centered by design. */
    val nowPlayingPillSide: String = "left",
    val nowPlayingChangingActionsSide: String = "right",
    val nowPlayingShuffleRepeatSide: String = "left",
    /** Retained for old records; the Play control deliberately ignores side placement. */
    val nowPlayingPlaySide: String = "right",
    val nowPlayingOptionsGroupSide: String = "right",
    /** Independent transparencies exposed by Now Playing → Customize. */
    val nowPlayingBottomBoxAlpha: Float = 1f,
    val nowPlayingControlsAlpha: Float = 1f,
    val nowPlayingPlayAlpha: Float = 1f,
    /** Persisted alignment-grid offsets in dp for the bottom Now Playing canvas. */
    val nowPlayingMetadataOffsetX: Float = 0f,
    val nowPlayingMetadataOffsetY: Float = 0f,
    val nowPlayingProgressOffsetX: Float = 0f,
    val nowPlayingProgressOffsetY: Float = 0f,
    val nowPlayingUtilityOffsetX: Float = 0f,
    val nowPlayingUtilityOffsetY: Float = 0f,
    val nowPlayingActionsOffsetX: Float = 0f,
    val nowPlayingActionsOffsetY: Float = 0f,
    val nowPlayingShuffleRepeatOffsetX: Float = 0f,
    val nowPlayingShuffleRepeatOffsetY: Float = 0f,
    val nowPlayingPreviousOffsetX: Float = 0f,
    val nowPlayingPreviousOffsetY: Float = 0f,
    val nowPlayingPlayOffsetX: Float = 0f,
    val nowPlayingPlayOffsetY: Float = 0f,
    val nowPlayingNextOffsetX: Float = 0f,
    val nowPlayingNextOffsetY: Float = 0f,
    val nowPlayingBrandOffsetX: Float = 0f,
    val nowPlayingBrandOffsetY: Float = 0f,
    /** Draftable in-place editor state persisted only after its Okay action. */
    val nowPlayingHiddenItems: String = "",
    /** Comma-separated token=scale entries; compact encoding keeps old chrome records readable. */
    val nowPlayingItemScales: String = "",
    /** Cover may resize in Customize but is intentionally never movable or hideable. */
    val nowPlayingCoverScale: Float = 1f,
    /** Individual outer transport positions supersede the legacy shared outer offset. */
    val nowPlayingShuffleOffsetX: Float = 0f,
    val nowPlayingShuffleOffsetY: Float = 0f,
    val nowPlayingRepeatOffsetX: Float = 0f,
    val nowPlayingRepeatOffsetY: Float = 0f
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
        nowPlayingShuffleRepeatSide, nowPlayingPlaySide, nowPlayingOptionsGroupSide,
        // Appended freeform Now Playing editor fields retain every existing record unchanged.
        nowPlayingBottomBoxAlpha, nowPlayingControlsAlpha, nowPlayingPlayAlpha,
        nowPlayingMetadataOffsetX, nowPlayingMetadataOffsetY,
        nowPlayingProgressOffsetX, nowPlayingProgressOffsetY,
        nowPlayingUtilityOffsetX, nowPlayingUtilityOffsetY,
        nowPlayingActionsOffsetX, nowPlayingActionsOffsetY,
        nowPlayingShuffleRepeatOffsetX, nowPlayingShuffleRepeatOffsetY,
        nowPlayingPreviousOffsetX, nowPlayingPreviousOffsetY,
        nowPlayingPlayOffsetX, nowPlayingPlayOffsetY,
        nowPlayingNextOffsetX, nowPlayingNextOffsetY,
        nowPlayingBrandOffsetX, nowPlayingBrandOffsetY,
        nowPlayingHiddenItems, nowPlayingItemScales, nowPlayingCoverScale,
        nowPlayingShuffleOffsetX, nowPlayingShuffleOffsetY,
        nowPlayingRepeatOffsetX, nowPlayingRepeatOffsetY
    ).joinToString("|")

    fun resetNowPlayingLayout(): XvoxChromeStyle = copy(
        nowPlayingMetadataOffsetX = 0f,
        nowPlayingMetadataOffsetY = 0f,
        nowPlayingProgressOffsetX = 0f,
        nowPlayingProgressOffsetY = 0f,
        nowPlayingUtilityOffsetX = 0f,
        nowPlayingUtilityOffsetY = 0f,
        nowPlayingActionsOffsetX = 0f,
        nowPlayingActionsOffsetY = 0f,
        nowPlayingShuffleRepeatOffsetX = 0f,
        nowPlayingShuffleRepeatOffsetY = 0f,
        nowPlayingPreviousOffsetX = 0f,
        nowPlayingPreviousOffsetY = 0f,
        nowPlayingPlayOffsetX = 0f,
        nowPlayingPlayOffsetY = 0f,
        nowPlayingNextOffsetX = 0f,
        nowPlayingNextOffsetY = 0f,
        nowPlayingBrandOffsetX = 0f,
        nowPlayingBrandOffsetY = 0f,
        nowPlayingHiddenItems = "",
        nowPlayingItemScales = "",
        nowPlayingCoverScale = 1f,
        nowPlayingShuffleOffsetX = 0f,
        nowPlayingShuffleOffsetY = 0f,
        nowPlayingRepeatOffsetX = 0f,
        nowPlayingRepeatOffsetY = 0f
    )

    companion object {
        fun decode(raw: String): XvoxChromeStyle {
            val parts = raw.split("|")
            fun str(i: Int): String = parts.getOrNull(i).orEmpty().trim()
            fun flt(i: Int, fallback: Float): Float =
                parts.getOrNull(i)?.trim()?.toFloatOrNull()?.coerceIn(0f, 1f) ?: fallback
            fun number(i: Int, fallback: Float): Float =
                parts.getOrNull(i)?.trim()?.toFloatOrNull() ?: fallback
            // The full bottom-box editor uses wide, discrete safe slots (the top clusters can
            // legally exchange opposite sides), so persisted offsets need more than the former
            // tiny free-nudge range while remaining bounded against corrupt records.
            fun offsetX(i: Int): Float = number(i, 0f).coerceIn(-300f, 300f)
            fun offsetY(i: Int): Float = number(i, 0f).coerceIn(-300f, 300f)
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
                nowPlayingOptionsGroupSide = side(36, "right"),
                nowPlayingBottomBoxAlpha = flt(37, 1f),
                nowPlayingControlsAlpha = flt(38, 1f),
                nowPlayingPlayAlpha = flt(39, 1f),
                nowPlayingMetadataOffsetX = offsetX(40),
                nowPlayingMetadataOffsetY = offsetY(41),
                nowPlayingProgressOffsetX = offsetX(42),
                nowPlayingProgressOffsetY = offsetY(43),
                nowPlayingUtilityOffsetX = offsetX(44),
                nowPlayingUtilityOffsetY = offsetY(45),
                nowPlayingActionsOffsetX = offsetX(46),
                nowPlayingActionsOffsetY = offsetY(47),
                nowPlayingShuffleRepeatOffsetX = offsetX(48),
                nowPlayingShuffleRepeatOffsetY = offsetY(49),
                nowPlayingPreviousOffsetX = offsetX(50),
                nowPlayingPreviousOffsetY = offsetY(51),
                nowPlayingPlayOffsetX = offsetX(52),
                nowPlayingPlayOffsetY = offsetY(53),
                nowPlayingNextOffsetX = offsetX(54),
                nowPlayingNextOffsetY = offsetY(55),
                nowPlayingBrandOffsetX = offsetX(56),
                nowPlayingBrandOffsetY = offsetY(57),
                nowPlayingHiddenItems = str(58),
                nowPlayingItemScales = str(59),
                nowPlayingCoverScale = number(60, 1f).coerceIn(.65f, 1.55f),
                // Records before individual outer transport editing retain their shared offset.
                nowPlayingShuffleOffsetX = number(61, offsetX(48)).coerceIn(-300f, 300f),
                nowPlayingShuffleOffsetY = number(62, offsetY(49)).coerceIn(-300f, 300f),
                nowPlayingRepeatOffsetX = number(63, offsetX(48)).coerceIn(-300f, 300f),
                nowPlayingRepeatOffsetY = number(64, offsetY(49)).coerceIn(-300f, 300f)
            )
        }
    }
}

/** IDs shared by the live Now Playing editor and its durable chrome record. */
val XvoxNowPlayingEditableItemIds = setOf(
    "utility", "actions", "metadata", "progress", "outer", "shuffle", "repeat", "previous", "play", "next", "brand"
)

private fun XvoxChromeStyle.normalizedHiddenItems(): Set<String> {
    val values = nowPlayingHiddenItems.split(',').map(String::trim)
        .filter { it in XvoxNowPlayingEditableItemIds }.toMutableSet()
    // Migrate the brief shared-outer implementation safely if a draft was persisted from it.
    if ("outer" in values) {
        values += "shuffle"
        values += "repeat"
        values -= "outer"
    }
    return values
}

private fun XvoxChromeStyle.normalizedItemScales(): Map<String, Float> {
    val values = nowPlayingItemScales.split(',').mapNotNull { encoded ->
        val key = encoded.substringBefore('=').trim()
        val scale = encoded.substringAfter('=', "").toFloatOrNull()?.coerceIn(.65f, 1.55f)
        key.takeIf { it in XvoxNowPlayingEditableItemIds }?.let { valid -> scale?.let { valid to it } }
    }.toMap().toMutableMap()
    values.remove("outer")?.let { legacyScale ->
        values.putIfAbsent("shuffle", legacyScale)
        values.putIfAbsent("repeat", legacyScale)
    }
    return values
}

fun XvoxChromeStyle.isNowPlayingItemHidden(itemId: String): Boolean = itemId in normalizedHiddenItems()

fun XvoxChromeStyle.nowPlayingItemScale(itemId: String): Float =
    normalizedItemScales()[itemId] ?: 1f

fun XvoxChromeStyle.withNowPlayingItemHidden(itemId: String, hidden: Boolean): XvoxChromeStyle {
    if (itemId !in XvoxNowPlayingEditableItemIds) return this
    val next = normalizedHiddenItems().toMutableSet()
    if (hidden) next += itemId else next -= itemId
    return copy(nowPlayingHiddenItems = next.sorted().joinToString(","))
}

fun XvoxChromeStyle.withNowPlayingItemScale(itemId: String, scale: Float): XvoxChromeStyle {
    if (itemId !in XvoxNowPlayingEditableItemIds) return this
    val next = normalizedItemScales().toMutableMap()
    val normalized = scale.coerceIn(.65f, 1.55f)
    if (kotlin.math.abs(normalized - 1f) < .01f) next.remove(itemId) else next[itemId] = normalized
    return copy(nowPlayingItemScales = next.toSortedMap().entries.joinToString(",") { (key, value) -> "$key=$value" })
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

/**
 * Rendering-side normalization for the in-place full-panel editor. Its invisible 16dp alignment
 * lattice gives long-press drops clean shared rows/columns without ever drawing grid lines; broad
 * bounds retain positions anywhere in the actual bottom box.
 */
private fun xvoxSafePanelGrid(value: Float, lower: Float, upper: Float): Float =
    (kotlin.math.round(value / 16f) * 16f).coerceIn(lower, upper)

fun XvoxChromeStyle.normalizedNowPlayingGrid(): XvoxChromeStyle = copy(
    nowPlayingMetadataOffsetX = xvoxSafePanelGrid(nowPlayingMetadataOffsetX, -320f, 320f),
    nowPlayingMetadataOffsetY = xvoxSafePanelGrid(nowPlayingMetadataOffsetY, -320f, 320f),
    nowPlayingProgressOffsetX = xvoxSafePanelGrid(nowPlayingProgressOffsetX, -320f, 320f),
    nowPlayingProgressOffsetY = xvoxSafePanelGrid(nowPlayingProgressOffsetY, -320f, 320f),
    nowPlayingUtilityOffsetX = xvoxSafePanelGrid(nowPlayingUtilityOffsetX, -320f, 320f),
    nowPlayingUtilityOffsetY = xvoxSafePanelGrid(nowPlayingUtilityOffsetY, -320f, 320f),
    nowPlayingActionsOffsetX = xvoxSafePanelGrid(nowPlayingActionsOffsetX, -320f, 320f),
    nowPlayingActionsOffsetY = xvoxSafePanelGrid(nowPlayingActionsOffsetY, -320f, 320f),
    nowPlayingShuffleRepeatOffsetX = xvoxSafePanelGrid(nowPlayingShuffleRepeatOffsetX, -320f, 320f),
    nowPlayingShuffleRepeatOffsetY = xvoxSafePanelGrid(nowPlayingShuffleRepeatOffsetY, -320f, 320f),
    nowPlayingShuffleOffsetX = xvoxSafePanelGrid(nowPlayingShuffleOffsetX, -320f, 320f),
    nowPlayingShuffleOffsetY = xvoxSafePanelGrid(nowPlayingShuffleOffsetY, -320f, 320f),
    nowPlayingRepeatOffsetX = xvoxSafePanelGrid(nowPlayingRepeatOffsetX, -320f, 320f),
    nowPlayingRepeatOffsetY = xvoxSafePanelGrid(nowPlayingRepeatOffsetY, -320f, 320f),
    nowPlayingPreviousOffsetX = xvoxSafePanelGrid(nowPlayingPreviousOffsetX, -320f, 320f),
    nowPlayingPreviousOffsetY = xvoxSafePanelGrid(nowPlayingPreviousOffsetY, -320f, 320f),
    nowPlayingPlayOffsetX = xvoxSafePanelGrid(nowPlayingPlayOffsetX, -320f, 320f),
    nowPlayingPlayOffsetY = xvoxSafePanelGrid(nowPlayingPlayOffsetY, -320f, 320f),
    nowPlayingNextOffsetX = xvoxSafePanelGrid(nowPlayingNextOffsetX, -320f, 320f),
    nowPlayingNextOffsetY = xvoxSafePanelGrid(nowPlayingNextOffsetY, -320f, 320f),
    nowPlayingBrandOffsetX = xvoxSafePanelGrid(nowPlayingBrandOffsetX, -320f, 320f),
    nowPlayingBrandOffsetY = xvoxSafePanelGrid(nowPlayingBrandOffsetY, -320f, 320f),
    nowPlayingHiddenItems = normalizedHiddenItems().sorted().joinToString(","),
    nowPlayingItemScales = normalizedItemScales().toSortedMap().entries.joinToString(",") { (key, value) -> "$key=$value" },
    nowPlayingCoverScale = nowPlayingCoverScale.coerceIn(.65f, 1.55f)
)
