package com.xvox.music.player.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class XvoxArtworkPalette(
    val primary: Color,
    val secondary: Color
)

class XvoxArtworkPaletteLoader(
    context: Context
) {
    private val appContext =
        context.applicationContext

    private val cache =
        ConcurrentHashMap<String, XvoxArtworkPalette>()

    suspend fun load(
        artworkUri: Uri?
    ): XvoxArtworkPalette {
        if (artworkUri == null) {
            return fallback()
        }

        val key =
            artworkUri.toString()

        cache[key]?.let {
            return it
        }

        return withContext(
            Dispatchers.IO
        ) {
            val palette =
                runCatching {
                    decodeSmall(
                        artworkUri
                    )?.let {
                        bitmap ->

                        try {
                            extract(bitmap)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                    .getOrNull()
                    ?: fallback()

            cache[key] =
                palette

            palette
        }
    }

    private fun decodeSmall(
        uri: Uri
    ): Bitmap? {
        return if (
            Build.VERSION.SDK_INT >= 28
        ) {
            val source =
                ImageDecoder.createSource(
                    appContext.contentResolver,
                    uri
                )

            ImageDecoder.decodeBitmap(
                source
            ) {
                decoder,
                info,
                _ ->

                val width =
                    max(
                        1,
                        info.size.width
                    )

                val height =
                    max(
                        1,
                        info.size.height
                    )

                val sample =
                    max(
                        1,
                        max(
                            width / 48,
                            height / 48
                        )
                    )

                decoder.setTargetSampleSize(
                    sample
                )

                decoder.allocator =
                    ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media
                .getBitmap(
                    appContext.contentResolver,
                    uri
                )
                ?.let {
                    original ->

                    val scaled =
                        Bitmap.createScaledBitmap(
                            original,
                            48,
                            48,
                            true
                        )

                    if (
                        scaled !== original
                    ) {
                        original.recycle()
                    }

                    scaled
                }
        }
    }

    private fun extract(
        bitmap: Bitmap
    ): XvoxArtworkPalette {
        val buckets =
            HashMap<Int, Int>()

        val stepX =
            max(
                1,
                bitmap.width / 24
            )

        val stepY =
            max(
                1,
                bitmap.height / 24
            )

        var y = 0

        while (y < bitmap.height) {
            var x = 0

            while (x < bitmap.width) {
                val pixel =
                    bitmap.getPixel(
                        x,
                        y
                    )

                val alpha =
                    android.graphics.Color
                        .alpha(pixel)

                if (alpha > 80) {
                    val r =
                        android.graphics.Color
                            .red(pixel)

                    val g =
                        android.graphics.Color
                            .green(pixel)

                    val b =
                        android.graphics.Color
                            .blue(pixel)

                    val brightness =
                        (
                            r +
                                g +
                                b
                            ) / 3

                    if (
                        brightness in 18..238
                    ) {
                        /*
                         * Quantize RGB to reduce hundreds of
                         * nearby colors into useful clusters.
                         */
                        val qr =
                            r / 32

                        val qg =
                            g / 32

                        val qb =
                            b / 32

                        val bucket =
                            (qr shl 8) or
                                (qg shl 4) or
                                qb

                        buckets[bucket] =
                            (
                                buckets[bucket]
                                    ?: 0
                                ) + 1
                    }
                }

                x += stepX
            }

            y += stepY
        }

        val best =
            buckets.entries
                .sortedByDescending {
                    it.value
                }
                .take(12)

        if (best.isEmpty()) {
            return fallback()
        }

        val first =
            bucketColor(
                best.first().key
            )

        val second =
            best
                .drop(1)
                .map {
                    bucketColor(
                        it.key
                    )
                }
                .maxByOrNull {
                    colorDistance(
                        first,
                        it
                    )
                }
                ?: first.copy(
                    red =
                        (
                            first.red *
                                0.55f
                            ),
                    green =
                        (
                            first.green *
                                0.55f
                            ),
                    blue =
                        (
                            first.blue *
                                0.55f
                            )
                )

        return XvoxArtworkPalette(
            primary =
                normalize(first),
            secondary =
                normalize(second)
        )
    }

    private fun bucketColor(
        bucket: Int
    ): Color {
        val r =
            ((bucket shr 8) and 0xF) *
                32 + 16

        val g =
            ((bucket shr 4) and 0xF) *
                32 + 16

        val b =
            (bucket and 0xF) *
                32 + 16

        return Color(
            red =
                r.coerceIn(0, 255) /
                    255f,
            green =
                g.coerceIn(0, 255) /
                    255f,
            blue =
                b.coerceIn(0, 255) /
                    255f
        )
    }

    private fun normalize(
        color: Color
    ): Color {
        val maxChannel =
            max(
                color.red,
                max(
                    color.green,
                    color.blue
                )
            )

        val scale =
            if (maxChannel > 0.72f) {
                0.72f /
                    maxChannel
            } else {
                1f
            }

        return Color(
            red =
                color.red *
                    scale,
            green =
                color.green *
                    scale,
            blue =
                color.blue *
                    scale
        )
    }

    private fun colorDistance(
        a: Color,
        b: Color
    ): Float {
        val dr =
            a.red - b.red

        val dg =
            a.green - b.green

        val db =
            a.blue - b.blue

        return dr * dr +
            dg * dg +
            db * db
    }

    private fun fallback():
        XvoxArtworkPalette {
        return XvoxArtworkPalette(
            primary =
                Color(
                    0xFF31201C
                ),
            secondary =
                Color(
                    0xFF120D0C
                )
        )
    }
}
