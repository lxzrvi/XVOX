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
import androidx.compose.runtime.mutableFloatStateOf
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
    RAIL_ZERO,
    RAIL_STAGING,
    RAIL_READY
}

private val StartupRailWidth = 180.dp
private val StartupRailHeight = 4.dp
private val StartupDotIdleGap = 10.dp
private const val StartupDotCount = 5

/**
 * A deliberately paced startup handoff:
 *
 * 1. Three calm left-to-right accent passes cross five muted strokes. A dot never flashes on and
 *    off in place; the accent simply travels to the next stroke.
 * 2. The five strokes widen until they form one uninterrupted muted rail.
 * 3. Accent progress begins at true zero, rests at three repeatable intermediate landmarks, and
 *    only reaches full after the actual bootstrap work reports ready.
 */
@Composable
fun XvoxStartupLoadingScreen(
    readyToEnter: Boolean,
    onSequenceComplete: () -> Unit,
    /** Work remains the gate for the final fill; visual stops intentionally stay deterministic. */
    progress: Float = 0f
) {
    val colors = XvoxTheme.colors
    var phase by remember { mutableStateOf(StartupVisualPhase.DOTS_SEQUENCE) }
    var activeDot by remember { mutableIntStateOf(-1) }
    var stagedProgress by remember { mutableFloatStateOf(0f) }
    val latestReady by rememberUpdatedState(readyToEnter)
    val latestComplete by rememberUpdatedState(onSequenceComplete)

    LaunchedEffect(Unit) {
        phase = StartupVisualPhase.DOTS_SEQUENCE
        activeDot = 0
        stagedProgress = 0f

        // Three intentional passes. There is no intervening unlit beat, which removes the former
        // blink while leaving a clearly readable travelling accent.
        repeat(3) {
            repeat(StartupDotCount) { index ->
                activeDot = index
                delay(132)
            }
            delay(92)
        }
        activeDot = -1

        // Every stroke grows while its spacing closes, yielding one contiguous muted bar.
        phase = StartupVisualPhase.DOTS_TO_RAIL
        delay(440)

        // Keep the empty rail visible for a real beat before any accent enters it.
        phase = StartupVisualPhase.RAIL_ZERO
        stagedProgress = 0f
        delay(180)

        phase = StartupVisualPhase.RAIL_STAGING
        // These are visual landmarks rather than a jittery reflection of incidental startup work.
        // Their pauses make the sequence feel deliberate, while readyToEnter still gates the end.
        val stops = listOf(.24f to 470L, .51f to 440L, .76f to 400L)
        stops.forEach { (stop, travelMs) ->
            stagedProgress = stop
            delay(travelMs)
            delay(160)
        }

        while (!latestReady) delay(80)
        phase = StartupVisualPhase.RAIL_READY
        stagedProgress = 1f
        // A short completed-state pause prevents a cut straight from a moving rail into Home.
        delay(440)
        latestComplete()
    }

    // Referencing work progress keeps the parameter semantically live without allowing fast I/O
    // to erase any of the staged rests above. It can only be used once the sequence is ready.
    val reportedProgress = progress.coerceIn(0f, 1f)
    val railTarget = when (phase) {
        StartupVisualPhase.DOTS_SEQUENCE,
        StartupVisualPhase.DOTS_TO_RAIL,
        StartupVisualPhase.RAIL_ZERO -> 0f
        StartupVisualPhase.RAIL_STAGING -> stagedProgress
        StartupVisualPhase.RAIL_READY -> maxOf(stagedProgress, reportedProgress, 1f)
    }
    val morphingToRail = phase != StartupVisualPhase.DOTS_SEQUENCE

    val dotWidth by animateDpAsState(
        targetValue = if (morphingToRail) StartupRailWidth / StartupDotCount.toFloat() else StartupRailHeight,
        animationSpec = tween(440, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupMutedDotsToRailWidth"
    )
    val dotSpacing by animateDpAsState(
        targetValue = if (morphingToRail) 0.dp else StartupDotIdleGap,
        animationSpec = tween(440, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupMutedDotsToRailGap"
    )
    val dotsAlpha by animateFloatAsState(
        targetValue = when (phase) {
            StartupVisualPhase.DOTS_SEQUENCE, StartupVisualPhase.DOTS_TO_RAIL -> 1f
            else -> 0f
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "startupDotsFade"
    )
    val shownRail by animateFloatAsState(
        targetValue = railTarget,
        animationSpec = tween(
            durationMillis = if (phase == StartupVisualPhase.RAIL_READY) 360 else 300,
            easing = CubicBezierEasing(.16f, 1f, .3f, 1f)
        ),
        label = "startupAccentRailProgress"
    )
    val railAlpha by animateFloatAsState(
        targetValue = if (morphingToRail) 1f else 0f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
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

            // The enclosing rail clip removes anti-aliased seams while the five dots become one
            // bar. Individual dots retain their rounded ends only during the initial pass.
            Row(
                modifier = Modifier
                    .graphicsLayer { alpha = dotsAlpha }
                    .clip(RoundedCornerShape(2.dp)),
                horizontalArrangement = Arrangement.spacedBy(dotSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(StartupDotCount) { index ->
                    val accentIsHere = phase == StartupVisualPhase.DOTS_SEQUENCE && index == activeDot
                    val dotColor = if (accentIsHere) colors.primaryAccent else colors.secondaryText.copy(alpha = .42f)
                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(StartupRailHeight)
                            .then(
                                if (morphingToRail) Modifier
                                else Modifier.clip(RoundedCornerShape(50))
                            )
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
