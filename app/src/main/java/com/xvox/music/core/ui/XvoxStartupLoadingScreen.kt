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
import androidx.compose.foundation.layout.offset
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
    DOTS_BLINK,
    DOTS_FILL,
    DOTS_TO_HALF_RAIL,
    RAIL_CRUISE,
    RAIL_READY
}

private val StartupRailWidth = 180.dp
private val StartupRailHeight = 4.dp
private val StartupHalfRailWidth = 90.dp
private val StartupDotIdleGap = 10.dp
private const val StartupDotCount = 5

/**
 * Five bar-height dots provide the opening rhythm, then become the first 50% of the live loading
 * rail.  After that hand-off, the same rail follows the app's actual startup progress without a
 * separate delayed queue, and it completes only when the Home shell is genuinely ready.
 */
@Composable
fun XvoxStartupLoadingScreen(
    readyToEnter: Boolean,
    onSequenceComplete: () -> Unit,
    /** Real monotonic startup work progress; the accent rail follows it once the dot intro ends. */
    progress: Float = 0f
) {
    val colors = XvoxTheme.colors
    var phase by remember { mutableStateOf(StartupVisualPhase.DOTS_BLINK) }
    var blinkOn by remember { mutableStateOf(false) }
    var filledDotCount by remember { mutableIntStateOf(0) }
    val latestReady by rememberUpdatedState(readyToEnter)
    val latestComplete by rememberUpdatedState(onSequenceComplete)

    LaunchedEffect(Unit) {
        phase = StartupVisualPhase.DOTS_BLINK
        blinkOn = false
        filledDotCount = 0
        // Two calm, complete blinks—not an endless pulse—make the hand-off predictable.
        repeat(2) {
            delay(150)
            blinkOn = true
            delay(150)
            blinkOn = false
        }

        phase = StartupVisualPhase.DOTS_FILL
        repeat(StartupDotCount) { index ->
            delay(112)
            filledDotCount = index + 1
        }
        delay(76)

        // The five now-accent dots retain their order and widen into adjacent segments. Together
        // they become exactly the first half of the 180dp loading rail.
        phase = StartupVisualPhase.DOTS_TO_HALF_RAIL
        delay(360)
        phase = StartupVisualPhase.RAIL_CRUISE

        while (!latestReady) delay(100)
        phase = StartupVisualPhase.RAIL_READY
        delay(620)
        latestComplete()
    }

    val reportedProgress = progress.coerceIn(.04f, .98f)
    val morphingToRail = phase == StartupVisualPhase.DOTS_TO_HALF_RAIL ||
        phase == StartupVisualPhase.RAIL_CRUISE ||
        phase == StartupVisualPhase.RAIL_READY
    val railVisible = phase == StartupVisualPhase.DOTS_TO_HALF_RAIL ||
        phase == StartupVisualPhase.RAIL_CRUISE ||
        phase == StartupVisualPhase.RAIL_READY

    // Dot morph establishes an honest 50% visual milestone. From there the rail uses real work
    // progress directly, never a simulated catch-up percentage.
    val railTarget = when (phase) {
        StartupVisualPhase.DOTS_TO_HALF_RAIL -> .50f
        StartupVisualPhase.RAIL_CRUISE -> maxOf(.50f, reportedProgress)
        StartupVisualPhase.RAIL_READY -> 1f
        else -> 0f
    }

    val dotWidth by animateDpAsState(
        targetValue = if (morphingToRail) StartupHalfRailWidth / StartupDotCount.toFloat() else StartupRailHeight,
        animationSpec = tween(350, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupDotsToHalfRailWidth"
    )
    val dotSpacing by animateDpAsState(
        targetValue = if (morphingToRail) 0.dp else StartupDotIdleGap,
        animationSpec = tween(350, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupDotsToHalfRailGap"
    )
    val dotRowOffset by animateDpAsState(
        // A centered 90dp row needs one half-row left shift to become the rail's leading half.
        targetValue = if (morphingToRail) -(StartupHalfRailWidth / 2f) else 0.dp,
        animationSpec = tween(350, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
        label = "startupDotsToHalfRailPosition"
    )
    val dotsAlpha by animateFloatAsState(
        targetValue = when (phase) {
            StartupVisualPhase.RAIL_CRUISE, StartupVisualPhase.RAIL_READY -> 0f
            else -> 1f
        },
        animationSpec = tween(170, easing = FastOutSlowInEasing),
        label = "startupDotsAlpha"
    )
    val shownRail by animateFloatAsState(
        targetValue = railTarget,
        animationSpec = tween(
            durationMillis = if (phase == StartupVisualPhase.RAIL_READY) 420 else 90,
            easing = CubicBezierEasing(.16f, 1f, .3f, 1f)
        ),
        label = "startupRailProgress"
    )
    val railAlpha by animateFloatAsState(
        targetValue = if (railVisible) 1f else 0f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "startupRailAlpha"
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
            // Guide and accent share one rail target. The guide is visible as dots turn into the
            // first half, so there is never an unrelated indicator appearing a beat later.
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
                        .background(colors.primaryAccent)
                )
            }

            Row(
                modifier = Modifier
                    .offset(x = dotRowOffset)
                    .graphicsLayer { alpha = dotsAlpha },
                horizontalArrangement = Arrangement.spacedBy(dotSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(StartupDotCount) { index ->
                    val filled = index < filledDotCount
                    val dotAlphaTarget = when (phase) {
                        StartupVisualPhase.DOTS_BLINK -> if (blinkOn) 1f else .28f
                        StartupVisualPhase.DOTS_FILL -> if (filled) 1f else .24f
                        else -> 1f
                    }
                    val dotAlpha by animateFloatAsState(
                        targetValue = dotAlphaTarget,
                        animationSpec = tween(110, easing = FastOutSlowInEasing),
                        label = "startupDotFill$index"
                    )
                    // No scale animation: every dot is exactly the rail's 4dp height at all times.
                    // During the ordered fill, untouched dots are theme-muted rather than merely
                    // faint accent copies, so the accent visibly travels left-to-right.
                    val dotColor = if (phase == StartupVisualPhase.DOTS_FILL && !filled) {
                        colors.secondaryText.copy(alpha = .34f)
                    } else {
                        colors.primaryAccent.copy(alpha = dotAlpha)
                    }
                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(StartupRailHeight)
                            .clip(RoundedCornerShape(50))
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
