package com.xvox.music.core.ui.effects

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import androidx.compose.runtime.Composable

/**
 * The hairline Apple puts around every cover: 1 dp, 15 % white on dark pages and 15 % black on
 * light ones, so the edge of an image never disappears into a page of a similar colour.
 * (Ported from BitChord's `thumbnailBorder`.)
 */
@Composable
fun Modifier.xvoxThumbBorder(shape: Shape): Modifier {
    val light = XvoxTheme.colors.isLight
    return border(
        width = 1.dp,
        color = if (light) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f),
        shape = shape
    )
}
