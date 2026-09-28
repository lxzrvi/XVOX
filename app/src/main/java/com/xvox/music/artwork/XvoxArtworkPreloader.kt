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
    suspend fun warm(songs: List<Song>, fromIndex: Int, count: Int) = withContext(Dispatchers.IO) {
        val start = fromIndex.coerceIn(0, songs.size)
        val end = (start + count.coerceIn(0, 12)).coerceAtMost(songs.size)
        if (start >= end) return@withContext
        val loader = SingletonImageLoader.get(app)
        val uris = songs.subList(start, end).mapNotNull { it.artworkUri }.distinct()
        for (uri in uris) {
            coroutineContext.ensureActive()
            // All Songs cards render through the size-qualified 256px cache key. Warming 160px
            // made this work invisible to the grid and caused decode stalls without improving
            // quality. Prefetch exactly the display quality that the cards consume.
            val key = "${XvoxArtworkCache.keyFor(uri)}_$XvoxGridArtworkSize"
            if (XvoxArtworkCache.get(key) != null) continue
            try {
                val result = loader.execute(ImageRequest.Builder(app).data(uri)
                    .size(XvoxGridArtworkSize, XvoxGridArtworkSize)
                    .precision(Precision.EXACT).memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED).networkCachePolicy(CachePolicy.DISABLED).build())
                (result.image as? BitmapImage)?.bitmap?.let { XvoxArtworkCache.put(key, it) }
            } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { /* A missing cover must not delay the next visible card. */ }
        }
    }
    suspend fun warmVisible(songs: List<Song>) { warm(songs, 0, 8) }
}
