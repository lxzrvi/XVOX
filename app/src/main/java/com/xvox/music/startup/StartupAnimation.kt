package com.xvox.music.startup

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun StartupAnimation(
    progress: Float
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val density = LocalDensity.current

    val typeface = remember {
        ResourcesCompat.getFont(
            context,
            R.font.xvoxcinzeldecorative
        )
    }

    val textSizePx = with(density) {
        40.sp.toPx()
    }

    val paint = remember(
        typeface,
        colors.primaryText,
        textSizePx
    ) {
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = colors.primaryText.toArgb()
            textSize = textSizePx
            textAlign = Paint.Align.LEFT
            this.typeface = typeface
        }
    }

    Canvas(
        modifier = Modifier.size(
            width = 240.dp,
            height = 80.dp
        )
    ) {
        val xWidth =
            paint.measureText("X")

        val voxWidth =
            paint.measureText("VOX")

        val fullWidth =
            xWidth + voxWidth

        val initialStart =
            size.width / 2f -
                xWidth / 2f

        val finalStart =
            size.width / 2f -
                fullWidth / 2f

        val startX =
            initialStart +
                (
                    finalStart -
                        initialStart
                    ) * progress

        val metrics =
            paint.fontMetrics

        val baseline =
            size.height / 2f -
                (
                    metrics.ascent +
                        metrics.descent
                    ) / 2f

        drawContext.canvas
            .nativeCanvas
            .drawText(
                "X",
                startX,
                baseline,
                paint
            )

        val revealWidth =
            voxWidth * progress

        if (revealWidth > 0f) {
            clipRect(
                left =
                    startX + xWidth,
                top = 0f,
                right =
                    startX +
                        xWidth +
                        revealWidth,
                bottom = size.height
            ) {
                drawContext.canvas
                    .nativeCanvas
                    .drawText(
                        "VOX",
                        startX + xWidth,
                        baseline,
                        paint
                    )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255f).toInt(),
        (red * 255f).toInt(),
        (green * 255f).toInt(),
        (blue * 255f).toInt()
    )
}
