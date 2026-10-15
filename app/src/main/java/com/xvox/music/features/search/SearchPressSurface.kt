package com.xvox.music.features.search

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xvox.music.core.ui.effects.xvoxSongPress

/**
 * Search-only long-press treatment. The interaction target remains the complete result, while
 * only its material frame dips inward; artwork, titles, and metadata stay at their exact rendered
 * size instead of looking as if the whole result was zoomed down independently.
 */
@Composable
internal fun SearchPressSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    fill: Color,
    border: Color,
    borderWidth: Dp = .8.dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var framePressed by remember { mutableStateOf(false) }
    val frameScale by animateFloatAsState(
        targetValue = if (framePressed) .96f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "searchResultFramePress"
    )

    Box(
        modifier = modifier.xvoxSongPress(
            onClick = onClick,
            onLongClick = onLongClick,
            // Keep the gesture/semantics on this outer result, but leave its child content at 1x.
            pressedScale = 1f,
            onPressedChange = { framePressed = it }
        )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = frameScale
                    scaleY = frameScale
                }
                .clip(shape)
                .background(fill)
                .border(borderWidth, border, shape)
        )
        content()
    }
}
