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

    suspend fun warmInitialCache(
        songs: List<Song>
    ) {
        val loader =
            SingletonImageLoader.get(
                appContext
            )

        val artwork =
            songs
                .asSequence()
                .mapNotNull {
                    it.artworkUri
                }
                .distinct()
                .take(48)
                .toList()

        coroutineScope {
            artwork
                .chunked(4)
                .forEach { batch ->

                    batch.map { uri ->
                        async {
                            val request =
                                ImageRequest.Builder(
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
