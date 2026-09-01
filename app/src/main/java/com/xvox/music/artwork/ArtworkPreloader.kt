package com.xvox.music.artwork

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.GridArtworkSize
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ArtworkPreloader(
    context: Context
) {

    private val appContext =
        context.applicationContext

    suspend fun warm(
        songs: List<Song>,
        fromIndex: Int,
        count: Int
    ) {
        if (songs.isEmpty()) return

        val start =
            fromIndex.coerceIn(
                0,
                songs.size
            )

        val end =
            (start + count)
                .coerceAtMost(
                    songs.size
                )

        if (start >= end) return

        val artworkUris =
            songs
                .subList(start, end)
                .asSequence()
                .mapNotNull {
                    it.artworkUri
                }
                .distinct()
                .toList()

        val loader =
            SingletonImageLoader.get(
                appContext
            )

        artworkUris
            .chunked(3)
            .forEach { batch ->
                coroutineScope {
                    batch.map { uri ->
                        async {
                            val request =
                                ImageRequest
                                    .Builder(
                                        appContext
                                    )
                                    .data(uri)
                                    .size(
                                        GridArtworkSize,
                                        GridArtworkSize
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

                            runCatching {
                                loader.execute(
                                    request
                                )
                            }
                        }
                    }.awaitAll()
                }
            }
    }
}
