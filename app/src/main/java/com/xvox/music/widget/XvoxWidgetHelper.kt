package com.xvox.music.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.xvox.music.MainActivity
import com.xvox.music.R
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object XvoxWidgetHelper {

    const val ACTION_PLAY_PAUSE = "com.xvox.music.widget.ACTION_PLAY_PAUSE"
    const val ACTION_PREVIOUS = "com.xvox.music.widget.ACTION_PREVIOUS"
    const val ACTION_NEXT = "com.xvox.music.widget.ACTION_NEXT"
    const val ACTION_TOGGLE_LIKE = "com.xvox.music.widget.ACTION_TOGGLE_LIKE"
    const val ACTION_OPEN_PLAYER = "com.xvox.music.widget.ACTION_OPEN_PLAYER"
    const val ACTION_UPDATE_WIDGET = "com.xvox.music.widget.ACTION_UPDATE_WIDGET"

    data class WidgetDisplayState(
        val songTitle: String = "No track playing",
        val songArtist: String = "Tap to open XVOX",
        val artworkUri: Uri? = null,
        val isPlaying: Boolean = false,
        val isLiked: Boolean = false,
        val currentPosition: Long = 0L,
        val duration: Long = 0L,
        val transparency: Float = 0.25f, // 0.0 = solid, 1.0 = fully transparent
        val theme: String = "Dynamic", // Dynamic, AMOLED, Dark, Light, Glass, Custom
        val customColor: String = "#171717",
        val showLogo: Boolean = true,
        val cornerRadiusDp: Int = 24
    )

    suspend fun loadCurrentWidgetState(
        context: Context,
        song: Song?,
        isPlaying: Boolean,
        position: Long = 0L,
        duration: Long = 0L
    ): WidgetDisplayState = withContext(Dispatchers.IO) {
        val prefs = UserPreferencesRepository(context)
        val libPrefs = XvoxLibraryPreferences(context)

        val transparency = prefs.widgetTransparency.first()
        val theme = prefs.widgetTheme.first()
        val customColor = prefs.widgetCustomColor.first()
        val showLogo = prefs.widgetShowLogo.first()
        val cornerRadius = prefs.widgetCornerRadius.first()

        val isLiked = if (song != null) {
            libPrefs.likedSongIds.first().contains(song.id)
        } else false

        WidgetDisplayState(
            songTitle = song?.title ?: "No track playing",
            songArtist = song?.artist ?: "Tap to open XVOX",
            artworkUri = song?.artworkUri,
            isPlaying = isPlaying,
            isLiked = isLiked,
            currentPosition = position,
            duration = if (duration > 0L) duration else (song?.duration ?: 0L),
            transparency = transparency,
            theme = theme,
            customColor = customColor,
            showLogo = showLogo,
            cornerRadiusDp = cornerRadius
        )
    }

    suspend fun buildRemoteViews(
        context: Context,
        state: WidgetDisplayState,
        isCompact: Boolean = false
    ): RemoteViews = withContext(Dispatchers.IO) {
        val layoutId = if (isCompact) R.layout.widget_xvox_player_compact else R.layout.widget_xvox_player
        val views = RemoteViews(context.packageName, layoutId)

        // 1. Artwork loading (Scales nicely)
        val targetArtSize = if (isCompact) 140 else 200
        val artworkBitmap = loadArtworkBitmap(context, state.artworkUri, targetArtSize)
        if (artworkBitmap != null) {
            val roundedArt = getRoundedBitmap(artworkBitmap, 18f)
            views.setImageViewBitmap(R.id.widget_artwork, roundedArt)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_xvox_music_note)
        }

        // 2. Background Color & Transparency computation
        val primaryTextColor: Int
        val secondaryTextColor: Int
        val accentColor: Int
        val bgColor: Int

        val baseColor = when (state.theme) {
            "AMOLED" -> Color.parseColor("#000000")
            "Dark" -> Color.parseColor("#141414")
            "Light" -> Color.parseColor("#F5F5F5")
            "Glass" -> Color.parseColor("#1E1E1E")
            "Custom" -> runCatching { Color.parseColor(state.customColor) }.getOrDefault(Color.parseColor("#171717"))
            else -> { // Dynamic
                if (artworkBitmap != null) {
                    extractDominantColor(artworkBitmap)
                } else {
                    Color.parseColor("#18181B")
                }
            }
        }

        val alphaInt = ((1.0f - state.transparency.coerceIn(0f, 1f)) * 255).toInt().coerceIn(0, 255)
        bgColor = Color.argb(alphaInt, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

        val isLightBg = ColorUtils.calculateLuminance(baseColor) > 0.5 && state.transparency < 0.7f
        if (isLightBg) {
            primaryTextColor = Color.parseColor("#111111")
            secondaryTextColor = Color.parseColor("#555555")
            accentColor = Color.parseColor("#000000")
        } else {
            primaryTextColor = Color.parseColor("#FFFFFF")
            secondaryTextColor = Color.parseColor("#A3A3A3")
            accentColor = Color.parseColor("#FFFFFF")
        }

        // 3. Render dynamic background with rounded corners
        val bgBitmap = createRoundedBackgroundBitmap(context, bgColor, state.cornerRadiusDp)
        views.setImageViewBitmap(R.id.widget_background, bgBitmap)

        // 4. "XVOX" Branding in custom Cinzel font
        if (state.showLogo) {
            val logoBitmap = XvoxWidgetFontRenderer.createLogoBitmap(
                context = context,
                text = "XVOX",
                textColor = primaryTextColor,
                textSizePx = if (isCompact) 28f else 38f
            )
            views.setImageViewBitmap(R.id.widget_logo_xvox, logoBitmap)
            views.setViewVisibility(R.id.widget_logo_xvox, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_logo_xvox, android.view.View.GONE)
        }

        // 5. Track Info text & colors
        views.setTextViewText(R.id.widget_song_title, state.songTitle)
        views.setTextColor(R.id.widget_song_title, primaryTextColor)
        views.setTextViewText(R.id.widget_song_artist, state.songArtist)
        views.setTextColor(R.id.widget_song_artist, secondaryTextColor)

        // 6. Play / Pause button icon
        val playPauseRes = if (state.isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play
        views.setImageViewResource(R.id.widget_btn_play_pause, playPauseRes)
        views.setInt(R.id.widget_btn_play_pause, "setColorFilter", accentColor)
        views.setInt(R.id.widget_btn_prev, "setColorFilter", primaryTextColor)
        views.setInt(R.id.widget_btn_next, "setColorFilter", primaryTextColor)

        // 7. Like Heart button
        val heartRes = if (state.isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline
        views.setImageViewResource(R.id.widget_btn_like, heartRes)
        views.setInt(R.id.widget_btn_like, "setColorFilter", if (state.isLiked) Color.parseColor("#FF453A") else secondaryTextColor)

        // Progress bar (only in full layout)
        if (!isCompact) {
            val progress = if (state.duration > 0) {
                ((state.currentPosition.toFloat() / state.duration) * 1000).toInt().coerceIn(0, 1000)
            } else 0
            views.setProgressBar(R.id.widget_progress_bar, 1000, progress, false)
        }

        // 8. Pending Intents for interactive controls
        // Play/Pause
        views.setOnClickPendingIntent(
            R.id.widget_btn_play_pause,
            createBroadcastPendingIntent(context, ACTION_PLAY_PAUSE, 101)
        )
        // Previous
        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            createBroadcastPendingIntent(context, ACTION_PREVIOUS, 102)
        )
        // Next
        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            createBroadcastPendingIntent(context, ACTION_NEXT, 103)
        )
        // Like Toggle
        views.setOnClickPendingIntent(
            R.id.widget_btn_like,
            createBroadcastPendingIntent(context, ACTION_TOGGLE_LIKE, 104)
        )

        // Click on Info / Background opens MainActivity
        val openAppPendingIntent = createActivityPendingIntent(context, 105)
        views.setOnClickPendingIntent(R.id.widget_artwork, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_song_title, openAppPendingIntent)
        if (!isCompact) {
            views.setOnClickPendingIntent(R.id.widget_info_row, openAppPendingIntent)
        }

        views
    }

    private fun createBroadcastPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).apply {
            component = ComponentName(context, XvoxAppWidgetProvider::class.java)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun createActivityPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun createRoundedBackgroundBitmap(context: Context, color: Int, radiusDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val radiusPx = radiusDp * density
        val width = (300 * density).toInt()
        val height = (160 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        val borderRect = RectF(1f, 1f, width.toFloat() - 1f, height.toFloat() - 1f)
        canvas.drawRoundRect(borderRect, radiusPx, radiusPx, borderPaint)

        return bitmap
    }

    private fun loadArtworkBitmap(context: Context, uri: Uri?, targetSizePx: Int): Bitmap? {
        if (uri == null) return null
        return runCatching {
            val contentResolver = context.contentResolver
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            var inSampleSize = 1
            if (options.outHeight > targetSizePx || options.outWidth > targetSizePx) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= targetSizePx && halfWidth / inSampleSize >= targetSizePx) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        }.getOrNull()
    }

    private fun getRoundedBitmap(bitmap: Bitmap, cornerRadiusDp: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, width, height)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, cornerRadiusDp, cornerRadiusDp, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    private fun extractDominantColor(bitmap: Bitmap): Int {
        return runCatching {
            val sample = Bitmap.createScaledBitmap(bitmap, 16, 16, false)
            var redBucket = 0L
            var greenBucket = 0L
            var blueBucket = 0L
            val pixelCount = sample.width * sample.height

            for (x in 0 until sample.width) {
                for (y in 0 until sample.height) {
                    val p = sample.getPixel(x, y)
                    redBucket += Color.red(p)
                    greenBucket += Color.green(p)
                    blueBucket += Color.blue(p)
                }
            }
            sample.recycle()

            val r = (redBucket / pixelCount).toInt().coerceIn(15, 230)
            val g = (greenBucket / pixelCount).toInt().coerceIn(15, 230)
            val b = (blueBucket / pixelCount).toInt().coerceIn(15, 230)
            Color.rgb(r, g, b)
        }.getOrDefault(Color.parseColor("#1E1E24"))
    }
}
