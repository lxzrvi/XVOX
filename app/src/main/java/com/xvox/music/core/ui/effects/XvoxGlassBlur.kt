package com.xvox.music.core.ui.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

val LocalLiveBlurEnabled = compositionLocalOf { true }

@Composable
fun Modifier.xvoxGlass(
    shape: Shape,
    enabled: Boolean = LocalLiveBlurEnabled.current,
    tint: Color = XvoxTheme.colors.card.copy(alpha = 0.62f),
    solidFallback: Color = XvoxTheme.colors.card,
    borderWidth: Dp = 0.75.dp,
    borderColor: Color = Color.White.copy(alpha = 0.14f),
    showSpecularSheen: Boolean = true
): Modifier {
    val colors = XvoxTheme.colors
    return if (enabled) {
        val glassBorderBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                borderColor,
                Color.White.copy(alpha = 0.04f)
            )
        )
        this
            .clip(shape)
            .background(tint)
            .then(
                if (showSpecularSheen) {
                    Modifier.drawBehind {
                        val sheenBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                        drawRect(
                            brush = sheenBrush,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height * 0.45f)
                        )
                    }
                } else {
                    Modifier
                }
            )
            .border(borderWidth, glassBorderBrush, shape)
    } else {
        this
            .clip(shape)
            .background(solidFallback)
            .border(borderWidth, colors.cardBorder, shape)
    }
}

@Composable
fun Modifier.xvoxHeaderGlass(
    enabled: Boolean = LocalLiveBlurEnabled.current
): Modifier {
    val colors = XvoxTheme.colors
    return if (enabled) {
        this.background(colors.background.copy(alpha = 0.72f))
    } else {
        this.background(colors.background)
    }
}
