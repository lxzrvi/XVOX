package com.xvox.music.player.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

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
                    val source =
                        ImageDecoder
                            .createSource(
                                appContext
                                    .contentResolver,
                                uri
                            )

                    val bitmap =
                        ImageDecoder
                            .decodeBitmap(
                                source
                            ) {
                                decoder,
                                info,
                                _ ->

                                val sample =
                                    max(
                                        1,
                                        max(
                                            info.size.width,
                                            info.size.height
                                        ) / 48
                                    )

                                decoder
                                    .setTargetSampleSize(
                                        sample
                                    )

                                decoder.allocator =
                                    ImageDecoder
                                        .ALLOCATOR_SOFTWARE
                            }

                    try {
                        dominant(
                            bitmap
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
                    .getOrDefault(
                        fallback()
                    )
            }

        cache[key] =
            result

        return result
    }

    private fun dominant(
        bitmap: Bitmap
    ): Color {
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

        while (
            y <
            bitmap.height
        ) {
            var x = 0

            while (
                x <
                bitmap.width
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

                val brightness =
                    (
                        r +
                            g +
                            b
                        ) / 3

                if (
                    brightness in
                    20..245
                ) {
                    val qr =
                        r / 32

                    val qg =
                        g / 32

                    val qb =
                        b / 32

                    val key =
                        (
                            qr shl 8
                            ) or
                            (
                                qg shl 4
                                ) or
                            qb

                    buckets[key] =
                        (
                            buckets[key]
                                ?: 0
                            ) + 1
                }

                x += stepX
            }

            y += stepY
        }

        val key =
            buckets
                .maxByOrNull {
                    it.value
                }
                ?.key
                ?: return fallback()

        val r =
            (
                (
                    key shr 8
                    ) and 0xF
                ) * 32 + 16

        val g =
            (
                (
                    key shr 4
                    ) and 0xF
                ) * 32 + 16

        val b =
            (
                key and 0xF
                ) * 32 + 16

        return lift(
            Color(
                red = r / 255f,
                green = g / 255f,
                blue = b / 255f
            )
        )
    }

    /*
     * Lift dark artwork colors toward a rich mid-tone.
     *
     * This keeps the Now Playing environment colorful
     * instead of muddy/black.
     */
    private fun lift(
        source: Color
    ): Color {
        val target =
            Color.White

        val luminance =
            source.luminance()

        val amount =
            when {
                luminance < 0.10f ->
                    0.42f

                luminance < 0.20f ->
                    0.30f

                luminance < 0.32f ->
                    0.18f

                else ->
                    0.08f
            }

        return Color(
            red =
                source.red +
                    (
                        target.red -
                            source.red
                        ) *
                    amount,

            green =
                source.green +
                    (
                        target.green -
                            source.green
                        ) *
                    amount,

            blue =
                source.blue +
                    (
                        target.blue -
                            source.blue
                        ) *
                    amount
        )
    }

    private fun fallback():
        Color {
        return Color(
            0xFF9A756C
        )
    }
}
