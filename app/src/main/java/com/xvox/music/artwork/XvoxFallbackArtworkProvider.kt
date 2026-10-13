package com.xvox.music.artwork

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import com.xvox.music.R
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Lazy PNG source for Media3's playback notification when a song has no embedded artwork.
 * It avoids placing a bitmap on every queued MediaItem while giving Android's notification loader
 * the same deterministic, branded wave concept used by the app and widgets.
 */
class XvoxFallbackArtworkProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "image/png"

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor =
        AssetFileDescriptor(openFile(uri, mode), 0, AssetFileDescriptor.UNKNOWN_LENGTH)

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode.startsWith("r")) { "XVOX fallback artwork is read-only" }
        val songId = uri.lastPathSegment?.toLongOrNull()
            ?: throw IllegalArgumentException("Missing fallback artwork song id")
        val context = context ?: throw IllegalStateException("Provider is not attached")
        val bytes = notificationArtwork(context, songId.hashCode())
        val pipe = ParcelFileDescriptor.createPipe()
        Thread({
            runCatching {
                ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { stream -> stream.write(bytes) }
            }.onFailure {
                runCatching { pipe[1].close() }
            }
        }, "XvoxFallbackArtwork").start()
        return pipe[0]
    }

    companion object {
        private const val AUTHORITY = "com.xvox.music.fallbackart"
        private val artworkCache = object : LruCache<String, ByteArray>(2 * 1024 * 1024) {
            override fun sizeOf(key: String, value: ByteArray): Int = value.size
        }

        /** Used only on MediaItem metadata; UI Song models retain their original null artwork. */
        fun uriForSong(songId: Long): Uri = Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .appendPath(songId.toString())
            .build()

        private fun notificationArtwork(context: android.content.Context, seed: Int): ByteArray {
            // Mirror the palette inputs used by WavePatternSquare. This is deliberately read at
            // request time: a notification refreshed after an Appearance change must not regress
            // to the old fixed-red/default-dark placeholder.
            val themeConfig = runCatching {
                runBlocking {
                    val prefs = UserPreferencesRepository(context.applicationContext)
                    Triple(
                        prefs.theme.first(),
                        prefs.accentColor.first(),
                        prefs.themeBackground.first()
                    )
                }
            }.getOrDefault(Triple("System", "Red", "Default"))
            val isLight = when (themeConfig.first) {
                "Light" -> true
                "Dark", "AMOLED" -> false
                else -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
            }
            // In light mode WavePatternSquare starts from cardElevated. In dark modes it starts
            // from the active page background, including the persisted Midnight/Warm/custom tint.
            val base = notificationWaveBase(themeConfig.first, themeConfig.third, isLight)
            val accent = notificationAccent(themeConfig.second, isLight)
            val palette = notificationPalette(seed, base, accent)
            val key = "$seed:${palette.first}:${palette.second}"
            artworkCache.get(key)?.let { return it }
            val bitmap = buildNotificationWave(context, seed, palette.first, palette.second)
            val bytes = java.io.ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                bitmap.recycle()
                stream.toByteArray()
            }
            artworkCache.put(key, bytes)
            return bytes
        }

        private fun notificationWaveBase(theme: String, background: String, light: Boolean): Int {
            if (light) return Color.rgb(242, 242, 242) // XvoxWhitePalette.cardElevated
            return when (background) {
                "Midnight" -> Color.rgb(7, 11, 22)
                "Warm" -> Color.rgb(22, 19, 14)
                else -> if (background.startsWith("#")) {
                    runCatching { Color.parseColor(background) }.getOrElse {
                        if (theme == "AMOLED") Color.BLACK else Color.rgb(10, 10, 10)
                    }
                } else if (theme == "AMOLED") {
                    Color.BLACK
                } else {
                    Color.rgb(10, 10, 10)
                }
            }
        }

        private fun notificationAccent(value: String, light: Boolean): Int = when {
            value.startsWith("#") -> runCatching { Color.parseColor(value) }
                .getOrDefault(if (light) Color.rgb(10, 10, 10) else Color.WHITE)
            value.equals("Blue", ignoreCase = true) || value.equals("XVOX Blue", ignoreCase = true) ->
                if (light) Color.rgb(0, 122, 255) else Color.rgb(10, 132, 255)
            value.equals("Red", ignoreCase = true) || value.equals("XVOX Red", ignoreCase = true) ->
                Color.rgb(240, 30, 44)
            else -> if (light) Color.rgb(10, 10, 10) else Color.WHITE
        }

        private fun notificationPalette(seed: Int, base: Int, accent: Int): Pair<Int, Int> {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(accent, hsl)
            val variation = notificationSeedUnit(seed, 41)
            val offsets = floatArrayOf(-7f, 38f, 83f, 129f, 174f, 220f, 266f, 313f)
            val lane = notificationSeedIndex(seed, 73, offsets.size)
            val baseHue = if (hsl[1] < .08f) 0f else hsl[0]
            val hue = (baseHue + offsets[lane] + (variation - .5f) * 18f + 360f) % 360f
            val saturation = (
                if (hsl[1] < .15f) .60f
                else .46f + hsl[1] * .40f + (variation - .5f) * .12f
            ).coerceIn(.42f, .88f)
            val light = ColorUtils.calculateLuminance(base) > .5
            val lightness = if (light) (.43f + variation * .12f).coerceIn(.43f, .55f)
            else (.48f + variation * .15f).coerceIn(.48f, .63f)
            val stripe = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
            return ColorUtils.blendARGB(base, stripe, if (light) .12f else .18f) to stripe
        }

        private fun buildNotificationWave(context: android.content.Context, seed: Int, dark: Int, bright: Int): Bitmap {
            val size = 256
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(size * size)
            val darkRed = Color.red(dark)
            val darkGreen = Color.green(dark)
            val darkBlue = Color.blue(dark)
            val brightRed = Color.red(bright)
            val brightGreen = Color.green(bright)
            val brightBlue = Color.blue(bright)
            val scale = 1080f / size
            for (py in 0 until size) {
                for (px in 0 until size) {
                    val x = px * scale
                    val y = py * scale
                    val one = notificationNoise(x, y, 230f, seed)
                    val two = notificationNoise(x, y, 230f, seed + 1)
                    val three = notificationNoise(x, y, 110f, seed + 2)
                    val warpedX = x + 70f * one
                    val warpedY = y + 85f * two + 28f * three + 22f * sin(warpedX / 85f)
                    var stripe = .5f + .5f * sin(warpedY / 92f * 2f * PI.toFloat())
                    stripe = ((stripe - .5f) * 1.6f + .5f).coerceIn(0f, 1f)
                    stripe = stripe * stripe * (3f - 2f * stripe)
                    pixels[py * size + px] = Color.rgb(
                        (darkRed + (brightRed - darkRed) * stripe).roundToInt().coerceIn(0, 255),
                        (darkGreen + (brightGreen - darkGreen) * stripe).roundToInt().coerceIn(0, 255),
                        (darkBlue + (brightBlue - darkBlue) * stripe).roundToInt().coerceIn(0, 255)
                    )
                }
            }
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)

            val canvas = Canvas(bitmap)
            val tint = if (ColorUtils.calculateLuminance(dark) < .42) Color.argb(230, 255, 255, 255)
            else Color.argb(190, 0, 0, 0)
            // The provider is the notification's actual artwork source, so it renders the same
            // centered, mark-only fallback as Compose—never a generic app icon or text lockup.
            val markSize = (size * .34f).roundToInt()
            val left = (size - markSize) / 2
            val top = (size - markSize) / 2
            context.getDrawable(R.drawable.xvox)?.mutate()?.let { mark ->
                mark.setTint(tint)
                mark.setBounds(left, top, left + markSize, top + markSize)
                mark.draw(canvas)
            }
            return bitmap
        }

        private fun notificationSeedIndex(seed: Int, salt: Int, bound: Int): Int {
            var value = seed * 1103515245 + salt * 12345
            value = value xor (value ushr 16)
            return (value and Int.MAX_VALUE) % bound
        }

        private fun notificationSeedUnit(seed: Int, salt: Int): Float {
            var value = seed * 1103515245 + salt * 12345
            value = value xor (value ushr 13)
            value *= 1274126177
            value = value xor (value ushr 16)
            return (value and Int.MAX_VALUE).toFloat() / Int.MAX_VALUE.toFloat()
        }

        private fun notificationHash(ix: Int, iy: Int, seed: Int): Float {
            var value = ix * 374761393 + iy * 668265263 + seed * 1274126177
            value = (value xor (value ushr 13)) * 1274126177
            value = value xor (value ushr 16)
            return (value and 0xFFFF) / 32767.5f - 1f
        }

        private fun notificationFade(value: Float): Float =
            value * value * value * (value * (value * 6f - 15f) + 10f)

        private fun notificationNoise(x: Float, y: Float, cell: Float, seed: Int): Float {
            val gridX = x / cell
            val gridY = y / cell
            val indexX = floor(gridX).toInt()
            val indexY = floor(gridY).toInt()
            val fractionX = notificationFade(gridX - indexX)
            val fractionY = notificationFade(gridY - indexY)
            val topLeft = notificationHash(indexX, indexY, seed)
            val topRight = notificationHash(indexX + 1, indexY, seed)
            val bottomLeft = notificationHash(indexX, indexY + 1, seed)
            val bottomRight = notificationHash(indexX + 1, indexY + 1, seed)
            val top = topLeft + (topRight - topLeft) * fractionX
            val bottom = bottomLeft + (bottomRight - bottomLeft) * fractionX
            return (top + (bottom - top) * fractionY) * 2.2f
        }
    }
}
