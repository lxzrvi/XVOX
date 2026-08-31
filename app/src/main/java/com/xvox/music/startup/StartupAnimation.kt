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

    val logoTypeface = remember {
        ResourcesCompat.getFont(
            context,
            R.font.xvoxcinzeldecorative
        )
    }

    val logoTextSize = with(density) {
        42.sp.toPx()
    }

    val logoColor =
        colors.primaryText.toAndroidColor()

    val paint = remember(
        logoTypeface,
        logoTextSize,
        logoColor
    ) {
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            typeface = logoTypeface
            textSize = logoTextSize
            color = logoColor
            textAlign = Paint.Align.LEFT
        }
    }

    Canvas(
        modifier = Modifier.size(
            width = 320.dp,
            height = 100.dp
        )
    ) {
        val xWidth =
            paint.measureText("X")

        val voxWidth =
            paint.measureText("VOX")

        val completeWidth =
            xWidth + voxWidth

        val initialXPosition =
            size.width / 2f -
                xWidth / 2f

        val finalXPosition =
            size.width / 2f -
                completeWidth / 2f

        val xPosition =
            lerp(
                start = initialXPosition,
                end = finalXPosition,
                fraction = progress
            )

        val finalVoxPosition =
            finalXPosition + xWidth

        val hiddenVoxPosition =
            initialXPosition +
                xWidth * 0.12f

        val voxPosition =
            lerp(
                start = hiddenVoxPosition,
                end = finalVoxPosition,
                fraction = progress
            )

        val metrics = paint.fontMetrics

        val baseline =
            size.height / 2f -
                (
                    metrics.ascent +
                        metrics.descent
                    ) / 2f

        if (progress > 0f) {
            val revealLeft =
                xPosition + xWidth * 0.68f

            clipRect(
                left = revealLeft,
                top = 0f,
                right = size.width,
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

private fun lerp(
    start: Float,
    end: Float,
    fraction: Float
): Float {
    return start +
        (end - start) *
        fraction.coerceIn(0f, 1f)
}

private fun androidx.compose.ui.graphics.Color.toAndroidColor(): Int {
    return android.graphics.Color.argb(
        (alpha * 255f).toInt(),
        (red * 255f).toInt(),
        (green * 255f).toInt(),
        (blue * 255f).toInt()
    )
}
