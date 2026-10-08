package com.xvox.music.artwork

import android.content.Context
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxGridArtworkSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
        val exactSize = requestSize.coerceAtLeast(XvoxGridArtworkSize)
        // Standard 256px pages can warm a complete landscape page; 512px mosaic tiles retain a
        // tighter queue to protect heap headroom without ever asking Coil for a smaller decode.
        val maxCount = if (exactSize >= 512) 16 else 32
        val end = (start + count.coerceIn(0, maxCount)).coerceAtMost(songs.size)
        if (start >= end) return@withContext
        val loader = SingletonImageLoader.get(app)
        val uris = songs.subList(start, end).mapNotNull { it.artworkUri }.distinct()
        for (uri in uris) {
            coroutineContext.ensureActive()
            // Warm the exact size that the forthcoming card asks Coil to decode. In particular,
            // non-unit mosaic tiles retain their 512px request instead of silently falling back
            // to a smaller preview during a fast scroll.
            val key = "${XvoxArtworkCache.keyFor(uri)}_$exactSize"
            if (XvoxArtworkCache.get(key) != null) continue
            try {
                val result = loader.execute(ImageRequest.Builder(app).data(uri)
                    .size(exactSize, exactSize)
                    .precision(Precision.EXACT).memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED).networkCachePolicy(CachePolicy.DISABLED).build())
                (result.image as? BitmapImage)?.bitmap?.let { XvoxArtworkCache.put(key, it) }
            } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { /* A missing cover must not delay the next visible card. */ }
        }
    }
    suspend fun warmVisible(songs: List<Song>) { warm(songs, 0, 16, XvoxGridArtworkSize) }
}
