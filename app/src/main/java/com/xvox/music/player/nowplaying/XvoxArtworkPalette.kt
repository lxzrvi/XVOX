package com.xvox.music.player.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class XvoxArtworkPaletteLoader(
    context: Context
) {
    private val appContext =
        context.applicationContext

    private val cache =
        ConcurrentHashMap<String, Color>()

    suspend fun load(
        uri: Uri?
    ): Color {
        if (uri == null) {
            return fallback()
        }

        val key =
            uri.toString()

        cache[key]?.let {
            return it
        }

        val result =
            withContext(
                Dispatchers.IO
            ) {
                runCatching {
                    decode(
                        uri
                    )
                }
                    .getOrNull()
                    ?.let {
                        bitmap ->

                        try {
                            extract(
                                bitmap
                            )
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    ?: fallback()
            }

        cache[key] =
            result

        return result
    }

    private fun decode(
        uri: Uri
    ): Bitmap {
        val source =
            ImageDecoder.createSource(
                appContext
                    .contentResolver,
                uri
            )

        return ImageDecoder.decodeBitmap(
            source
        ) {
            decoder,
            info,
            _ ->

            val maxSide =
                max(
                    info.size.width,
                    info.size.height
                )

            decoder.setTargetSampleSize(
                max(
                    1,
                    maxSide / 56
                )
            )

            decoder.allocator =
                ImageDecoder
                    .ALLOCATOR_SOFTWARE
        }
    }

    private fun extract(
        bitmap: Bitmap
    ): Color {
        data class Bucket(
            var count: Int = 0,
            var red: Long = 0L,
            var green: Long = 0L,
            var blue: Long = 0L,
            var saturation: Float = 0f
        )

        val buckets =
            HashMap<Int, Bucket>()

        val stepX =
            max(
                1,
                bitmap.width / 28
            )

        val stepY =
            max(
                1,
                bitmap.height / 28
            )

        var y = 0

        while (
            y < bitmap.height
        ) {
            var x = 0

            while (
                x < bitmap.width
            ) {
                val pixel =
                    bitmap.getPixel(
                        x,
                        y
                    )

                val r =
                    android.graphics.Color
                        .red(pixel)

                val g =
                    android.graphics.Color
                        .green(pixel)

                val b =
                    android.graphics.Color
                        .blue(pixel)

                val maxChannel =
                    max(
                        r,
                        max(
                            g,
                            b
                        )
                    )

                val minChannel =
                    min(
                        r,
                        min(
                            g,
                            b
                        )
                    )

                val brightness =
                    (
                        r +
                            g +
                            b
                        ) / 3f

                val saturation =
                    if (
                        maxChannel == 0
                    ) {
                        0f
                    } else {
                        (
                            maxChannel -
                                minChannel
                            ) /
                            maxChannel.toFloat()
                    }

                /*
                 * Ignore nearly-black, nearly-white and
                 * almost-gray samples.
                 */
                if (
                    brightness in
                    28f..232f &&
                    saturation >
                    0.12f
                ) {
                    val key =
                        (
                            (r / 32) shl 8
                            ) or
                            (
                                (g / 32) shl 4
                                ) or
                            (b / 32)

                    val bucket =
                        buckets
                            .getOrPut(
                                key
                            ) {
                                Bucket()
                            }

                    bucket.count++
                    bucket.red +=
                        r.toLong()
                    bucket.green +=
                        g.toLong()
                    bucket.blue +=
                        b.toLong()
                    bucket.saturation +=
                        saturation
                }

                x += stepX
            }

            y += stepY
        }

        val best =
            buckets.values
                .maxByOrNull {
                    bucket ->

                    val averageSaturation =
                        bucket.saturation /
                            bucket.count
                                .coerceAtLeast(
                                    1
                                )

                    bucket.count *
                        (
                            0.65f +
                                averageSaturation *
                                    1.25f
                            )
                }
                ?: return fallback()

        val count =
            best.count
                .coerceAtLeast(
                    1
                )

        val source =
            Color(
                red =
                    (
                        best.red /
                            count
                        )
                        .coerceIn(
                            0L,
                            255L
                        ) /
                        255f,

                green =
                    (
                        best.green /
                            count
                        )
                        .coerceIn(
                            0L,
                            255L
                        ) /
                        255f,

                blue =
                    (
                        best.blue /
                            count
                        )
                        .coerceIn(
                            0L,
                            255L
                        ) /
                        255f
            )

        return normalize(
            source
        )
    }

    private fun normalize(
        source: Color
    ): Color {
        val luminance =
            source.luminance()

        /*
         * Desired natural mid-tone.
         *
         * Dark artwork is lifted,
         * very bright artwork is gently reduced,
         * but hue remains intact.
         */
        val target =
            when {
                luminance <
                    0.18f ->
                    0.30f

                luminance >
                    0.62f ->
                    0.50f

                else ->
                    luminance
            }

        if (
            abs(
                target -
                    luminance
            ) <
            0.015f
        ) {
            return source
        }

        return if (
            target >
            luminance
        ) {
            val amount =
                (
                    target -
                        luminance
                    ) /
                    (
                        1f -
                            luminance
                        )
                            .coerceAtLeast(
                                0.01f
                            )

            Color(
                red =
                    source.red +
                        (
                            1f -
                                source.red
                            ) *
                        amount,

                green =
                    source.green +
                        (
                            1f -
                                source.green
                            ) *
                        amount,

                blue =
                    source.blue +
                        (
                            1f -
                                source.blue
                            ) *
                        amount
            )
        } else {
            val scale =
                (
                    target /
                        luminance
                    )
                    .coerceIn(
                        0f,
                        1f
                    )

            Color(
                red =
                    source.red *
                        scale,

                green =
                    source.green *
                        scale,

                blue =
                    source.blue *
                        scale
            )
        }
    }

    private fun fallback():
        Color {
        return Color(
            0xFF8C7772
        )
    }
}
