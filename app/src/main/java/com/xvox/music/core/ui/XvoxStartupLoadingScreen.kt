package com.xvox.music.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay

private enum class StartupVisualPhase {
    DOTS_ONE, BAR_THIRTY, DOTS_TWO, BAR_SIXTY, DOTS_THREE, BAR_FULL
}

/**
 * A deliberately staged startup sequence.  The visual rail communicates calm progress without
 * exposing implementation-specific loading percentages, while the final full rail stays gated on
 * real shell preparation.
 */
@Composable
fun XvoxStartupLoadingScreen(
    readyToEnter: Boolean,
    onSequenceComplete: () -> Unit
) {
    val colors = XvoxTheme.colors
    var phase by remember { mutableStateOf(StartupVisualPhase.DOTS_ONE) }
    val latestReady by rememberUpdatedState(readyToEnter)
    val latestComplete by rememberUpdatedState(onSequenceComplete)

    LaunchedEffect(Unit) {
        phase = StartupVisualPhase.DOTS_ONE
        delay(560)
        phase = StartupVisualPhase.BAR_THIRTY
        delay(620)
        phase = StartupVisualPhase.DOTS_TWO
        delay(520)
        phase = StartupVisualPhase.BAR_SIXTY
        delay(620)
        phase = StartupVisualPhase.DOTS_THREE
        // Background library/player/layout preparation is allowed to finish here.  The dots keep
        // moving rather than exposing a real percent or jumping into a half-built Home screen.
        while (!latestReady) delay(180)
        phase = StartupVisualPhase.BAR_FULL
        delay(620)
        latestComplete()
    }

    val railTarget = when (phase) {
        StartupVisualPhase.BAR_THIRTY -> .30f
        StartupVisualPhase.BAR_SIXTY -> .60f
        StartupVisualPhase.BAR_FULL -> 1f
        else -> 0f
    }
    val showRail = railTarget > 0f
    val shownRail by animateFloatAsState(
        targetValue = railTarget,
        animationSpec = tween(460, easing = FastOutSlowInEasing),
        label = "stagedStartupRail"
    )
    val dotsAlpha by animateFloatAsState(
        targetValue = if (showRail) 0f else 1f,
        animationSpec = tween(170),
        label = "stagedStartupDotsAlpha"
    )
    val railAlpha by animateFloatAsState(
        targetValue = if (showRail) 1f else 0f,
        animationSpec = tween(180),
        label = "stagedStartupRailAlpha"
    )
    val transition = rememberInfiniteTransition(label = "stagedStartupDots")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_050, easing = LinearEasing)),
        label = "stagedStartupPulse"
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
                .height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.graphicsLayer { alpha = dotsAlpha },
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val distance = ((pulse - index / 5f + 1f) % 1f)
                    val active = 1f - (distance / .22f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.primaryAccent.copy(alpha = .35f + .65f * active))
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
                        .fillMaxWidth(shownRail.coerceIn(.01f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primaryAccent)
                )
            }
        }
    }
}
