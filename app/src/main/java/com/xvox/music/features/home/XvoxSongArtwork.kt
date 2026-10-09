package com.xvox.music.features.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.xvox.music.artwork.XvoxArtworkCache
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

const val XvoxGridArtworkSize = 256
const val XvoxRecentArtworkSize = 512
const val XvoxNowPlayingArtworkSize = 0

/**
 * Shared artwork renderer.
 *
 * Normal library cards still use the requested resolution unchanged. Full-player callers can ask
 * to retain their last decoded bitmap while Coil resolves the next cover, avoiding a card-colour
 * flash when playback advances or an adjacent queue row changes.
 */
@Composable
fun XvoxSongArtwork(
    artwork: Any?,
    modifier: Modifier = Modifier,
    requestSize: Int = XvoxGridArtworkSize,
    contentScale: ContentScale = ContentScale.Crop,
    keepPreviousOnLoading: Boolean = false
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current

    if (artwork == null) {
        Box(
            modifier = modifier.background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "X",
                color = colors.mutedText,
                fontFamily = XvoxLogoFont,
                fontSize = 20.sp
            )
        }
        return
    }

    val baseKey = remember(artwork) { XvoxArtworkCache.keyFor(artwork) }
    val cacheKey = remember(baseKey, requestSize) { "${baseKey}_$requestSize" }

    fun cachedBitmap(): Bitmap? = when {
        requestSize == 0 -> XvoxArtworkCache.get("${baseKey}_0")
        requestSize >= 1024 -> {
            XvoxArtworkCache.get(cacheKey)
                ?: XvoxArtworkCache.get("${baseKey}_1024")
                ?: XvoxArtworkCache.get("${baseKey}_0")
        }
        requestSize >= 512 -> {
            XvoxArtworkCache.get(cacheKey)
                ?: XvoxArtworkCache.get("${baseKey}_512")
                ?: XvoxArtworkCache.get("${baseKey}_1024")
                ?: XvoxArtworkCache.get("${baseKey}_0")
        }
        else -> {
            XvoxArtworkCache.get(cacheKey)
                ?: XvoxArtworkCache.get("${baseKey}_256")
                ?: XvoxArtworkCache.get(baseKey)
                ?: XvoxArtworkCache.get("${baseKey}_512")
                ?: XvoxArtworkCache.get("${baseKey}_0")
        }
    }

    // Keep a Compose-observable result in addition to the LRU cache. LruCache writes themselves
    // do not invalidate composition, so this state lets a freshly decoded cover replace its
    // placeholder immediately without reducing its requested quality.
    var decodedBitmap by remember(cacheKey, baseKey, requestSize) { mutableStateOf(cachedBitmap()) }
    var previousBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(decodedBitmap) {
        decodedBitmap?.let { previousBitmap = it }
    }

    val visibleBitmap = decodedBitmap ?: previousBitmap.takeIf { keepPreviousOnLoading }
    // asImageBitmap() allocations on every parent playback-state recomposition were enough to
    // make a dense library fling feel behind the finger. Keep the exact decoded bitmap/quality;
    // only its lightweight Compose wrapper is memoized.
    val visibleImageBitmap = remember(visibleBitmap) { visibleBitmap?.asImageBitmap() }
    val request = remember(artwork, requestSize) {
        val builder = ImageRequest.Builder(context)
            .data(artwork)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.DISABLED)

        if (requestSize > 0) {
            builder.size(requestSize, requestSize).precision(Precision.EXACT)
        } else {
            builder.size(coil3.size.Size.ORIGINAL).precision(Precision.INEXACT)
        }
        builder.build()
    }

    Box(
        modifier = modifier.background(colors.cardElevated),
        contentAlignment = Alignment.Center
    ) {
        visibleImageBitmap?.let { imageBitmap ->
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Do not draw a solid loading layer above a retained full-player cover. Coil paints the
        // newly decoded original-resolution image transparently over it when it is ready.
        if (decodedBitmap == null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = contentScale,
                onSuccess = { success ->
                    (success.result.image as? BitmapImage)?.bitmap?.let { bitmap ->
                        XvoxArtworkCache.put(cacheKey, bitmap)
                        if (requestSize == 0) XvoxArtworkCache.put("${baseKey}_0", bitmap)
                        decodedBitmap = bitmap
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun XvoxSongArtwork(
    song: com.xvox.music.core.model.Song?,
    modifier: Modifier = Modifier,
    requestSize: Int = XvoxGridArtworkSize,
    contentScale: ContentScale = ContentScale.Crop,
    keepPreviousOnLoading: Boolean = false
) {
    XvoxSongArtwork(
        artwork = song?.artworkUri,
        modifier = modifier,
        requestSize = requestSize,
        contentScale = contentScale,
        keepPreviousOnLoading = keepPreviousOnLoading
    )
}
