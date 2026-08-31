package com.xvox.music.startup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun StartupAnimation(
    progress: Float
) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current

    val textSizePx = with(density) {
        40.sp.toPx()
    }

    Canvas(
        modifier = Modifier.size(
            width = 240.dp,
            height = 80.dp
        )
    ) {
        val paint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = colors.primaryText.toArgb()
            textSize = textSizePx
            textAlign = android.graphics.Paint.Align.LEFT
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SERIF,
                android.graphics.Typeface.NORMAL
            )
        }

        val xWidth = paint.measureText("X")
        val voxWidth = paint.measureText("VOX")
        val fullWidth = xWidth + voxWidth

        val initialStart =
            size.width / 2f - xWidth / 2f

        val finalStart =
            size.width / 2f - fullWidth / 2f

        val startX =
            initialStart +
                (finalStart - initialStart) *
                progress

        val metrics = paint.fontMetrics

        val baseline =
            size.height / 2f -
                (metrics.ascent + metrics.descent) / 2f

        drawContext.canvas.nativeCanvas.drawText(
            "X",
            startX,
            baseline,
            paint
        )

        val revealWidth =
            voxWidth * progress

        clipRect(
            left = startX + xWidth,
            top = 0f,
            right = startX + xWidth + revealWidth,
            bottom = size.height
        ) {
            drawContext.canvas.nativeCanvas.drawText(
                "VOX",
                startX + xWidth,
                baseline,
                paint
            )
        }
    }
}

private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
