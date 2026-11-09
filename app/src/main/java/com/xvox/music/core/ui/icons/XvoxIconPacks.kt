// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/icons/XvoxIconPacks.kt
package com.xvox.music.core.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource as basePainterResource
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

/**
 * App-wide icon pack + style. Every `painterResource(R.drawable.ic_xvox_*)` in the app is routed
 * through [painterResource] below, so one Settings choice restyles every icon at once.
 */
object XvoxIconPrefs {
    /** monks (bundled Icon Monks) | filled | outlined | rounded | sharp | twotone */
    var pack: String by mutableStateOf("monks")
    /** thin | normal | fill | bold */
    var style: String by mutableStateOf("normal")

    val packs = listOf(
        "monks" to "Icon Monks",
        "rounded" to "Rounded",
        "outlined" to "Outlined",
        "sharp" to "Sharp",
        "twotone" to "Two-tone",
        "filled" to "Filled"
    )
    val styles = listOf("thin" to "Thin", "normal" to "Normal", "fill" to "Fill", "bold" to "Bold")
}

// ic_xvox_<key> -> Material icon name. Anything missing/unresolvable falls back to the bundled icon.
private val MaterialNames = mapOf(
    "add" to "Add", "plus" to "Add", "arrow_left" to "ArrowBack", "artist" to "Person",
    "bluetooth" to "Bluetooth", "caret_right" to "ChevronRight", "check" to "Check",
    "chevron_down" to "KeyboardArrowDown", "close" to "Close", "collapse" to "UnfoldLess",
    "copy" to "ContentCopy", "crossfade" to "SwapHoriz", "delete" to "Delete", "disc" to "Album",
    "done_all" to "DoneAll", "download" to "Download", "drag_dots" to "DragIndicator",
    "edit" to "Edit", "equalizer" to "Equalizer", "folder" to "Folder", "fullscreen" to "Fullscreen",
    "fullscreen_exit" to "FullscreenExit", "headset" to "Headphones", "heart" to "Favorite",
    "heart_outline" to "FavoriteBorder", "home" to "Home", "info" to "Info", "lyrics" to "Lyrics",
    "lyrics_add" to "PlaylistAdd", "lyrics_file" to "Description", "microphone" to "Mic",
    "microphone_outline" to "MicNone", "microphone_stage" to "Mic", "more" to "MoreVert",
    "music_note" to "MusicNote", "mute" to "VolumeOff", "pause" to "Pause", "phone" to "PhoneAndroid",
    "play" to "PlayArrow", "playlist" to "PlaylistPlay", "queue" to "QueueMusic", "recent" to "History",
    "refresh" to "Refresh", "repeat" to "Repeat", "repeat_one" to "RepeatOne", "resize" to "OpenInFull",
    "search" to "Search", "settings" to "Settings", "share" to "Share", "shuffle" to "Shuffle",
    "skip_next" to "SkipNext", "skip_previous" to "SkipPrevious", "sparkle" to "AutoAwesome",
    "split" to "CallSplit", "star" to "Star", "timer" to "Timer", "undo" to "Undo", "user" to "Person",
    "volume_high" to "VolumeUp", "volume_low" to "VolumeDown", "waveform" to "GraphicEq"
)

private val cache = ConcurrentHashMap<String, Any>()
private object Missing

private fun materialVector(family: String, name: String): ImageVector? {
    val key = "$family/$name"
    val hit = cache[key]
    if (hit != null) return hit as? ImageVector
    val found = runCatching {
        val holder = Class.forName("androidx.compose.material.icons.Icons\$" + family.replaceFirstChar { it.uppercase() }
            .let { if (it == "Twotone") "TwoTone" else it })
        val instance = holder.getField("INSTANCE").get(null)
        val cls = Class.forName("androidx.compose.material.icons.$family.${name}Kt")
        cls.getMethod("get$name", holder).invoke(null, instance) as? ImageVector
    }.getOrNull()
    cache[key] = found ?: Missing
    return found
}

/** Draws [delegate] several times around its centre: a cheap, crisp "bold" for any glyph. */
private class BoldPainter(private val delegate: Painter, private val spread: Float) : Painter() {
    private var a = 1f
    private var f: ColorFilter? = null
    override val intrinsicSize: Size get() = delegate.intrinsicSize
    override fun applyAlpha(alpha: Float): Boolean { a = alpha; return true }
    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean { f = colorFilter; return true }
    override fun DrawScope.onDraw() {
        val r = size.minDimension * spread
        val steps = 8
        for (i in 0 until steps) {
            val ang = (Math.PI * 2 * i / steps).toFloat()
            translate(cos(ang) * r, sin(ang) * r) { with(delegate) { draw(size, a, f) } }
        }
        with(delegate) { draw(size, a, f) }
    }
}

private class FadedPainter(private val delegate: Painter, private val factor: Float) : Painter() {
    private var a = 1f
    private var f: ColorFilter? = null
    override val intrinsicSize: Size get() = delegate.intrinsicSize
    override fun applyAlpha(alpha: Float): Boolean { a = alpha; return true }
    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean { f = colorFilter; return true }
    override fun DrawScope.onDraw() {
        // Thin: slightly smaller glyph at reduced weight, centred.
        val s = size * factor
        translate((size.width - s.width) / 2f, (size.height - s.height) / 2f) {
            with(delegate) { draw(s, a * .78f, f) }
        }
    }
}

/**
 * Drop-in replacement for androidx's painterResource. Non-icon drawables pass straight through.
 */
@Composable
fun painterResource(id: Int): Painter {
    val context = LocalContext.current
    val entry = remember(id) { runCatching { context.resources.getResourceEntryName(id) }.getOrNull() }
    val base = basePainterResource(id)
    if (entry == null || !entry.startsWith("ic_xvox_")) return base

    val pack = XvoxIconPrefs.pack
    val style = XvoxIconPrefs.style
    val key = entry.removePrefix("ic_xvox_")

    // Thin / Fill steer Material packs to their outline / filled families.
    val family = if (pack == "monks") null else when (style) {
        "thin" -> "outlined"
        "fill" -> "filled"
        else -> pack
    }
    val materialName = MaterialNames[key]
    val vector = if (family != null && materialName != null) {
        // A "liked" heart stays distinguishable from the outline heart in every family.
        val fam = if (key == "heart" || key == "star") "filled" else family
        materialVector(fam, materialName)
    } else null
    val painter = if (vector != null) rememberVectorPainter(vector) else base

    return remember(painter, style, vector) {
        when {
            style == "bold" -> BoldPainter(painter, .018f)
            style == "thin" && vector == null -> FadedPainter(painter, .94f)
            else -> painter
        }
    }
}
