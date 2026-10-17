package com.xvox.music.artwork

import android.content.Context
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxGridArtworkSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class XvoxArtworkPreloader(context: Context) {
    private val app = context.applicationContext
    suspend fun warm(
        songs: List<Song>,
        fromIndex: Int,
        count: Int,
        requestSize: Int = XvoxGridArtworkSize
    ) = withContext(Dispatchers.IO) {
        val start = fromIndex.coerceIn(0, songs.size)
        val originalQuality = requestSize == 0
        val exactSize = if (originalQuality) 0 else requestSize.coerceAtLeast(XvoxGridArtworkSize)
        // Full-player originals warm only the immediate three-cover window. Home prefetches only
        // its nearest visual runway so decoders never compete with a live fling; no path silently
        // substitutes a smaller requested decode for a visible cover.
        val maxCount = when {
            originalQuality -> 3
            exactSize >= 512 -> 8
            else -> 16
        }
        val end = (start + count.coerceIn(0, maxCount)).coerceAtMost(songs.size)
        if (start >= end) return@withContext
        val loader = SingletonImageLoader.get(app)
        val uris = songs.subList(start, end).mapNotNull { it.artworkUri }.distinct()

        suspend fun warmUri(uri: Any) {
            coroutineContext.ensureActive()
            // Warm the exact size that the forthcoming card asks Coil to decode. In particular,
            // non-unit mosaic tiles retain their 512px request and Now Playing retains original
            // artwork rather than falling back to a thumbnail during a fast gesture.
            val key = "${XvoxArtworkCache.keyFor(uri)}_$exactSize"
            if (XvoxArtworkCache.get(key) != null) return
            try {
                val builder = ImageRequest.Builder(app)
                    .data(uri)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.DISABLED)
                if (originalQuality) {
                    builder.size(Size.ORIGINAL).precision(Precision.INEXACT)
                } else {
                    builder.size(exactSize, exactSize).precision(Precision.EXACT)
                }
                val result = loader.execute(builder.build())
                (result.image as? BitmapImage)?.bitmap?.let { bitmap ->
                    XvoxArtworkCache.markSuccessful(uri)
                    XvoxArtworkCache.put(key, bitmap)
                } ?: XvoxArtworkCache.markFailed(uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A missing cover must not delay the next visible card. Record it so every later
                // consumer immediately uses the supplied XVOX fallback rather than a blank URI.
                XvoxArtworkCache.markFailed(uri)
            }
        }

        if (originalQuality) {
            // The Now Playing pager has only its immediate three-cover runway. Decode that small
            // window concurrently so the first left or right swipe does not wait behind the other
            // adjacent cover and flash a temporary fallback before its actual artwork arrives.
            coroutineScope { uris.map { uri -> async { warmUri(uri) } }.awaitAll() }
        } else {
            for (uri in uris) warmUri(uri)
        }
    }
    // Keep first paint light: visible cards still request their full 256px artwork on demand, while
    // only one compact runway warms after library load so startup decoding cannot compete with a
    // fast All Songs scroll.
    suspend fun warmVisible(songs: List<Song>) { warm(songs, 0, 8, XvoxGridArtworkSize) }
}
