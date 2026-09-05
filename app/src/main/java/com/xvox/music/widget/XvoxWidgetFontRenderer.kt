package com.xvox.music.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.res.ResourcesCompat
import com.xvox.music.R

object XvoxWidgetFontRenderer {

    fun createLogoBitmap(
        context: Context,
        text: String = "XVOX",
        textColor: Int = 0xFFFFFFFF.toInt(),
        textSizePx: Float = 44f,
        letterSpacing: Float = 0.12f
    ): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = textSizePx
            this.letterSpacing = letterSpacing
            isSubpixelText = true
            runCatching {
                typeface = ResourcesCompat.getFont(context, R.font.xvoxcinzeldecorative)
            }
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val textWidth = paint.measureText(text)
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        val padding = 8
        val width = (textWidth + padding * 2).toInt().coerceAtLeast(1)
        val height = (textHeight + padding * 2).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val x = padding.toFloat()
        val y = padding - fontMetrics.ascent

        canvas.drawText(text, x, y, paint)
        return bitmap
    }
}
