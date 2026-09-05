package com.xvox.music.core.ui.effects

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
    tint: Color = XvoxTheme.colors.card.copy(alpha = 0.72f),
    solidFallback: Color = XvoxTheme.colors.card,
    borderWidth: Dp = 0.6.dp,
    borderColor: Color = Color.White.copy(alpha = 0.12f)
): Modifier {
    val colors = XvoxTheme.colors
    return if (enabled) {
        val glassBorderBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                borderColor,
                Color.White.copy(alpha = 0.04f)
            )
        )
        this
            .clip(shape)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(20.dp)
                } else {
                    Modifier
                }
            )
            .background(tint)
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
        this
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(16.dp)
                } else {
                    Modifier
                }
            )
            .background(colors.background.copy(alpha = 0.75f))
    } else {
        this.background(colors.background)
    }
}
