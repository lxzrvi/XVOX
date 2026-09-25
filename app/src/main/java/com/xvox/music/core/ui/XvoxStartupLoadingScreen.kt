package com.xvox.music.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay

/**
 * Startup begins as five sequentially blinking accent dots. They expand and join into the loading
 * rail before determinate progress appears, so the first frame feels alive even before MediaStore
 * work has reported a percentage.
 */
@Composable
fun XvoxStartupLoadingScreen(
    progress: Float = 0f,
    stage: String = ""
) {
    val colors = XvoxTheme.colors
    var morphDots by remember { mutableStateOf(false) }
    var showProgressRail by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1_150)
        morphDots = true
        delay(360)
        showProgressRail = true
    }

    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "startupProgress"
    )
    val dotWidth by animateDpAsState(
        targetValue = if (morphDots) 36.dp else 10.dp,
        animationSpec = tween(430, easing = FastOutSlowInEasing),
        label = "startupDotWidth"
    )
    val dotGap by animateDpAsState(
        targetValue = if (morphDots) 0.dp else 8.dp,
        animationSpec = tween(430, easing = FastOutSlowInEasing),
        label = "startupDotGap"
    )
    val dotsAlpha by animateFloatAsState(
        targetValue = if (showProgressRail) 0f else 1f,
        animationSpec = tween(180),
        label = "startupDotsAlpha"
    )
    val railAlpha by animateFloatAsState(
        targetValue = if (showProgressRail) 1f else 0f,
        animationSpec = tween(220),
        label = "startupRailAlpha"
    )
    val transition = rememberInfiniteTransition(label = "startupDotPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = LinearEasing)),
        label = "startupPulsePhase"
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
                .height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Five discrete accents merge into one rail before it becomes determinate.
            Row(
                modifier = Modifier.graphicsLayer { alpha = dotsAlpha },
                horizontalArrangement = Arrangement.spacedBy(dotGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val distance = ((pulse - index / 5f + 1f) % 1f)
                    val active = 1f - (distance / .20f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(if (morphDots) 3.dp else 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.primaryAccent.copy(alpha = .38f + .62f * active))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .graphicsLayer { alpha = railAlpha }
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.primaryText.copy(alpha = .14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shown.coerceIn(.02f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primaryAccent)
                )
            }
        }
    }
}
