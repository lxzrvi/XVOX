package com.xvox.music.core.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableIntStateOf
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
    DOTS_SEQUENCE,
    DOTS_TO_RAIL,
    RAIL_CRUISE,
    RAIL_READY
}

private val StartupRailWidth = 180.dp
private val StartupRailHeight = 4.dp
private val StartupDotIdleGap = 10.dp
private const val StartupDotCount = 5

/**
 * Startup begins with five muted, bar-height dots blinking one-by-one for three rounds. The same
 * dots then widen into the neutral guide rail; only after that handoff does the accent fill grow
 * smoothly from zero through the real startup work and finally readiness.
 */
@Composable
fun XvoxStartupLoadingScreen(
    readyToEnter: Boolean,
    onSequenceComplete: () -> Unit,
    /** Real monotonic startup work progress; the accent rail follows it after the dot intro. */
    progress: Float = 0f
) {
    val colors = XvoxTheme.colors
    var phase by remember { mutableStateOf(StartupVisualPhase.DOTS_SEQUENCE) }
    var activeDot by remember { mutableIntStateOf(-1) }
    var activeDotLit by remember { mutableStateOf(false) }
    val latestReady by rememberUpdatedState(readyToEnter)
    val latestComplete by rememberUpdatedState(onSequenceComplete)

    LaunchedEffect(Unit) {
        phase = StartupVisualPhase.DOTS_SEQUENCE
        activeDot = -1
        activeDotLit = false
        // Three deliberate left-to-right rounds. Each dot is only a rail-height stroke—there is
        // no circle-scale pulse and no accent fill before the rail itself exists.
        repeat(3) {
            repeat(StartupDotCount) { index ->
                activeDot = index
                activeDotLit = true
                delay(96)
                activeDotLit = false
                delay(62)
            }
        }
        activeDot = -1

        // The unchanged muted dots widen until their five adjoining segments are one guide rail.
        phase = StartupVisualPhase.DOTS_TO_RAIL
        delay(420)
        phase = StartupVisualPhase.RAIL_CRUISE

        while (!latestReady) delay(100)
        phase = StartupVisualPhase.RAIL_READY
        delay(620)
        latestComplete()
    }

    val reportedProgress = progress.coerceIn(0f, .985f)
    val morphingToRail = phase != StartupVisualPhase.DOTS_SEQUENCE
    val railVisible = morphingToRail
    val railTarget = when (phase) {
        StartupVisualPhase.DOTS_SEQUENCE, StartupVisualPhase.DOTS_TO_RAIL -> 0f
        StartupVisualPhase.RAIL_CRUISE -> reportedProgress
        StartupVisualPhase.RAIL_READY -> 1f
    }

    val dotWidth by animateDpAsState(
        targetValue = if (morphingToRail) StartupRailWidth / StartupDotCount.toFloat() else StartupRailHeight,
        animationSpec = tween(420, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupMutedDotsToRailWidth"
    )
    val dotSpacing by animateDpAsState(
        targetValue = if (morphingToRail) 0.dp else StartupDotIdleGap,
        animationSpec = tween(420, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupMutedDotsToRailGap"
    )
    val dotsAlpha by animateFloatAsState(
        targetValue = if (phase == StartupVisualPhase.DOTS_TO_RAIL) 1f
        else if (phase == StartupVisualPhase.RAIL_CRUISE || phase == StartupVisualPhase.RAIL_READY) 0f
        else 1f,
        animationSpec = tween(230, easing = FastOutSlowInEasing),
        label = "startupDotsFade"
    )
    val shownRail by animateFloatAsState(
        targetValue = railTarget,
        // The accent begins at exactly zero and is allowed a calm catch-up rather than a jump
        // if library work has already progressed while the dot sequence was playing.
        animationSpec = tween(
            durationMillis = if (phase == StartupVisualPhase.RAIL_READY) 480 else 520,
            easing = CubicBezierEasing(.16f, 1f, .3f, 1f)
        ),
        label = "startupAccentRailProgress"
    )
    val railAlpha by animateFloatAsState(
        targetValue = if (railVisible) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "startupGuideRailAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(StartupRailWidth)
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StartupRailHeight)
                    .graphicsLayer { alpha = railAlpha },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StartupRailHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.secondaryText.copy(alpha = .30f))
                )
                // Do not force a one-pixel placeholder: the accent fill is truly absent at zero
                // and starts growing only after the muted-dot rail has finished morphing.
                if (shownRail > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(shownRail.coerceIn(0f, 1f))
                            .height(StartupRailHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primaryAccent)
                    )
                }
            }

            Row(
                modifier = Modifier.graphicsLayer { alpha = dotsAlpha },
                horizontalArrangement = Arrangement.spacedBy(dotSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(StartupDotCount) { index ->
                    val alphaTarget = if (phase == StartupVisualPhase.DOTS_SEQUENCE) {
                        if (index == activeDot && activeDotLit) 1f else .30f
                    } else {
                        .42f
                    }
                    val dotAlpha by animateFloatAsState(
                        targetValue = alphaTarget,
                        animationSpec = tween(90, easing = FastOutSlowInEasing),
                        label = "startupSequentialDot$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(StartupRailHeight)
                            .clip(RoundedCornerShape(50))
                            .background(colors.secondaryText.copy(alpha = dotAlpha))
                    )
                }
            }
        }
    }
}
