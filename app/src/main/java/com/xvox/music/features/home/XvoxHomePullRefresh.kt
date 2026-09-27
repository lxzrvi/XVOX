package com.xvox.music.features.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.launch
import kotlin.math.min

private val HomeRefreshTrigger = 76.dp

/**
 * Pulling at a Home page's true top consumes only the otherwise-unconsumed downward gesture. The
 * LazyColumn remains at its existing item/order while its painted content stretches down beneath
 * the shell Header; releasing past the threshold refreshes that same page.
 */
@Stable
class XvoxHomePullRefreshState internal constructor(
    val pullModifier: Modifier,
    val contentTranslationPx: Float,
    val revealProgress: Float,
    val refreshing: Boolean
)

@Composable
fun rememberXvoxHomePullRefreshState(
    listState: LazyListState,
    refreshing: Boolean,
    enabled: Boolean = true,
    onRefresh: () -> Unit
): XvoxHomePullRefreshState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val triggerPx = with(density) { HomeRefreshTrigger.toPx() }
    val onRefreshState = rememberUpdatedState(onRefresh)
    var pullDistancePx by remember(listState) { mutableFloatStateOf(0f) }

    fun animatePullTo(target: Float) {
        val start = pullDistancePx
        scope.launch {
            animate(
                initialValue = start,
                targetValue = target.coerceAtLeast(0f),
                animationSpec = tween(220)
            ) { value, _ -> pullDistancePx = value }
        }
    }

    val connection = remember(listState, refreshing, enabled, triggerPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // A returning/upward drag first collapses the temporary stretch before it scrolls
                // the library, so cards never jump to a different position under the finger.
                if (pullDistancePx > 0f && available.y < 0f) {
                    val consumedMagnitude = min(pullDistancePx, -available.y)
                    pullDistancePx = (pullDistancePx - consumedMagnitude).coerceAtLeast(0f)
                    return Offset(0f, -consumedMagnitude)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val isAtTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (enabled && !refreshing && isAtTop && available.y > 0f) {
                    // Dampen the final part of the pull so the icon has time to form from a point
                    // instead of popping at a fixed overscroll distance.
                    val next = pullDistancePx + available.y * .56f
                    pullDistancePx = next.coerceAtMost(triggerPx * 1.45f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistancePx <= 0f) return Velocity.Zero
                if (enabled && !refreshing && pullDistancePx >= triggerPx) {
                    onRefreshState.value()
                    // Hold a compact spinner lane while the real refresh begins, then the state
                    // effect below clears it once the library reports completion.
                    animate(
                        initialValue = pullDistancePx,
                        targetValue = triggerPx * .58f,
                        animationSpec = tween(160)
                    ) { value, _ -> pullDistancePx = value }
                } else {
                    animate(
                        initialValue = pullDistancePx,
                        targetValue = 0f,
                        animationSpec = tween(200)
                    ) { value, _ -> pullDistancePx = value }
                }
                return Velocity(0f, available.y)
            }
        }
    }

    LaunchedEffect(refreshing) {
        if (!refreshing && pullDistancePx > 0f) animatePullTo(0f)
    }

    return XvoxHomePullRefreshState(
        pullModifier = Modifier.nestedScroll(connection),
        // The real list moves only part of the finger distance—an elastic stretch, not a scroll
        // reset—while the Header remains in its own unchanged shell layer.
        contentTranslationPx = pullDistancePx * .42f,
        revealProgress = (pullDistancePx / triggerPx).coerceIn(0f, 1f),
        refreshing = refreshing
    )
}

/** A thin refresh glyph grows from a point in the temporary gap, then spins only while scanning. */
@Composable
fun XvoxHomePullRefreshIndicator(
    state: XvoxHomePullRefreshState,
    topInset: Dp,
    modifier: Modifier = Modifier
) {
    val visibleProgress = if (state.refreshing) 1f else state.revealProgress
    if (visibleProgress <= .001f && !state.refreshing) return

    val density = LocalDensity.current
    val colors = XvoxTheme.colors
    val spin = rememberInfiniteTransition(label = "homeRefreshSpin")
    val refreshRotation by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(820, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homeRefreshRotation"
    )
    val translationDp = with(density) { state.contentTranslationPx.toDp() }
    val indicatorTop = if (state.refreshing) topInset + 16.dp else topInset + (translationDp * .45f)
    val iconSize = 2.dp + 20.dp * visibleProgress

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .offset(y = indicatorTop),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_xvox_refresh),
            contentDescription = if (state.refreshing) "Refreshing library" else "Pull to refresh",
            tint = colors.primaryAccent,
            modifier = Modifier
                .graphicsLayer {
                    alpha = visibleProgress
                    scaleX = visibleProgress
                    scaleY = visibleProgress
                    rotationZ = if (state.refreshing) refreshRotation else 0f
                }
                .size(iconSize)
        )
    }
}
