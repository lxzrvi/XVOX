package com.xvox.music.artwork

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.xvox.music.R
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Lazy PNG source for Media3's playback notification when a song has no embedded artwork.
 *
 * Android's notification pipeline needs a local content URI rather than a Compose painter.  This
 * provider therefore renders the same bundled XVOX default cover used by the app: the supplied
 * dark asset in dark themes and the separately supplied light asset in light themes. No extra
 * mark is painted and neither source image is transformed at runtime.
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
        require(uri.lastPathSegment?.toLongOrNull() != null) { "Missing fallback artwork song id" }
        val context = context ?: throw IllegalStateException("Provider is not attached")
        val bytes = notificationArtwork(context)
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

        private fun notificationArtwork(context: Context): ByteArray {
            // Notification artwork is produced outside Compose, so read the app's selected theme
            // at request time. The accent is intentionally irrelevant to this pure bitmap path.
            val theme = runCatching {
                runBlocking { UserPreferencesRepository(context.applicationContext).theme.first() }
            }.getOrDefault("System")
            val light = when (theme) {
                "Light" -> true
                "Dark", "AMOLED" -> false
                else -> context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
            }
            val key = if (light) "xvox_default:light" else "xvox_default:dark"
            artworkCache.get(key)?.let { return it }

            val bitmap = buildNotificationDefaultCover(context, light)
            val bytes = java.io.ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                bitmap.recycle()
                stream.toByteArray()
            }
            artworkCache.put(key, bytes)
            return bytes
        }

        private fun buildNotificationDefaultCover(context: Context, light: Boolean): Bitmap {
            val source = checkNotNull(
                BitmapFactory.decodeResource(
                    context.resources,
                    if (light) R.drawable.xvox_default_2 else R.drawable.xvox_default,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                )
            ) { "Bundled XVOX default cover is unavailable" }
            // The supplied bitmap is the complete fallback identity. Keep its decoded source
            // dimensions intact; the notification surface performs its own display scaling. Do
            // not paint, recolour, downsample, or invert either source artwork after decode.
            return source
        }
    }
}
