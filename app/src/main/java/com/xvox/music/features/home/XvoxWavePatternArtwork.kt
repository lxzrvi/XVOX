package com.xvox.music.features.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.xvox.music.R
import com.xvox.music.artwork.XvoxArtworkCache
import com.xvox.music.core.design.theme.XvoxPalette
import com.xvox.music.core.design.theme.XvoxTheme

/** Fallback identity is centered everywhere; NONE is reserved for a deliberately blurred backdrop. */
enum class XvoxFallbackBrand {
    STANDARD,
    MINI_PLAYER,
    NOW_PLAYING,
    NONE
}

/**
 * Compatibility palette for the Default Now Playing canvas.
 *
 * The supplied fallback itself is intentionally one stable artwork, not a synthetic per-song wave.
 * Its canvas therefore follows the active app theme, with the accent retained only as a themed
 * companion value.  This keeps light fallbacks light, dark fallbacks dark, and avoids locking the
 * player to the fresh-install red accent.
 */
data class XvoxWavePalette(
    val surface: Color,
    val stripe: Color,
    val dominant: Color
)

internal fun xvoxWavePalette(@Suppress("UNUSED_PARAMETER") seed: Int, colors: XvoxPalette): XvoxWavePalette {
    val surface = if (colors.isLight) colors.cardElevated else colors.background
    return XvoxWavePalette(
        surface = surface,
        stripe = colors.primaryAccent,
        dominant = surface
    )
}

/**
 * Decodes the exact bundled fallback once for bitmap-only surfaces such as the local Now Playing
 * blur. Dark mode returns the supplied dark artwork and light mode returns the separately supplied
 * light artwork. Neither asset is recoloured or inverted at runtime.
 */
internal fun xvoxDefaultCoverBitmap(context: Context, light: Boolean): Bitmap {
    val key = "xvox_default_cover_v2:${if (light) "light" else "dark"}"
    XvoxArtworkCache.get(key)?.let { return it }

    val decoded = checkNotNull(
        BitmapFactory.decodeResource(
            context.resources,
            if (light) R.drawable.xvox_default_2 else R.drawable.xvox_default,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        )
    ) { "Bundled XVOX default cover is unavailable" }
    XvoxArtworkCache.put(key, decoded)
    return XvoxArtworkCache.get(key) ?: decoded
}

/**
 * Shared no-cover renderer for songs, playlists, Mini Player, Now Playing, and every Compose
 * failure state. Dark mode draws the supplied dark PNG and light mode draws the separately supplied
 * light PNG—there is no runtime inversion, added logo, accent tint, pattern, or branding overlay.
 */
@Composable
fun WavePatternSquare(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") darkColor: Color? = null,
    @Suppress("UNUSED_PARAMETER") brightColor: Color? = null,
    @Suppress("UNUSED_PARAMETER") seed: Int = 11,
    @Suppress("UNUSED_PARAMETER") bitmapSize: Int = 256,
    @Suppress("UNUSED_PARAMETER") brand: XvoxFallbackBrand = XvoxFallbackBrand.STANDARD
) {
    val colors = XvoxTheme.colors
    Image(
        painter = painterResource(if (colors.isLight) R.drawable.xvox_default_2 else R.drawable.xvox_default),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
