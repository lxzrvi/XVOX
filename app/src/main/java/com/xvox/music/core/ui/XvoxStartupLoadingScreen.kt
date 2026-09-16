package com.xvox.music.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

/**
 * Clean determinate startup screen: centered sleek loading progress line.
 */
@Composable
fun XvoxStartupLoadingScreen(
    progress: Float = 0f,
    stage: String = ""
) {
    val colors = XvoxTheme.colors

    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "startup_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.primaryText.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(shown.coerceIn(0.02f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.primaryAccent)
            )
        }
    }
}
