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
    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = XvoxTheme.colors

    val typeface = remember {
        ResourcesCompat.getFont(
            context,
            R.font.xvoxcinzeldecorative
        )
    }

    val textSize = with(density) {
        42.sp.toPx()
    }

    val paint = remember(
        typeface,
        textSize,
        colors.primaryText
    ) {
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            typeface = typeface
            textSize = textSize
            color = colors.primaryText.toAndroidColor()
        }
    }

    Canvas(
        modifier = Modifier.size(
            width = 280.dp,
            height = 90.dp
        )
    ) {
        val xWidth = paint.measureText("X")
        val voxWidth = paint.measureText("VOX")
        val fullWidth = xWidth + voxWidth

        val initialX =
            size.width / 2f -
                xWidth / 2f

        val finalX =
            size.width / 2f -
                fullWidth / 2f

        val xPosition =
            initialX +
                (finalX - initialX) * progress

        val metrics = paint.fontMetrics

        val baseline =
            size.height / 2f -
                (
                    metrics.ascent +
                        metrics.descent
                    ) / 2f

        val voxFinalX =
            xPosition + xWidth

        val voxStartX =
            xPosition +
                xWidth * 0.18f

        val voxPosition =
            voxStartX +
                (
                    voxFinalX -
                        voxStartX
                    ) * progress

        val revealRight =
            voxFinalX +
                voxWidth * progress

        if (progress > 0f) {
            clipRect(
                left = xPosition + xWidth * 0.62f,
                top = 0f,
                right = revealRight,
                bottom = size.height
            ) {
                drawContext.canvas
                    .nativeCanvas
                    .drawText(
                        "VOX",
                        voxPosition,
                        baseline,
                        paint
                    )
            }
        }

        drawContext.canvas
            .nativeCanvas
            .drawText(
                "X",
                xPosition,
                baseline,
                paint
            )
    }
}

private fun androidx.compose.ui.graphics.Color.toAndroidColor(): Int {
    return android.graphics.Color.argb(
        (alpha * 255f).toInt(),
        (red * 255f).toInt(),
        (green * 255f).toInt(),
        (blue * 255f).toInt()
    )
}
