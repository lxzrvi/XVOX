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
import android.view.View
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
        val songTitle: String = "XVOX Music",
        val songArtist: String = "Tap to play",
        val artworkUri: Uri? = null,
        val isPlaying: Boolean = false,
        val isLiked: Boolean = false,
        val currentPosition: Long = 0L,
        val duration: Long = 0L,
        val transparency: Float = 0.25f,
        val theme: String = "Dynamic",
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
            songTitle = song?.title ?: "XVOX Music",
            songArtist = song?.artist ?: "Tap to play",
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
        layoutType: WidgetLayoutType = WidgetLayoutType.HORIZONTAL
    ): RemoteViews = withContext(Dispatchers.IO) {
        val layoutId = when (layoutType) {
            WidgetLayoutType.SQUARE -> R.layout.widget_xvox_player_square
            WidgetLayoutType.HORIZONTAL -> R.layout.widget_xvox_player_horizontal
            WidgetLayoutType.COMPACT -> R.layout.widget_xvox_player_compact
            WidgetLayoutType.STANDARD -> R.layout.widget_xvox_player
        }

        val views = RemoteViews(context.packageName, layoutId)

        val targetArtSize = when (layoutType) {
            WidgetLayoutType.SQUARE -> 400
            WidgetLayoutType.HORIZONTAL -> 220
            WidgetLayoutType.COMPACT -> 180
            WidgetLayoutType.STANDARD -> 260
        }
        val artworkBitmap = loadArtworkBitmap(context, state.artworkUri, targetArtSize)

        val primaryTextColor: Int
        val secondaryTextColor: Int
        val accentColor: Int
        val baseColor = when (state.theme) {
            "AMOLED" -> Color.parseColor("#000000")
            "Dark" -> Color.parseColor("#141414")
            "Light" -> Color.parseColor("#F5F5F5")
            "Glass" -> Color.parseColor("#1E1E1E")
            "Custom" -> runCatching { Color.parseColor(state.customColor) }.getOrDefault(Color.parseColor("#171717"))
            else -> {
                if (artworkBitmap != null) extractDominantColor(artworkBitmap) else Color.parseColor("#18181B")
            }
        }

        val alphaInt = ((1.0f - state.transparency.coerceIn(0f, 1f)) * 255).toInt().coerceIn(0, 255)
        val bgColor = Color.argb(alphaInt, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

        val isLightBg = ColorUtils.calculateLuminance(baseColor) > 0.5 && state.transparency < 0.7f
        if (isLightBg) {
            primaryTextColor = Color.parseColor("#111111")
            secondaryTextColor = Color.parseColor("#555555")
            accentColor = Color.parseColor("#000000")
        } else {
            primaryTextColor = Color.parseColor("#FFFFFF")
            secondaryTextColor = Color.parseColor("#D0D0D0")
            accentColor = Color.parseColor("#FFFFFF")
        }

        val bgBitmap = createRoundedBackgroundBitmap(context, bgColor, state.cornerRadiusDp)
        views.setImageViewBitmap(R.id.widget_bg, bgBitmap)
        runCatching { views.setImageViewBitmap(R.id.widget_background, bgBitmap) }

        if (artworkBitmap != null) {
            val density = context.resources.displayMetrics.density
            val cornerRadPx = if (layoutType == WidgetLayoutType.SQUARE) state.cornerRadiusDp * density else 10f * density
            val roundedArt = getRoundedBitmap(artworkBitmap, cornerRadPx)
            views.setImageViewBitmap(R.id.widget_cover, roundedArt)
            views.setViewVisibility(R.id.widget_cover, View.VISIBLE)
            views.setViewVisibility(R.id.widget_fallback_logo, View.GONE)
            runCatching { views.setImageViewBitmap(R.id.widget_artwork, roundedArt) }
        } else {
            views.setViewVisibility(R.id.widget_cover, View.GONE)
            views.setViewVisibility(R.id.widget_fallback_logo, View.VISIBLE)
            runCatching { views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_xvox_music_note) }
        }

        if (state.showLogo) {
            val logoBitmap = XvoxWidgetFontRenderer.createLogoBitmap(
                context = context,
                text = "X",
                textColor = primaryTextColor,
                textSizePx = 30f
            )
            views.setImageViewBitmap(R.id.widget_logo_cinzel, logoBitmap)
            views.setViewVisibility(R.id.widget_logo_cinzel, View.VISIBLE)
            runCatching {
                views.setImageViewBitmap(R.id.widget_logo_xvox, logoBitmap)
                views.setViewVisibility(R.id.widget_logo_xvox, View.VISIBLE)
            }
        } else {
            views.setViewVisibility(R.id.widget_logo_cinzel, View.GONE)
            runCatching { views.setViewVisibility(R.id.widget_logo_xvox, View.GONE) }
        }

        views.setTextViewText(R.id.widget_title, state.songTitle)
        views.setTextColor(R.id.widget_title, primaryTextColor)
        views.setTextViewText(R.id.widget_artist, state.songArtist)
        views.setTextColor(R.id.widget_artist, secondaryTextColor)

        runCatching {
            views.setTextViewText(R.id.widget_song_title, state.songTitle)
            views.setTextColor(R.id.widget_song_title, primaryTextColor)
            views.setTextViewText(R.id.widget_song_artist, state.songArtist)
            views.setTextColor(R.id.widget_song_artist, secondaryTextColor)
        }

        if (state.duration > 0L) {
            val progressInt = ((state.currentPosition.toFloat() / state.duration.toFloat()) * 1000).toInt().coerceIn(0, 1000)
            runCatching { views.setProgressBar(R.id.widget_progress_bar, 1000, progressInt, false) }
        } else {
            runCatching { views.setProgressBar(R.id.widget_progress_bar, 1000, 0, false) }
        }

        val playPauseRes = if (state.isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play
        views.setImageViewResource(R.id.widget_btn_play_pause, playPauseRes)
        views.setInt(R.id.widget_btn_play_pause, "setColorFilter", accentColor)
        views.setInt(R.id.widget_btn_prev, "setColorFilter", primaryTextColor)
        views.setInt(R.id.widget_btn_next, "setColorFilter", primaryTextColor)

        val heartRes = if (state.isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline
        views.setImageViewResource(R.id.widget_btn_like, heartRes)
        views.setInt(R.id.widget_btn_like, "setColorFilter", if (state.isLiked) Color.parseColor("#FF453A") else secondaryTextColor)

        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, createBroadcastPendingIntent(context, ACTION_PLAY_PAUSE, 101))
        views.setOnClickPendingIntent(R.id.widget_btn_prev, createBroadcastPendingIntent(context, ACTION_PREVIOUS, 102))
        views.setOnClickPendingIntent(R.id.widget_btn_next, createBroadcastPendingIntent(context, ACTION_NEXT, 103))
        views.setOnClickPendingIntent(R.id.widget_btn_like, createBroadcastPendingIntent(context, ACTION_TOGGLE_LIKE, 104))

        val openAppPendingIntent = createActivityPendingIntent(context, 105)
        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_cover, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

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
        val width = (320 * density).toInt().coerceAtLeast(100)
        val height = (180 * density).toInt().coerceAtLeast(60)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(35, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
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

    private fun getRoundedBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        val srcRect = Rect(xOffset, yOffset, xOffset + size, yOffset + size)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, srcRect, rect, paint)

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
