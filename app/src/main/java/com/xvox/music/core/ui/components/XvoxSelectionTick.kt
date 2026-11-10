// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/components/XvoxSelectionTick.kt
package com.xvox.music.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import androidx.compose.ui.res.painterResource

/**
 * Selection mark shown over the CENTER of a cover (song, playlist, artist): a soft dim over the
 * artwork plus an accent disc with a check that pops in with a small spring.
 */
@Composable
fun BoxScope.XvoxSelectionTick(
    selected: Boolean,
    shape: Shape,
    tickSize: Dp = 22.dp
) {
    val colors = XvoxTheme.colors
    val pop by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (selected) spring(dampingRatio = .55f, stiffness = Spring.StiffnessMedium) else tween(120),
        label = "selectionTick"
    )
    if (pop <= 0.001f) return
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(shape)
            .graphicsLayer { alpha = pop.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = .34f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(tickSize)
                .graphicsLayer { scaleX = pop; scaleY = pop }
                .clip(CircleShape)
                .background(colors.primaryAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_check),
                contentDescription = "Selected",
                tint = colors.background,
                modifier = Modifier.size(tickSize * .56f)
            )
        }
    }
}
