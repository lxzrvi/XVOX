package com.xvox.music.widget

/** Shape-aware geometry shared by RemoteViews and the real settings preview. Values are in dp. */
data class WidgetLayoutSpec(
    val width: Int, val height: Int, val type: WidgetLayoutType,
    val paddingX: Int, val paddingY: Int, val radius: Float,
    val coverSide: Int, val coverRadius: Float,
    val showTitle: Boolean, val showArtist: Boolean, val showLogo: Boolean,
    val showPrevious: Boolean, val showLike: Boolean
) {
    companion object {
        fun create(width: Int, height: Int, x: Int, y: Int, radius: Int, logo: Boolean, fontScale: Float = 1f): WidgetLayoutSpec {
            val w = width.coerceAtLeast(40); val h = height.coerceAtLeast(40)
            val px = x.coerceIn(0, minOf(32, w / 6))
            val py = y.coerceIn(0, minOf(28, h / 6))
            val innerW = w - 2 * px; val innerH = h - 2 * py
            val type = when {
                w < 110 && h >= 140 -> WidgetLayoutType.VERTICAL
                w < 110 -> WidgetLayoutType.TINY
                h >= w * .85f && h >= 130 -> WidgetLayoutType.SQUARE
                h >= 115 -> WidgetLayoutType.STANDARD
                w >= 260 -> WidgetLayoutType.HORIZONTAL
                else -> WidgetLayoutType.COMPACT
            }
            val previous = when (type) {
                WidgetLayoutType.TINY -> false
                WidgetLayoutType.VERTICAL -> h >= 210
                WidgetLayoutType.COMPACT -> innerW - 44 >= 96
                else -> true
            }
            val like = when (type) {
                WidgetLayoutType.TINY -> false
                WidgetLayoutType.VERTICAL -> h >= 300
                WidgetLayoutType.COMPACT -> innerW - 44 >= 132
                WidgetLayoutType.HORIZONTAL -> w >= 300
                else -> innerW >= 135
            }
            val title = type != WidgetLayoutType.TINY && !(type == WidgetLayoutType.COMPACT && innerH < 44) &&
                !(type == WidgetLayoutType.VERTICAL && h < 190)
            val artist = when (type) {
                WidgetLayoutType.HORIZONTAL -> innerH >= 40 * fontScale
                WidgetLayoutType.SQUARE -> h >= 185
                WidgetLayoutType.STANDARD -> true
                else -> false
            }
            val showLogo = logo && when (type) {
                WidgetLayoutType.HORIZONTAL -> innerH >= 52 * fontScale
                WidgetLayoutType.SQUARE -> h >= 220
                WidgetLayoutType.STANDARD -> true
                else -> false
            }
            val side = when (type) {
                WidgetLayoutType.TINY -> minOf(innerW, innerH)
                WidgetLayoutType.HORIZONTAL -> minOf(48, innerH - 6)
                WidgetLayoutType.COMPACT -> minOf(36, innerH - 6)
                WidgetLayoutType.STANDARD -> minOf(64, innerH - 44)
                WidgetLayoutType.VERTICAL -> minOf(innerW, innerH - 78 - (if (previous) 32 else 0) - (if (like) 28 else 0) - (if (title) 18 else 0))
                WidgetLayoutType.SQUARE -> minOf(innerW, innerH - 44 - (if (title) 18 else 0) - (if (artist) 14 else 0) - (if (showLogo) 14 else 0))
            }.coerceAtLeast(16)
            val outerRadius = radius.toFloat().coerceIn(0f, minOf(w, h) / 2f)
            val innerRadius = (outerRadius - minOf(px, py)).coerceIn(0f, side / 2f)
            return WidgetLayoutSpec(w, h, type, px, py, outerRadius, side, innerRadius, title, artist, showLogo, previous, like)
        }
    }
}

data class WidgetStyle(
    val transparency: Float = .25f, val theme: String = "Dark", val customColor: String = "#000000",
    val showLogo: Boolean = true, val radius: Int = 16, val paddingX: Int = 10, val paddingY: Int = 8,
    val customization: WidgetCustomization = WidgetCustomization()
)
