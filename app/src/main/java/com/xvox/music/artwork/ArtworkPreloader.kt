package com.xvox.music.artwork

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.xvox.music.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class ArtworkPreloader(
    context: Context
) {

    private val appContext =
        context.applicationContext

    suspend fun warmInitialCache(
        songs: List<Song>
    ) {
        val artwork =
            songs
                .asSequence()
                .mapNotNull {
                    it.artworkUri
                }
                .distinct()
                .take(36)
                .toList()

        withContext(
            Dispatchers.IO
        ) {
            coroutineScope {
                artwork
                    .chunked(4)
                    .forEach { batch ->
                        batch
                            .map { uri ->
                                async {
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
                                            .allowHardware(
                                                true
                                            )
                                            .build()

                                    runCatching {
                                        SingletonImageLoader
                                            .get(
                                                appContext
                                            )
                                            .execute(
                                                request
                                            )
                                    }
                                }
                            }
                            .awaitAll()
                    }
            }
        }
    }
}
