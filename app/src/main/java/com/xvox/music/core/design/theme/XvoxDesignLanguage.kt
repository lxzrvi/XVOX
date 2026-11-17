// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/design/theme/XvoxDesignLanguage.kt
package com.xvox.music.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** App-wide design language. The keys match the Now Playing layout keys (xvox | material | ios). */
enum class XvoxDesignLanguage(val key: String, val label: String) {
    XVOX("xvox", "XVOX"),
    MATERIAL("material", "Material"),
    IOS("ios", "iOS");

    companion object {
        fun from(key: String?): XvoxDesignLanguage = entries.firstOrNull { it.key == key } ?: XVOX
    }
}

/**
 * Shape/spacing/typography tokens that shared components read, so one setting restyles the app.
 * Material 3: tonal 28dp containers, sentence-case labels, no hairlines.
 * iOS: inset-grouped lists, 34sp large title, glyph tiles, hairline separators.
 */
@Immutable
class XvoxDesignTokens(
    val language: XvoxDesignLanguage,
    val groupRadius: Dp,
    val groupInset: Dp,
    val rowMinHeight: Dp,
    val rowDividers: Boolean,
    val headerUppercase: Boolean,
    val headerSize: TextUnit,
    val titleSize: TextUnit,
    val titleWeight: FontWeight,
    val searchRadius: Dp,
    val searchHeight: Dp,
    /** Rows get a tinted glyph tile (iOS square tile / Material tonal circle) instead of a bare icon. */
    val glyphTile: Boolean,
    val glyphTileRadius: Dp,
    val glass: Boolean
)

fun xvoxDesignTokens(language: XvoxDesignLanguage): XvoxDesignTokens = when (language) {
    XvoxDesignLanguage.XVOX -> XvoxDesignTokens(
        language, groupRadius = 18.dp, groupInset = 14.dp, rowMinHeight = 54.dp, rowDividers = true,
        headerUppercase = true, headerSize = 11.sp, titleSize = 30.sp, titleWeight = FontWeight.Bold,
        searchRadius = 16.dp, searchHeight = 46.dp, glyphTile = false, glyphTileRadius = 0.dp, glass = true
    )
    XvoxDesignLanguage.MATERIAL -> XvoxDesignTokens(
        language, groupRadius = 28.dp, groupInset = 16.dp, rowMinHeight = 64.dp, rowDividers = false,
        headerUppercase = false, headerSize = 13.sp, titleSize = 28.sp, titleWeight = FontWeight.Normal,
        searchRadius = 28.dp, searchHeight = 56.dp, glyphTile = true, glyphTileRadius = 20.dp, glass = false
    )
    XvoxDesignLanguage.IOS -> XvoxDesignTokens(
        language, groupRadius = 20.dp, groupInset = 16.dp, rowMinHeight = 52.dp, rowDividers = true,
        headerUppercase = true, headerSize = 12.sp, titleSize = 34.sp, titleWeight = FontWeight.Bold,
        searchRadius = 14.dp, searchHeight = 40.dp, glyphTile = true, glyphTileRadius = 8.dp, glass = true
    )
}

val LocalXvoxDesign = staticCompositionLocalOf { xvoxDesignTokens(XvoxDesignLanguage.XVOX) }
