package com.xvox.music.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.util.LruCache
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
import kotlin.math.roundToInt

object XvoxWidgetHelper {
    const val ACTION_PLAY_PAUSE = "com.xvox.music.widget.ACTION_PLAY_PAUSE"
    const val ACTION_PREVIOUS = "com.xvox.music.widget.ACTION_PREVIOUS"
    const val ACTION_NEXT = "com.xvox.music.widget.ACTION_NEXT"
    const val ACTION_TOGGLE_LIKE = "com.xvox.music.widget.ACTION_TOGGLE_LIKE"
    const val ACTION_OPEN_PLAYER = "com.xvox.music.widget.ACTION_OPEN_PLAYER"
    const val ACTION_UPDATE_WIDGET = "com.xvox.music.widget.ACTION_UPDATE_WIDGET"

    data class WidgetDisplayState(
        val songTitle: String = "XVOX Music", val songArtist: String = "Tap to play", val artworkUri: Uri? = null,
        val isPlaying: Boolean = false, val isLiked: Boolean = false, val currentPosition: Long = 0,
        val duration: Long = 0, val transparency: Float = .25f, val theme: String = "Dynamic",
        val customColor: String = "#171717", val showLogo: Boolean = true, val cornerRadiusDp: Int = 24,
        val paddingX: Int = 10, val paddingY: Int = 8
    )
    private val bitmaps = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val dominantColors = LruCache<String, Int>(96)

    suspend fun loadCurrentWidgetState(context: Context, song: Song?, isPlaying: Boolean, position: Long = 0, duration: Long = 0): WidgetDisplayState = withContext(Dispatchers.IO) {
        val style = UserPreferencesRepository(context).widgetStyle.first()
        val liked = song?.let { it.id in XvoxLibraryPreferences(context).likedSongIds.first() } ?: false
        WidgetDisplayState(song?.title ?: "XVOX Music", song?.artist ?: "Tap to play", song?.artworkUri,
            isPlaying, liked, position, duration.takeIf { it > 0 } ?: song?.duration ?: 0,
            style.transparency, style.theme, style.customColor, style.showLogo, style.radius, style.paddingX, style.paddingY)
    }

    fun layoutId(type: WidgetLayoutType): Int = when (type) {
        WidgetLayoutType.TINY -> R.layout.widget_xvox_player_tiny
        WidgetLayoutType.VERTICAL -> R.layout.widget_xvox_player_vertical
        WidgetLayoutType.SQUARE -> R.layout.widget_xvox_player_square
        WidgetLayoutType.COMPACT -> R.layout.widget_xvox_player_compact
        WidgetLayoutType.HORIZONTAL -> R.layout.widget_xvox_player_horizontal
        WidgetLayoutType.STANDARD -> R.layout.widget_xvox_player
    }

    suspend fun buildRemoteViews(context: Context, state: WidgetDisplayState, widthDp: Int = 300, heightDp: Int = 90, interactive: Boolean = true): RemoteViews = withContext(Dispatchers.IO) {
        val spec = spec(context, state, widthDp, heightDp)
        val views = RemoteViews(context.packageName, layoutId(spec.type))
        val density = context.resources.displayMetrics.density
        val art = artwork(context, state.artworkUri)
        if (art != null && state.artworkUri != null && dominantColors.get(state.artworkUri.toString()) == null) {
            dominantColors.put(state.artworkUri.toString(), dominant(art))
        }
        val base = backgroundColor(state)
        val foreground = foregroundColor(state, base)
        val secondary = ColorUtils.setAlphaComponent(foreground, 185)
        val bg = ColorUtils.setAlphaComponent(base, ((1 - state.transparency.coerceIn(0f, 1f)) * 255).roundToInt())
        views.setImageViewBitmap(R.id.widget_bg, background(spec, bg, foreground, density))
        views.setViewPadding(R.id.widget_content, (spec.paddingX * density).roundToInt(), (spec.paddingY * density).roundToInt(),
            (spec.paddingX * density).roundToInt(), (spec.paddingY * density).roundToInt())
        if (art != null) {
            views.setImageViewBitmap(R.id.widget_cover, roundedCover(art, state.artworkUri.toString(), spec, density))
        }
        views.setViewVisibility(R.id.widget_cover, if (art != null) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_fallback_logo, if (art == null) View.VISIBLE else View.GONE)
        views.setInt(R.id.widget_fallback_logo, "setColorFilter", foreground)
        views.setTextViewText(R.id.widget_title, state.songTitle)
        views.setTextViewText(R.id.widget_artist, state.songArtist)
        views.setTextColor(R.id.widget_title, foreground)
        views.setTextColor(R.id.widget_artist, secondary)
        views.setViewVisibility(R.id.widget_title, if (spec.showTitle) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_artist, if (spec.showArtist) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_logo_cinzel, if (spec.showLogo) View.VISIBLE else View.GONE)
        if (spec.showLogo) {
            val key = "logo:$foreground:$density"
            val logo = bitmaps.get(key) ?: XvoxWidgetFontRenderer.createLogoBitmap(context, "X", foreground, 30f * density).also { bitmaps.put(key, it) }
            views.setImageViewBitmap(R.id.widget_logo_cinzel, logo)
        }
        views.setImageViewResource(R.id.widget_btn_play_pause, if (state.isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play)
        views.setImageViewResource(R.id.widget_btn_like, if (state.isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline)
        views.setInt(R.id.widget_btn_play_pause, "setColorFilter", if (spec.type == WidgetLayoutType.TINY) Color.WHITE else foreground)
        views.setInt(R.id.widget_btn_prev, "setColorFilter", foreground)
        views.setInt(R.id.widget_btn_next, "setColorFilter", foreground)
        views.setInt(R.id.widget_btn_like, "setColorFilter", if (state.isLiked) Color.rgb(255, 69, 58) else secondary)
        views.setViewVisibility(R.id.widget_btn_prev, if (spec.showPrevious) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_btn_like, if (spec.showLike) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_btn_next, if (spec.type == WidgetLayoutType.TINY) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_progress_bar, if (spec.type == WidgetLayoutType.TINY) View.GONE else View.VISIBLE)
        views.setImageViewBitmap(R.id.widget_progress_bar, progressBitmap(state.currentPosition, state.duration, foreground))
        if (interactive) {
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, action(context, ACTION_PLAY_PAUSE, 101))
            views.setOnClickPendingIntent(R.id.widget_btn_prev, action(context, ACTION_PREVIOUS, 102))
            views.setOnClickPendingIntent(R.id.widget_btn_next, action(context, ACTION_NEXT, 103))
            views.setOnClickPendingIntent(R.id.widget_btn_like, action(context, ACTION_TOGGLE_LIKE, 104))
            val open = PendingIntent.getActivity(context, 105, Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_cover, open)
            views.setOnClickPendingIntent(R.id.widget_title, open)
        }
        views
    }

    /** Progress-only actions are a few KB; no album decode, full card bitmap or preference reads. */
    fun progressRemoteViews(context: Context, state: WidgetDisplayState, widthDp: Int, heightDp: Int): RemoteViews {
        val spec = spec(context, state, widthDp, heightDp)
        return RemoteViews(context.packageName, layoutId(spec.type)).apply {
            setImageViewBitmap(R.id.widget_progress_bar, progressBitmap(state.currentPosition, state.duration, foregroundColor(state, backgroundColor(state))))
        }
    }
    private fun spec(context: Context, state: WidgetDisplayState, w: Int, h: Int) = WidgetLayoutSpec.create(
        w, h, state.paddingX, state.paddingY, state.cornerRadiusDp, state.showLogo, context.resources.configuration.fontScale)
    private fun action(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(context, code,
        Intent(action).setComponent(ComponentName(context, XvoxAppWidgetProvider::class.java)),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun backgroundColor(state: WidgetDisplayState): Int = when (state.theme) {
        "AMOLED" -> Color.BLACK
        "Light" -> Color.rgb(245, 245, 245)
        "Dark" -> Color.rgb(20, 20, 20)
        "Glass" -> Color.rgb(30, 30, 34)
        "Custom" -> runCatching { Color.parseColor(state.customColor) }.getOrDefault(Color.rgb(23, 23, 23))
        else -> state.artworkUri?.let { dominantColors.get(it.toString()) } ?: Color.rgb(24, 24, 27)
    }
    private fun foregroundColor(state: WidgetDisplayState, base: Int): Int =
        if (ColorUtils.calculateLuminance(base) > .5 && state.transparency < .7f) Color.rgb(17, 17, 17) else Color.WHITE
    private fun background(spec: WidgetLayoutSpec, color: Int, foreground: Int, density: Float): Bitmap {
        val key = "bg:${spec.width}:${spec.height}:${spec.radius}:$color:$foreground:$density"
        bitmaps.get(key)?.let { return it }
        val scale = minOf(1f, 512f / (maxOf(spec.width, spec.height) * density))
        val w = (spec.width * density * scale).roundToInt().coerceAtLeast(1)
        val h = (spec.height * density * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = spec.radius * density * scale
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, paint)
        paint.color = ColorUtils.setAlphaComponent(foreground, 35); paint.style = Paint.Style.STROKE; paint.strokeWidth = maxOf(1f, density * scale * .7f)
        val inset = paint.strokeWidth / 2
        canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, paint)
        bitmaps.put(key, bitmap)
        return bitmap
    }
    private fun roundedCover(source: Bitmap, uri: String, spec: WidgetLayoutSpec, density: Float): Bitmap {
        val key = "cover:$uri:${spec.coverSide}:${spec.coverRadius}:$density"
        bitmaps.get(key)?.let { return it }
        val side = (spec.coverSide * density).roundToInt().coerceIn(16, 320)
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = side.toFloat() / minOf(source.width, source.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((side - source.width * scale) / 2, (side - source.height * scale) / 2)
        }
        shader.setLocalMatrix(matrix)
        val radius = spec.coverRadius / spec.coverSide * side
        Canvas(bitmap).drawRoundRect(RectF(0f, 0f, side.toFloat(), side.toFloat()), radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader })
        bitmaps.put(key, bitmap)
        return bitmap
    }
    private fun progressBitmap(position: Long, duration: Long, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 4, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { this.color = ColorUtils.setAlphaComponent(color, 45) }
        canvas.drawRect(0f, 0f, 256f, 4f, paint)
        val blend = com.xvox.music.player.playback.XvoxBlendMonitor.state.value
        if (blend.enabled && duration > 0) {
            val window = com.xvox.music.player.playback.CrossfadeMath.windowMs(blend.configuredSeconds, duration).toFloat() / duration
            paint.color = Color.argb(140, 98, 205, 189)
            canvas.drawRect(0f, 0f, 256 * window, 4f, paint)
            paint.color = Color.argb(140, 230, 171, 108)
            canvas.drawRect(256 * (1 - window), 0f, 256f, 4f, paint)
        }
        paint.color = color
        val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        canvas.drawRect(0f, 1f, 256 * fraction, 3f, paint)
        return bitmap
    }
    private fun artwork(context: Context, uri: Uri?): Bitmap? {
        if (uri == null) return null
        val key = "source:$uri"
        bitmaps.get(key)?.let { return it }
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?.also { bitmaps.put(key, it) }
        }.getOrNull()
    }
    private fun dominant(bitmap: Bitmap): Int {
        var r = 0L; var g = 0L; var b = 0L; var count = 0
        val step = maxOf(1, minOf(bitmap.width, bitmap.height) / 16)
        for (y in 0 until bitmap.height step step) for (x in 0 until bitmap.width step step) {
            val p = bitmap.getPixel(x, y); r += Color.red(p); g += Color.green(p); b += Color.blue(p); count++
        }
        return if (count > 0) Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt()) else Color.rgb(24, 24, 27)
    }
}
