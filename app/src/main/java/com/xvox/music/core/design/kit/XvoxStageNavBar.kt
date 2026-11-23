package com.xvox.music.core.design.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.ui.effects.XvoxGlassLevel
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal data class StageNavEntry(val key: Int, val label: String, val symbol: SfSymbol)

/** The glass material the Settings "Glass blur" level stands for. Shared by the bar and the mini player. */
internal fun xvoxNavMaterial(): NavigationGlassMaterial = when (XvoxGlassLevel.key) {
    "clear" -> NavigationGlassMaterial.Clear
    "small" -> NavigationGlassMaterial.RegularSmall
    "regular" -> NavigationGlassMaterial.RegularMedium
    else -> NavigationGlassMaterial.RegularLarge
}

/**
 * XVOX's own navigation bar: the same GlassSurface (and the same chosen blur level) as the mini
 * player, a round search button beside it, and one selection pill whose two edges always move
 * together. While the page scrolls, [compactProgress] folds the bar into a single circle holding
 * the selected tab, and the search button moves out to the far edge.
 */
@Composable
internal fun BoxScope.StageNavBar(
    entries: List<StageNavEntry>,
    selectedKey: Int,
    compactProgress: () -> Float,
    onSelect: (Int) -> Unit,
    onExpand: () -> Unit,
    onSearch: () -> Unit,
    onSearchBounds: (Rect) -> Unit,
    backdrop: Backdrop,
    maxBarWidth: Dp,
    controlHeight: Dp,
    gap: Dp,
    sideMargin: Dp,
) {
    val glass = LocalGlassColors.current
    val scope = rememberCoroutineScope()
    val count = entries.size
    val selectedIndex = entries.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val pillPos = remember { Animatable(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    // One spring drives the whole pill, so its left and right edges can never disagree.
    LaunchedEffect(selectedIndex) {
        if (!dragging) {
            pillPos.animateTo(selectedIndex.toFloat(), spring(dampingRatio = .84f, stiffness = 420f))
        }
    }
    val pillScale by animateFloatAsState(
        targetValue = if (pressed || dragging) 1.07f else 1f,
        animationSpec = spring(dampingRatio = .62f, stiffness = Spring.StiffnessMedium),
        label = "pillScale"
    )
    val material = xvoxNavMaterial()
    val p = compactProgress().coerceIn(0f, 1f)

    BoxWithConstraints(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(controlHeight)
    ) {
        val full = maxWidth - sideMargin * 2
        val barTotal = lerp(minOf(maxBarWidth, full), full, p)
        val capsuleW = lerp(barTotal - gap - controlHeight, controlHeight, p)
        val left = (maxWidth - barTotal) / 2

        GlassSurface(
            modifier = Modifier.offset(x = left).width(capsuleW).height(controlHeight),
            backdrop = backdrop,
            shape = RoundedCornerShape(50),
            style = GlassSurfaceStyle.Navigation,
            material = material
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val inset = 4.dp
                val slotW = (capsuleW - inset * 2) / count
                val slotPx = with(androidx.compose.ui.platform.LocalDensity.current) { slotW.toPx() }
                val insetPx = with(androidx.compose.ui.platform.LocalDensity.current) { inset.toPx() }
                val compact by rememberUpdatedState(p > .5f)
                val slotNow by rememberUpdatedState(slotPx)
                val countNow by rememberUpdatedState(count)

                // Selection pill.
                val pillH = controlHeight - inset * 2
                val pillW = lerp(slotW, controlHeight - inset * 2, p)
                val pillCenter = lerp(inset + slotW * (pillPos.value + .5f), capsuleW / 2, p)
                // The pill is itself a piece of liquid glass, lifted a step above the bar.
                GlassSurface(
                    modifier = Modifier
                        .offset(x = pillCenter - pillW / 2, y = inset)
                        .size(pillW, pillH)
                        .graphicsLayer { scaleX = pillScale; scaleY = pillScale },
                    backdrop = backdrop,
                    shape = CircleShape,
                    style = GlassSurfaceStyle.Navigation,
                    emphasis = GlassEmphasis.Prominent,
                    material = material
                ) {
                    Box(Modifier.fillMaxSize().background(glass.content.copy(alpha = .08f)))
                }

                entries.forEachIndexed { index, entry ->
                    val isSelected = index == selectedIndex
                    val center = lerp(
                        inset + slotW * (index + .5f),
                        if (isSelected) capsuleW / 2 else inset + slotW * (index + .5f),
                        p
                    )
                    val tint = if (isSelected) glass.accent else glass.content.copy(alpha = .72f)
                    Column(
                        modifier = Modifier
                            .offset(x = center - slotW / 2)
                            .width(slotW)
                            .fillMaxSize()
                            .graphicsLayer { alpha = if (isSelected) 1f else 1f - p },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        SfIcon(entry.symbol, entry.label, tint = tint, size = 24.dp)
                        Text(
                            entry.label,
                            color = tint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer { alpha = 1f - p }
                        )
                    }
                }

                // Tap a tab, or press and slide the pill across them.
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    pressed = true
                                    tryAwaitRelease()
                                    pressed = false
                                },
                                onTap = { offset ->
                                    if (compact) {
                                        onExpand()
                                    } else {
                                        val idx = ((offset.x - insetPx) / slotNow).toInt().coerceIn(0, countNow - 1)
                                        onSelect(entries[idx].key)
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { if (!compact) dragging = true },
                                onDragEnd = {
                                    if (dragging) {
                                        dragging = false
                                        val idx = pillPos.value.roundToInt().coerceIn(0, countNow - 1)
                                        scope.launch { pillPos.animateTo(idx.toFloat(), spring(dampingRatio = .84f, stiffness = 420f)) }
                                        onSelect(entries[idx].key)
                                    }
                                },
                                onDragCancel = {
                                    dragging = false
                                    scope.launch { pillPos.animateTo(selectedIndex.toFloat(), spring(dampingRatio = .84f, stiffness = 420f)) }
                                },
                                onHorizontalDrag = { change, _ ->
                                    if (dragging) {
                                        change.consume()
                                        val f = ((change.position.x - insetPx) / slotNow - .5f).coerceIn(0f, (countNow - 1).toFloat())
                                        scope.launch { pillPos.snapTo(f) }
                                    }
                                }
                            )
                        }
                )
            }
        }

        // Search button, a circle of its own.
        GlassSurface(
            modifier = Modifier
                .offset(x = left + barTotal - controlHeight)
                .size(controlHeight)
                .onGloballyPositioned { onSearchBounds(it.boundsInWindow()) },
            backdrop = backdrop,
            shape = CircleShape,
            style = GlassSurfaceStyle.Navigation,
            material = material,
            onClick = onSearch
        ) {
            SfIcon(SfSymbol.Search, "Search", tint = glass.content, size = 24.dp)
        }
    }
}
