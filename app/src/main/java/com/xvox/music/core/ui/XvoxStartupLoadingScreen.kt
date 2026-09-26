package com.xvox.music.core.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
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
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private enum class StartupVisualPhase {
    DOTS_WAKE,
    RAIL_PRIME,
    RAIL_CRUISE,
    RAIL_READY
}

private val StartupRailWidth = 180.dp
private val StartupRailHeight = 4.dp
private const val StartupDotCount = 5

/**
 * A staged startup sequence in which five tiny dots physically widen into one loading rail before
 * each progress movement. The final rail never completes until the mounted shell is actually
 * ready, so the expressive motion still protects against revealing a half-built Home screen.
 */
@Composable
fun XvoxStartupLoadingScreen(
    readyToEnter: Boolean,
    onSequenceComplete: () -> Unit,
    /** Real monotonic startup work progress; the accent rail follows it without a visual queue. */
    progress: Float = 0f
) {
    val colors = XvoxTheme.colors
    var phase by remember { mutableStateOf(StartupVisualPhase.DOTS_WAKE) }
    val latestReady by rememberUpdatedState(readyToEnter)
    val latestComplete by rememberUpdatedState(onSequenceComplete)

    LaunchedEffect(Unit) {
        phase = StartupVisualPhase.DOTS_WAKE
        delay(430)
        phase = StartupVisualPhase.RAIL_PRIME
        // Let the five dots finish their physical widening before the live rail takes over.
        delay(330)
        phase = StartupVisualPhase.RAIL_CRUISE
        // This calm rail stage is real readiness gating, not a fake percentage pause.
        while (!latestReady) delay(150)
        phase = StartupVisualPhase.RAIL_READY
        delay(620)
        latestComplete()
    }

    val reportedProgress = progress.coerceIn(.04f, .98f)
    val railVisible = phase != StartupVisualPhase.DOTS_WAKE
    // The rail takes the app's real monotonic progress immediately. Only final completion is held
    // behind actual shell readiness, so guide and accent never visually lag reported work.
    val railTarget = if (phase == StartupVisualPhase.RAIL_READY) 1f else if (railVisible) reportedProgress else 0f
    val dotsLead = if (phase == StartupVisualPhase.DOTS_WAKE) 1f else 0f

    // A dot is exactly as tall as the accent rail. During a rail phase all five dots widen and
    // their gaps close, producing a real circle-to-pill-to-bar morph rather than two unrelated
    // loading indicators fading over each other.
    val dotWidth by animateDpAsState(
        targetValue = if (railVisible) 36.dp else StartupRailHeight,
        animationSpec = tween(320, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupDotToRailWidth"
    )
    val dotSpacing by animateDpAsState(
        targetValue = if (railVisible) 0.dp else 10.dp,
        animationSpec = tween(320, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupDotToRailGap"
    )
    val dotsMorph by animateFloatAsState(
        targetValue = if (railVisible) 1f else 0f,
        animationSpec = tween(320, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupDotMorph"
    )
    val dotsAlpha by animateFloatAsState(
        targetValue = when (phase) {
            StartupVisualPhase.RAIL_PRIME -> .55f
            StartupVisualPhase.RAIL_CRUISE, StartupVisualPhase.RAIL_READY -> 0f
            else -> 1f
        },
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "startupDotsAlpha"
    )
    val shownRail by animateFloatAsState(
        targetValue = railTarget,
        animationSpec = tween(
            durationMillis = if (phase == StartupVisualPhase.RAIL_READY) 420 else 120,
            easing = CubicBezierEasing(.16f, 1f, .3f, 1f)
        ),
        label = "startupRailProgress"
    )
    val railAlpha by animateFloatAsState(
        targetValue = if (railVisible) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "startupRailAlpha"
    )
    val finalGlow by animateFloatAsState(
        targetValue = if (phase == StartupVisualPhase.RAIL_READY) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "startupReadyGlow"
    )

    val transition = rememberInfiniteTransition(label = "startupMotion")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_180, easing = LinearEasing)),
        label = "startupDotPulse"
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
            // The morph row stays mounted over the rail's exact centre. At the end of the widening
            // animation its five adjacent pills equal the rail's complete width and height.
            Row(
                modifier = Modifier.graphicsLayer { alpha = dotsAlpha },
                horizontalArrangement = Arrangement.spacedBy(dotSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(StartupDotCount) { index ->
                    val wave = ((pulse - index / StartupDotCount.toFloat() + 1f) % 1f)
                    val active = 1f - (wave / .28f).coerceIn(0f, 1f)
                    val bob = sin((pulse * 2f * PI.toFloat() + index * .88f).toDouble()).toFloat() *
                        1.15f * dotsLead * (1f - dotsMorph)
                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(StartupRailHeight)
                            .graphicsLayer {
                                translationY = bob
                                scaleY = .86f + .14f * active
                            }
                            .clip(RoundedCornerShape(50))
                            .background(colors.primaryAccent.copy(alpha = .34f + .66f * active))
                    )
                }
            }

            // Guide and accent share the same progress value, so the line and its active fill
            // advance in one frame with no separate delayed catch-up animation.
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
                        .height(1.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colors.primaryText.copy(alpha = .16f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shownRail.coerceIn(.001f, 1f))
                        .height(StartupRailHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primaryAccent.copy(alpha = .88f + .12f * finalGlow))
                )
            }
        }
    }
}
