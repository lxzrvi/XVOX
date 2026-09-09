package com.xvox.music.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.xvox.music.MainActivity
import com.xvox.music.R
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import com.xvox.music.player.session.XvoxPlaybackService
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
        val songTitle: String = "XVOX Music", val songArtist: String = "Tap play", val artworkUri: Uri? = null,
        val isPlaying: Boolean = false, val isLiked: Boolean = false, val currentPosition: Long = 0,
        val duration: Long = 0, val transparency: Float = .25f, val theme: String = "Dynamic", val customColor: String = "#171717",
        val showLogo: Boolean = true, val cornerRadiusDp: Int = 24, val paddingX: Int = 10, val paddingY: Int = 8,
        val customization: WidgetCustomization = WidgetCustomization()
    )
    private val images = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val colours = LruCache<String, Int>(80)
    private val steps = listOf(12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 56, 64, 80, 96, 120, 144, 160)
    private fun size(value: Int) = steps.lastOrNull { it <= value } ?: 12
    private fun imageLayout(value: Int): Int = when (value) {
        12 -> R.layout.widget_image_12; 16 -> R.layout.widget_image_16; 20 -> R.layout.widget_image_20
        24 -> R.layout.widget_image_24; 28 -> R.layout.widget_image_28; 32 -> R.layout.widget_image_32
        36 -> R.layout.widget_image_36; 40 -> R.layout.widget_image_40; 44 -> R.layout.widget_image_44
        48 -> R.layout.widget_image_48; 56 -> R.layout.widget_image_56; 64 -> R.layout.widget_image_64
        80 -> R.layout.widget_image_80; 96 -> R.layout.widget_image_96; 120 -> R.layout.widget_image_120
        144 -> R.layout.widget_image_144; else -> R.layout.widget_image_160
    }
    suspend fun loadCurrentWidgetState(context: Context, song: Song?, isPlaying: Boolean, position: Long = 0, duration: Long = 0): WidgetDisplayState = withContext(Dispatchers.IO) {
        val style = UserPreferencesRepository(context).widgetStyle.first()
        val liked = song?.let { it.id in XvoxLibraryPreferences(context).likedSongIds.first() } ?: false
        WidgetDisplayState(song?.title ?: "XVOX Music", song?.artist ?: "Tap play", song?.artworkUri, isPlaying, liked, position,
            duration.takeIf { it > 0 } ?: song?.duration ?: 0, style.transparency, style.theme, style.customColor,
            style.showLogo, style.radius, style.paddingX, style.paddingY, style.customization)
    }
    suspend fun buildRemoteViews(context: Context, state: WidgetDisplayState, widthDp: Int = 300, heightDp: Int = 90, interactive: Boolean = true): RemoteViews = withContext(Dispatchers.IO) {
        val c = state.customization.sanitized()
        val w = widthDp.coerceAtLeast(40); val h = heightDp.coerceAtLeast(40)
        val mx = c.marginX.coerceAtMost(w / 6); val my = c.marginY.coerceAtMost(h / 6)
        val iw = w - 2 * mx; val ih = h - 2 * my
        val spec = WidgetLayoutSpec.create(iw, ih, state.paddingX, state.paddingY, state.cornerRadiusDp, state.showLogo)
        val cw = (iw - 2 * spec.paddingX).coerceAtLeast(12); val ch = (ih - 2 * spec.paddingY).coerceAtLeast(12)
        val narrow = iw < 140 && ih >= 120
        val tiny = iw < 110 && ih < 120
        val full = c.fullCover || (tiny && c.coverPlacement == "auto")
        val art = artwork(context, state.artworkUri)
        val base = when (state.theme) {
            "AMOLED" -> Color.BLACK; "Light" -> Color.rgb(245, 245, 245); "Dark" -> Color.rgb(20, 20, 20)
            "Glass" -> Color.rgb(30, 30, 34); "Custom" -> color(c = state.customColor, fallback = Color.DKGRAY)
            else -> state.artworkUri?.let { colours.get(it.toString()) } ?: Color.rgb(24, 24, 27)
        }
        val fg = if (!full && ColorUtils.calculateLuminance(base) > .5 && state.transparency < .7) Color.rgb(17, 17, 17) else Color.WHITE
        val background = ColorUtils.setAlphaComponent(base, ((1 - state.transparency.coerceIn(0f, 1f)) * 255).roundToInt())
        val views = RemoteViews(context.packageName, if (narrow) R.layout.widget_xvox_player_custom_narrow else R.layout.widget_xvox_player)
        val density = context.resources.displayMetrics.density
        fun px(dp: Int) = (dp * density).roundToInt()
        views.setViewPadding(R.id.widget_bg, px(mx), px(my), px(mx), px(my))
        views.setViewPadding(R.id.widget_content, px(mx + spec.paddingX), px(my + spec.paddingY), px(mx + spec.paddingX), px(my + spec.paddingY))
        for (overlay in listOf(R.id.widget_overlay_left, R.id.widget_overlay_center, R.id.widget_overlay_right)) {
            views.setViewPadding(overlay, px(mx + spec.paddingX), px(my + spec.paddingY), px(mx + spec.paddingX), px(my + spec.paddingY))
        }
        views.setImageViewBitmap(R.id.widget_bg, surface(iw, ih, spec.radius, background,
            color(c.borderColor, ColorUtils.setAlphaComponent(fg, 50)), c.borderWidth, density,
            photo = if (full) art else null, shade = if (full) c.fullCoverShade else 0f,
            photoKey = state.artworkUri?.toString().orEmpty(), photoOpacity = if (full) 1 - state.transparency else 1f,
            photoInsetX = if (full) (c.coverMarginX + c.coverPaddingX).coerceAtLeast(0) else 0,
            photoInsetY = if (full) (c.coverMarginY + c.coverPaddingY).coerceAtLeast(0) else 0))
        val slots = listOf(R.id.widget_top_left, R.id.widget_top_center, R.id.widget_top_right, R.id.widget_bottom_left,
            R.id.widget_bottom_center, R.id.widget_bottom_right, R.id.widget_inline_left, R.id.widget_inline_center,
            R.id.widget_inline_right, R.id.widget_cover_left, R.id.widget_cover_right, R.id.widget_cover_top,
            R.id.widget_cover_bottom, R.id.widget_meta, R.id.widget_labels_top, R.id.widget_labels_bottom, R.id.widget_overlay,
            R.id.widget_overlay_left, R.id.widget_overlay_center, R.id.widget_overlay_right)
        slots.forEach { views.removeAllViews(it) } // Reapply must not accumulate old children.
        listOf(R.id.widget_cover_top, R.id.widget_cover_bottom).forEach { views.setInt(it, "setGravity", gravity(c.alignment)) }
        val verticalGravity = when (c.verticalAlignment) { "top" -> Gravity.TOP; "bottom" -> Gravity.BOTTOM; else -> Gravity.CENTER_VERTICAL }
        for (id in listOf(R.id.widget_meta, R.id.widget_cover_left, R.id.widget_cover_right, R.id.widget_inline_left, R.id.widget_inline_center, R.id.widget_inline_right)) {
            views.setInt(id, "setGravity", verticalGravity or Gravity.CENTER_HORIZONTAL)
        }
        val placement = when {
            tiny && c.buttonsPlacement == "auto" -> "overlay"
            c.buttonsPlacement != "auto" -> c.buttonsPlacement
            ih < 110 && !narrow -> "inline"
            else -> "bottom"
        }
        val coverAt = when {
            full -> "hidden"
            c.coverPlacement != "auto" -> c.coverPlacement
            narrow || spec.type == WidgetLayoutType.SQUARE -> "top"
            else -> "left"
        }
        val visible = c.buttonOrder.filter { id ->
            val pos = c.button(id).position
            pos != "hidden" && (pos != "auto" || when (id) {
                "prev" -> spec.showPrevious; "like" -> spec.showLike; "next" -> !tiny; else -> true
            })
        }
        val requestedButtons = visible.map { id -> c.button(id).size.takeIf { it > 0 } ?: if (id == "play") 36 else 32 }
        val controlsHeight = if (placement in setOf("inline", "overlay")) 0 else
            if (narrow) minOf(3, visible.size) * (requestedButtons.maxOrNull() ?: 0) else requestedButtons.maxOrNull() ?: 0
        val labelReserve = if (c.labelPlacement != "center" || coverAt in setOf("top", "bottom")) {
            WidgetCustomization.labelIds.sumOf { id ->
                val st = c.label(id)
                val show = st.visibility == "show" || (st.visibility == "auto" && when (id) { "title" -> spec.showTitle; "artist" -> spec.showArtist; else -> spec.showLogo })
                if (show) (st.size * context.resources.configuration.fontScale * 1.25f + 2).roundToInt() else 0
            }
        } else 0
        val coverMaxH = (ch - controlsHeight - labelReserve - c.coverMarginY * 2).coerceAtLeast(12)
        val coverMaxW = (cw - c.coverMarginX * 2 - (if (placement == "inline") visible.size * 20 else 0) -
            (if (coverAt in setOf("left", "right")) 36 else 0)).coerceAtLeast(12)
        val initialCover = if (c.coverSize == 0) minOf(128, coverMaxH, coverMaxW) else c.coverSize
        val coverSide = size(minOf(initialCover, coverMaxW, coverMaxH))
        if (coverAt != "hidden") {
            val child = RemoteViews(context.packageName, imageLayout(coverSide))
            val radius = if (c.coverRadius < 0) (spec.radius - minOf(spec.paddingX, spec.paddingY)).coerceAtLeast(0f) else c.coverRadius.toFloat()
            val paddingX = c.coverPaddingX.coerceAtMost(coverSide / 3); val paddingY = c.coverPaddingY.coerceAtMost(coverSide / 3)
            val artSide = (coverSide - 2 * maxOf(paddingX, 0) - 2 * maxOf(paddingY, 0) + 2 * minOf(paddingX, paddingY).coerceAtMost(0)).coerceAtLeast(4)
            child.setImageViewBitmap(R.id.widget_item_bg, surface(coverSide, coverSide, radius, background,
                color(c.coverBorderColor, fg), c.coverBorderWidth, density))
            child.setImageViewBitmap(R.id.widget_item_image, surface(artSide, artSide, (radius - minOf(paddingX, paddingY)).coerceAtLeast(0f), Color.TRANSPARENT,
                Color.TRANSPARENT, 0f, density, photo = art, photoKey = state.artworkUri.toString()))
            // Positive values inset the artwork; negative values push it out past the card edge.
            child.setViewPadding(R.id.widget_item_image, px(paddingX.coerceAtLeast(0)), px(paddingY.coerceAtLeast(0)),
                px(paddingX.coerceAtLeast(0)), px(paddingY.coerceAtLeast(0)))
            applyOffset(child, R.id.widget_item_image, minOf(paddingX, 0), minOf(paddingY, 0))
            if (art == null) {
                child.setImageViewResource(R.id.widget_item_image, R.drawable.ic_xvox_music_note)
                child.setInt(R.id.widget_item_image, "setColorFilter", fg)
                child.setViewPadding(R.id.widget_item_image, px(6), px(6), px(6), px(6))
            }
            val coverSlot = when (coverAt) { "right" -> R.id.widget_cover_right; "top" -> R.id.widget_cover_top; "bottom" -> R.id.widget_cover_bottom; else -> R.id.widget_cover_left }
            views.setViewPadding(coverSlot, px(c.coverMarginX.coerceAtLeast(0)), px(c.coverMarginY.coerceAtLeast(0)),
                px(c.coverMarginX.coerceAtLeast(0)), px(c.coverMarginY.coerceAtLeast(0)))
            applyOffset(views, coverSlot, minOf(c.coverMarginX, 0), minOf(c.coverMarginY, 0))
            views.addView(coverSlot, child)
        }
        fun side(id: String): String {
            val explicit = c.button(id).position
            return if (explicit != "auto") explicit else if (placement == "inline") "right" else when (id) { "prev" -> "left"; "play" -> "center"; else -> "right" }
        }
        fun resolvedButtonSize(id: String): Int {
            val st = c.button(id); val sameZone = visible.count { side(it) == side(id) }.coerceAtLeast(1)
            val allowed = when (placement) {
                "inline" -> (cw - (if (coverAt in setOf("left", "right")) coverSide else 0) - 30).coerceAtLeast(12) / visible.size.coerceAtLeast(1)
                "overlay" -> minOf(cw, ch) / visible.size.coerceAtLeast(1)
                else -> (if (narrow || visible.map(::side).distinct().size == 1) cw else cw / 3) / sameZone
            }
            val wanted = if (st.size == 0) if (id == "play") 36 else 32 else st.size
            return size(minOf(wanted, allowed.coerceAtLeast(12)))
        }
        val labelsAt = if (tiny && c.labelPlacement == "center") "bottom" else c.labelPlacement
        val textWidth = (if (labelsAt == "center") cw - (if (coverAt in setOf("left", "right")) coverSide else 0) -
            (if (placement == "inline") visible.sumOf(::resolvedButtonSize) else 0) else cw).coerceAtLeast(20)
        for (id in WidgetCustomization.labelIds) {
            val style = c.label(id)
            val show = when (style.visibility) { "show" -> true; "hide" -> false; else -> when (id) {
                "title" -> spec.showTitle; "artist" -> spec.showArtist; else -> spec.showLogo && state.showLogo
            } }
            if (!show) continue
            val text = when (id) { "title" -> state.songTitle; "artist" -> state.songArtist; else -> "X" }
            val child = label(context, style, text, textWidth, fg, density)
            applyOffset(child, R.id.widget_label_text, style.offsetX, style.offsetY)
            views.addView(when (labelsAt) { "top" -> R.id.widget_labels_top; "bottom" -> R.id.widget_labels_bottom; else -> R.id.widget_meta }, child)
        }
        for (id in visible) {
            val style = c.button(id)
            val zone = side(id)
            val buttonSize = resolvedButtonSize(id)
            val button = RemoteViews(context.packageName, imageLayout(buttonSize))
            val autoBg = if (full) Color.argb(160, 0, 0, 0) else ColorUtils.setAlphaComponent(fg, 24)
            button.setImageViewBitmap(R.id.widget_item_bg, surface(buttonSize, buttonSize, style.radius.toFloat().coerceAtMost(buttonSize / 2f),
                color(style.background, autoBg), color(style.borderColor, fg), style.borderWidth, density))
            val resource = when (id) { "prev" -> R.drawable.ic_xvox_skip_previous; "next" -> R.drawable.ic_xvox_skip_next;
                "like" -> if (state.isLiked) R.drawable.ic_xvox_heart else R.drawable.ic_xvox_heart_outline;
                else -> if (state.isPlaying) R.drawable.ic_xvox_pause else R.drawable.ic_xvox_play }
            button.setImageViewResource(R.id.widget_item_image, resource)
            button.setInt(R.id.widget_item_image, "setColorFilter", color(style.color, if (id == "like" && state.isLiked) Color.rgb(255, 69, 58) else fg))
            val inset = px(style.padding.coerceAtMost(buttonSize / 3))
            val labelSize = style.labelSize.coerceAtMost((buttonSize / 3).coerceAtLeast(6))
            button.setViewPadding(R.id.widget_item_image, inset, inset, inset, if (style.showLabel) maxOf(inset, px(labelSize + 2)) else inset)
            button.setViewVisibility(R.id.widget_item_label, if (style.showLabel) View.VISIBLE else View.GONE)
            if (style.showLabel) {
                button.setTextViewText(R.id.widget_item_label, when (id) { "prev" -> "Prev"; "next" -> "Next"; "like" -> "Like"; else -> if (state.isPlaying) "Pause" else "Play" })
                button.setTextViewTextSize(R.id.widget_item_label, TypedValue.COMPLEX_UNIT_SP, labelSize.toFloat())
                button.setTextColor(R.id.widget_item_label, color(style.labelColor, fg))
            }
            button.setContentDescription(R.id.widget_item, when (id) { "prev" -> "Previous"; "next" -> "Next"; "like" -> "Like"; else -> "Play or pause" })
            if (interactive) button.setOnClickPendingIntent(R.id.widget_item, action(context, when (id) {
                "prev" -> ACTION_PREVIOUS; "next" -> ACTION_NEXT; "like" -> ACTION_TOGGLE_LIKE; else -> ACTION_PLAY_PAUSE }, id.hashCode()))
            // Buttons float freely: the per-button nudge shifts the control within its zone.
            applyOffset(button, R.id.widget_item, style.offsetX, style.offsetY)
            views.addView(buttonSlot(placement, zone), button)
        }
        if (interactive) views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 105,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        views
    }
    /**
     * Nudges a view by a dp amount that may be negative.
     *
     * View padding clamps at zero, so anything that needs to escape its box has to use a layout
     * margin. `setViewLayoutMargin` exists from API 31; below that the nudge is simply skipped.
     */
    private fun applyOffset(views: RemoteViews, viewId: Int, dx: Int, dy: Int) {
        if (dx == 0 && dy == 0) return
        if (android.os.Build.VERSION.SDK_INT < 31) return
        views.setViewLayoutMargin(viewId, RemoteViews.MARGIN_START, dx.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutMargin(viewId, RemoteViews.MARGIN_END, (-dx).toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutMargin(viewId, RemoteViews.MARGIN_TOP, dy.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutMargin(viewId, RemoteViews.MARGIN_BOTTOM, (-dy).toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }

    private fun buttonSlot(row: String, side: String): Int = when (row) {
        "overlay" -> when (side) { "left" -> R.id.widget_overlay_left; "right" -> R.id.widget_overlay_right; else -> R.id.widget_overlay_center }
        "top" -> when (side) { "left" -> R.id.widget_top_left; "center" -> R.id.widget_top_center; else -> R.id.widget_top_right }
        "inline" -> when (side) { "left" -> R.id.widget_inline_left; "center" -> R.id.widget_inline_center; else -> R.id.widget_inline_right }
        else -> when (side) { "left" -> R.id.widget_bottom_left; "center" -> R.id.widget_bottom_center; else -> R.id.widget_bottom_right }
    }
    private fun label(context: Context, style: WidgetLabelStyle, text: String, width: Int, foreground: Int, density: Float): RemoteViews {
        val layout = when (style.font) { "cinzel" -> R.layout.widget_label_cinzel; "hand" -> R.layout.widget_label_hand; else -> R.layout.widget_label_inter }
        val font = when (style.font) { "cinzel" -> R.font.xvoxcinzeldecorative; "hand" -> R.font.xvoxnothingyoucoulddo; else -> R.font.xvox_inter_semibold }
        val views = RemoteViews(context.packageName, layout)
        views.setTextViewText(R.id.widget_label_text, text)
        views.setTextViewTextSize(R.id.widget_label_text, TypedValue.COMPLEX_UNIT_SP, style.size.toFloat())
        views.setTextColor(R.id.widget_label_text, color(style.color, foreground))
        views.setInt(R.id.widget_label_text, "setGravity", gravity(style.alignment))
        val bg = color(style.background, Color.TRANSPARENT)
        if (bg == Color.TRANSPARENT && style.borderWidth == 0f) views.setViewVisibility(R.id.widget_label_bg, View.GONE)
        else {
            val paint = Paint().apply { textSize = style.size * context.resources.displayMetrics.scaledDensity; typeface = ResourcesCompat.getFont(context, font) }
            val height = ((paint.fontMetrics.descent - paint.fontMetrics.ascent) / density + 2).roundToInt().coerceAtLeast(8)
            views.setImageViewBitmap(R.id.widget_label_bg, surface(width, height, style.radius.toFloat(), bg,
                color(style.borderColor, foreground), style.borderWidth, density))
        }
        return views
    }
    private fun gravity(value: String): Int = Gravity.CENTER_VERTICAL or when (value) { "left" -> Gravity.START; "right" -> Gravity.END; else -> Gravity.CENTER_HORIZONTAL }
    private fun action(context: Context, action: String, code: Int) = PendingIntent.getForegroundService(context, code,
        Intent(context, XvoxPlaybackService::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun color(c: String, fallback: Int): Int = when {
        c.equals("Auto", true) -> fallback
        c.equals("Transparent", true) -> Color.TRANSPARENT
        else -> runCatching { Color.parseColor(c) }.getOrDefault(fallback)
    }
    private fun surface(w: Int, h: Int, radius: Float, fill: Int, border: Int, stroke: Float, density: Float,
        photo: Bitmap? = null, shade: Float = 0f, photoKey: String = "", photoOpacity: Float = 1f, photoInsetX: Int = 0, photoInsetY: Int = 0): Bitmap {
        val key = "$w:$h:$radius:$fill:$border:$stroke:$density:${if (photo != null) photoKey else ""}:$shade:$photoOpacity:$photoInsetX:$photoInsetY"
        images.get(key)?.let { return it }
        val scale = minOf(1f, 512f / (maxOf(w, h) * density))
        val width = (w * density * scale).roundToInt().coerceAtLeast(1); val height = (h * density * scale).roundToInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out); val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val rad = radius.coerceIn(0f, minOf(w, h) / 2f) * density * scale
        val layer = if (photo != null && photoOpacity < 1f) canvas.saveLayerAlpha(rect, (photoOpacity.coerceIn(0f, 1f) * 255).roundToInt()) else -1
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { color = if (photo != null) ColorUtils.setAlphaComponent(fill, 255) else fill }
        canvas.drawRoundRect(rect, rad, rad, paint)
        if (photo != null) {
            val ix = (photoInsetX * density * scale).coerceAtMost(width * .4f)
            val iy = (photoInsetY * density * scale).coerceAtMost(height * .4f)
            val photoRect = RectF(ix, iy, width - ix, height - iy)
            val photoRadius = (rad - minOf(ix, iy)).coerceAtLeast(0f)
            val s = maxOf(photoRect.width() / photo.width, photoRect.height() / photo.height)
            val shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setLocalMatrix(Matrix().apply { setScale(s, s); postTranslate(ix + (photoRect.width() - photo.width * s) / 2, iy + (photoRect.height() - photo.height * s) / 2) })
            paint.shader = shader; canvas.drawRoundRect(photoRect, photoRadius, photoRadius, paint); paint.shader = null
            if (shade > 0) { paint.color = Color.argb((shade * 255).roundToInt(), 0, 0, 0); canvas.drawRoundRect(photoRect, photoRadius, photoRadius, paint) }
        }
        if (layer >= 0) canvas.restoreToCount(layer)
        if (stroke > 0) {
            paint.color = border; paint.style = Paint.Style.STROKE; paint.strokeWidth = (stroke * density * scale).coerceAtLeast(.7f)
            val inset = paint.strokeWidth / 2
            canvas.drawRoundRect(RectF(inset, inset, width - inset, height - inset), rad, rad, paint)
        }
        images.put(key, out)
        return out
    }
    private fun artwork(context: Context, uri: Uri?): Bitmap? {
        if (uri == null) return null
        val key = "source:$uri"; images.get(key)?.let { return it }
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1; while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }?.also {
                images.put(key, it)
                if (colours.get(uri.toString()) == null) {
                    var red = 0L; var green = 0L; var blue = 0L; var n = 0
                    val step = maxOf(1, minOf(it.width, it.height) / 16)
                    for (y in 0 until it.height step step) for (x in 0 until it.width step step) {
                        val pixel = it.getPixel(x, y); red += Color.red(pixel); green += Color.green(pixel); blue += Color.blue(pixel); n++
                    }
                    if (n > 0) colours.put(uri.toString(), Color.rgb((red / n).toInt(), (green / n).toInt(), (blue / n).toInt()))
                }
            }
        }.getOrNull()
    }
}
