package com.xvox.music.core.ui

import androidx.compose.animation.core.CubicBezierEasing
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
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private enum class StartupVisualPhase {
    DOTS_WAKE,
    RAIL_PRIME,
    DOTS_BRIDGE,
    RAIL_CRUISE,
    DOTS_WAIT,
    RAIL_READY
}

/**
 * A staged startup sequence in which the dots physically gather into the rail before each progress
 * movement. The final rail never completes until the mounted shell is actually ready, so the more
 * expressive motion still protects against revealing a half-built Home screen.
 */
@Composable
fun XvoxStartupLoadingScreen(
    readyToEnter: Boolean,
    onSequenceComplete: () -> Unit
) {
    val colors = XvoxTheme.colors
    var phase by remember { mutableStateOf(StartupVisualPhase.DOTS_WAKE) }
    val latestReady by rememberUpdatedState(readyToEnter)
    val latestComplete by rememberUpdatedState(onSequenceComplete)

    LaunchedEffect(Unit) {
        phase = StartupVisualPhase.DOTS_WAKE
        delay(430)
        phase = StartupVisualPhase.RAIL_PRIME
        delay(620)
        phase = StartupVisualPhase.DOTS_BRIDGE
        delay(390)
        phase = StartupVisualPhase.RAIL_CRUISE
        delay(680)
        phase = StartupVisualPhase.DOTS_WAIT
        // This calm breathing stage is real readiness gating, not a fake percentage pause.
        while (!latestReady) delay(150)
        phase = StartupVisualPhase.RAIL_READY
        delay(760)
        latestComplete()
    }

    val railTarget = when (phase) {
        StartupVisualPhase.RAIL_PRIME -> .24f
        StartupVisualPhase.RAIL_CRUISE -> .70f
        StartupVisualPhase.RAIL_READY -> 1f
        else -> 0f
    }
    val railVisible = railTarget > 0f
    val dotsLead = when (phase) {
        StartupVisualPhase.DOTS_WAKE -> 1f
        StartupVisualPhase.DOTS_BRIDGE -> .82f
        StartupVisualPhase.DOTS_WAIT -> 1f
        else -> 0f
    }
    val dotsAlpha by animateFloatAsState(
        targetValue = if (railVisible) .12f else 1f,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "startupDotsAlpha"
    )
    val dotsScale by animateFloatAsState(
        targetValue = if (railVisible) .62f else 1f,
        animationSpec = tween(330, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupDotsGather"
    )
    val dotsY by animateFloatAsState(
        targetValue = if (railVisible) -7f else 0f,
        animationSpec = tween(330, easing = FastOutSlowInEasing),
        label = "startupDotsLift"
    )
    val shownRail by animateFloatAsState(
        targetValue = railTarget,
        animationSpec = tween(
            durationMillis = if (phase == StartupVisualPhase.RAIL_READY) 680 else 520,
            easing = CubicBezierEasing(.16f, 1f, .3f, 1f)
        ),
        label = "startupRailProgress"
    )
    val railAlpha by animateFloatAsState(
        targetValue = if (railVisible) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "startupRailAlpha"
    )
    val railLift by animateFloatAsState(
        targetValue = if (railVisible) 0f else 7f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "startupRailLift"
    )
    val finalGlow by animateFloatAsState(
        targetValue = if (phase == StartupVisualPhase.RAIL_READY) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "startupReadyGlow"
    )

    val transition = rememberInfiniteTransition(label = "startupMotion")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_180, easing = LinearEasing)),
        label = "startupDotPulse"
    )
    val shimmer by transition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1_520, easing = LinearEasing)),
        label = "startupRailShimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(190.dp)
                .height(34.dp),
            contentAlignment = Alignment.Center
        ) {
            // The dots stay mounted during rail phases, shrink and lift as though they have fed
            // the rail, then return to their resting wave while readiness is being confirmed.
            Row(
                modifier = Modifier.graphicsLayer {
                    alpha = dotsAlpha
                    scaleX = dotsScale
                    scaleY = dotsScale
                    translationY = dotsY
                },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val wave = ((pulse - index / 5f + 1f) % 1f)
                    val active = 1f - (wave / .28f).coerceIn(0f, 1f)
                    val bob = sin((pulse * 2f * PI.toFloat() + index * .88f).toDouble()).toFloat() * 2.2f * dotsLead
                    Box(
                        modifier = Modifier
                            .width(9.dp)
                            .height(9.dp)
                            .graphicsLayer {
                                translationY = bob
                                scaleX = .78f + .22f * active
                                scaleY = scaleX
                            }
                            .clip(RoundedCornerShape(50))
                            .background(colors.primaryAccent.copy(alpha = .28f + .72f * active))
                    )
                }
            }

            // Twin, palette-derived rails create a tangible handoff: a quiet guide rail and a
            // bright progress rail. The tiny travelling highlight makes even a held readiness
            // state feel alive without pretending that progress advanced.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .graphicsLayer {
                        alpha = railAlpha
                        translationY = railLift
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primaryText.copy(alpha = .13f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shownRail.coerceIn(.01f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.primaryAccent.copy(alpha = .86f + .14f * finalGlow))
                )
                if (shownRail > .08f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((shownRail * .16f).coerceIn(.025f, .16f))
                            .height(2.dp)
                            .graphicsLayer {
                                // Kept in pixels intentionally: this is a short highlight rather
                                // than a layout offset, and remains proportional to rail width.
                                translationX = shimmer * 160f
                                alpha = .25f + .45f * finalGlow
                            }
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primaryText.copy(alpha = .62f))
                    )
                }
            }
        }
    }
}
