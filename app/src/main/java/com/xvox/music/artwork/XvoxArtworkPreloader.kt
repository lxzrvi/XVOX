package com.xvox.music.artwork

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.xvox.music.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XvoxArtworkPreloader(
    context: Context
) {
    private val appContext =
        context.applicationContext

    suspend fun warm(
        songs: List<Song>,
        fromIndex: Int,
        count: Int
    ) = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) {
            return@withContext
        }

        val start =
            fromIndex.coerceIn(
                0,
                songs.size
            )

        val end =
            (
                start +
                    count.coerceAtMost(
                        24
                    )
                )
                .coerceAtMost(
                    songs.size
                )

        if (start >= end) {
            return@withContext
        }

        val loader =
            SingletonImageLoader.get(
                appContext
            )

        songs.subList(
            start,
            end
        )
            .asSequence()
            .mapNotNull {
                it.artworkUri
            }
            .distinct()
            .forEach {
                uri ->

                val key = XvoxArtworkCache.keyFor(uri)

                if (XvoxArtworkCache.get(key) != null) {
                    return@forEach
                }

                val request =
                    ImageRequest
                        .Builder(
                            appContext
                        )
                        .data(uri)
                        .size(
                            160,
                            160
                        )
                        .precision(
                            Precision.INEXACT
                        )
                        .memoryCachePolicy(
                            CachePolicy.ENABLED
                        )
                        .diskCachePolicy(
                            CachePolicy.ENABLED
                        )
                        .networkCachePolicy(
                            CachePolicy.DISABLED
                        )
                        .build()

                val isVisible = songs.indexOfFirst { it.artworkUri == uri } in start until (start + 6)

                runCatching {
                    if (isVisible) {
                        val result = loader.execute(request)
                        result.image?.let { image ->
                        }
                    } else {
                        loader.enqueue(request)
                    }
                }
            }
    }

    suspend fun warmVisible(songs: List<Song>) {
        warm(songs, 0, 12)
    }
}
