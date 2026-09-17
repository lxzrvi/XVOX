package com.xvox.music.features.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.xvox.music.artwork.XvoxArtworkCache
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

const val XvoxGridArtworkSize = 256
const val XvoxRecentArtworkSize = 512
const val XvoxNowPlayingArtworkSize = 1024

@Composable
fun XvoxSongArtwork(
    artwork: Any?,
    modifier: Modifier = Modifier,
    requestSize: Int = XvoxGridArtworkSize,
    contentScale: ContentScale = ContentScale.Crop
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

    // Find best available cached bitmap matching requested resolution
    val cachedBitmap = remember(cacheKey, baseKey) {
        if (requestSize >= 1024) {
            XvoxArtworkCache.get(cacheKey)
                ?: XvoxArtworkCache.get("${baseKey}_1024")
        } else if (requestSize >= 512) {
            XvoxArtworkCache.get(cacheKey)
                ?: XvoxArtworkCache.get("${baseKey}_1024")
                ?: XvoxArtworkCache.get("${baseKey}_512")
        } else {
            XvoxArtworkCache.get(cacheKey)
                ?: XvoxArtworkCache.get("${baseKey}_1024")
                ?: XvoxArtworkCache.get("${baseKey}_512")
                ?: XvoxArtworkCache.get("${baseKey}_256")
                ?: XvoxArtworkCache.get(baseKey)
        }
    }

    if (cachedBitmap != null) {
        Image(
            bitmap = cachedBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
        return
    }

    val request = remember(artwork, requestSize) {
        ImageRequest.Builder(context)
            .data(artwork)
            .size(requestSize, requestSize)
            .precision(Precision.EXACT)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale,
        onSuccess = { successResult ->
            val drawable = successResult.result.image
            if (drawable is coil3.BitmapImage) {
                XvoxArtworkCache.put(cacheKey, drawable.bitmap)
                XvoxArtworkCache.put("${baseKey}_1024", drawable.bitmap)
            }
        },
        modifier = modifier.background(colors.cardElevated)
    )
}

@Composable
fun XvoxSongArtwork(
    song: com.xvox.music.core.model.Song?,
    modifier: Modifier = Modifier,
    requestSize: Int = XvoxGridArtworkSize,
    contentScale: ContentScale = ContentScale.Crop
) {
    XvoxSongArtwork(
        artwork = song?.artworkUri,
        modifier = modifier,
        requestSize = requestSize,
        contentScale = contentScale
    )
}
