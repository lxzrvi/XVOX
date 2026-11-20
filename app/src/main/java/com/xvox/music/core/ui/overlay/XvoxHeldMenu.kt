// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/overlay/XvoxHeldMenu.kt
package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import com.xvox.music.core.ui.effects.XvoxHeldItem
import com.xvox.music.core.ui.effects.XvoxLongPressOrigin
import kotlin.math.roundToInt
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.XvoxBlurLevel
import com.xvox.music.core.ui.blur.xvoxLiveBlur

/**
 * "Held item" presentation (the way BitChord / Apple Music open a long-press): the page behind is
 * blurred and dimmed, the held song lifts off the page with a small spring pop and its actions hang
 * under it. There is no sheet card, handle or footer; tapping outside or Back closes it.
 */
@Composable
fun XvoxHeldMenuHost(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    closeSignal: Int = 0,
    liveScrim: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val scrimColor = if (colors.isLight) colors.primaryText else colors.background
    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    val dismiss by rememberUpdatedState(onDismiss)
    val ctl = LocalXvoxOverlayController.current

    if (liveScrim) {
        LaunchedEffect(closing) { ctl.sheetBlurActive = !closing }
        DisposableEffect(Unit) { onDispose { ctl.sheetBlurActive = false } }
    }

    LaunchedEffect(closing) {
        if (!closing) {
            // Under-damped on purpose: the small overshoot is the "pop" of the item coming off the page.
            progress.animateTo(1f, spring(dampingRatio = .74f, stiffness = 430f))
        } else {
            progress.animateTo(0f, tween(durationMillis = 190, easing = FastOutSlowInEasing))
            dismiss()
        }
    }
    fun close() { if (!closing) closing = true }

    BackHandler(onBack = ::close)
    val initialCloseSignal = remember { closeSignal }
    LaunchedEffect(closeSignal) { if (closeSignal != initialCloseSignal) close() }

    // A held card: lift it out of the list. A three-dot open has no held card and falls through
    // to the centred layout below.
    val held = remember { XvoxLongPressOrigin.consume() }
    if (held != null) {
        XvoxLiftedMenu(
            held = held,
            progress = { progress.value },
            scrimColor = scrimColor,
            onScrimClick = ::close,
            modifier = modifier,
            content = content
        )
        return
    }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .xvoxLiveBlur(
                    shape = RoundedCornerShape(0.dp),
                    level = XvoxBlurLevel.SheetBackdrop,
                    baseColor = scrimColor,
                    // Host-level sibling of the captured app layer, so it may sample the page.
                    sky = null,
                    borderWidth = 0.dp
                )
                .drawBehind { drawRect(scrimColor, alpha = .30f) }
                .clickable(remember { MutableInteractionSource() }, indication = null) { close() }
        )
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .graphicsLayer {
                    val p = progress.value
                    val s = lerp(.86f, 1f, p)
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                }
                // Taps on empty space inside the content area still dismiss; content consumes its own.
                .clickable(remember { MutableInteractionSource() }, indication = null) { close() },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.pointerInput(Unit) {}) { content() }
        }
    }
}


/** True inside a lifted menu: the content then draws only its actions, not its own cover card. */
val LocalXvoxHeldLifted = compositionLocalOf { false }

private val MenuPlateRadius = 30.dp
private val LiftCardRadius = 20.dp
private val LiftMenuWidth = 262.dp

/**
 * BitChord's HeldContextMenu: the held card, exactly as it was drawn, pops out of its list onto a
 * frosted plate while the page behind blurs; the actions hang off it, below by preference and above
 * when the card is low on screen. Under-damped spring = the small overshoot is the "pop".
 */
@Composable
private fun XvoxLiftedMenu(
    held: XvoxHeldItem,
    progress: () -> Float,
    scrimColor: Color,
    onScrimClick: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val dark = !colors.isLight
    val plate = (if (dark) Color.Black else Color.White).copy(alpha = .5f)
    val hairline = if (dark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .06f)
    val cardShape = RoundedCornerShape(LiftCardRadius)
    val platePadding = 8.dp
    val pictureShape = RoundedCornerShape(LiftCardRadius - platePadding)
    val insets = WindowInsets.safeDrawing
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }

    Layout(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOffset = it.positionInWindow() },
        content = {
            // 0: the page, pushed back (blur sibling + dim).
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
                    .xvoxLiveBlur(
                        shape = RoundedCornerShape(0.dp),
                        level = XvoxBlurLevel.SheetBackdrop,
                        baseColor = scrimColor,
                        sky = null,
                        borderWidth = 0.dp
                    )
                    .drawBehind { drawRect(scrimColor, alpha = if (dark) .30f else .10f) }
                    .clickable(remember { MutableInteractionSource() }, indication = null) { onScrimClick() }
            )
            // 1: the held card on its plate.
            Box(
                Modifier
                    .shadow(20.dp, cardShape, clip = false)
                    .pointerInput(Unit) {}
                    .clip(cardShape)
                    .background(plate)
                    .border(.5.dp, hairline, cardShape)
                    .padding(platePadding)
            ) {
                held.image?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize().clip(pictureShape)
                    )
                }
            }
            // 2: the actions.
            Box(Modifier.pointerInput(Unit) {}) {
                CompositionLocalProvider(LocalXvoxHeldLifted provides true) { content() }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val margin = 12.dp.roundToPx()
        val gap = 10.dp.roundToPx()
        val pad = platePadding.roundToPx()
        val top = insets.getTop(this) + 8.dp.roundToPx()
        val bottom = height - insets.getBottom(this) - 8.dp.roundToPx()

        val backdrop = measurables[0].measure(Constraints.fixed(width, height))

        val itemBounds = held.bounds.translate(-overlayOffset)
        // The plate at the card's own size, so at rest the picture on it is pixel for pixel what
        // was held; a wide row is scaled down just enough to clear the screen edges.
        val plateW = (itemBounds.width.roundToInt() + pad * 2).coerceAtLeast(1)
        val plateH = (itemBounds.height.roundToInt() + pad * 2).coerceAtLeast(1)
        val card = measurables[1].measure(Constraints.fixed(plateW, plateH))
        val liftScale = ((width - margin * 2f) / plateW).coerceAtMost(1f)
        val visualW = plateW * liftScale
        val visualH = plateH * liftScale

        val menuWidth = (width - margin * 2).coerceAtMost(LiftMenuWidth.roundToPx()).coerceAtLeast(0)
        val menuMaxHeight = (bottom - top - visualH.roundToInt() - gap).coerceAtLeast(0)
        val menu = measurables[2].measure(
            Constraints(minWidth = menuWidth, maxWidth = menuWidth, maxHeight = menuMaxHeight)
        )

        val visualLeft = (itemBounds.center.x - visualW / 2f)
            .coerceIn(margin.toFloat(), (width - margin - visualW).coerceAtLeast(margin.toFloat()))
        var visualTop = (itemBounds.center.y - visualH / 2f)
            .coerceIn(top.toFloat(), (bottom - visualH).coerceAtLeast(top.toFloat()))
        val menuBelow: Boolean
        when {
            visualTop + visualH + gap + menu.height <= bottom -> menuBelow = true
            visualTop - gap - menu.height >= top -> menuBelow = false
            else -> {
                menuBelow = true
                visualTop = (bottom - menu.height - gap - visualH).coerceAtLeast(top.toFloat())
            }
        }
        val visualCenterX = visualLeft + visualW / 2f
        val visualCenterY = visualTop + visualH / 2f

        // The menu hangs off the card's nearer edge, so a right-hand column keeps it under itself.
        val alignEnd = visualCenterX > width / 2f
        val menuX = (if (alignEnd) visualLeft + visualW - menuWidth else visualLeft).roundToInt()
            .coerceIn(margin, (width - margin - menuWidth).coerceAtLeast(margin))
        val menuY = if (menuBelow) (visualTop + visualH + gap).roundToInt()
        else (visualTop - gap - menu.height).roundToInt()

        val cardX = (visualCenterX - plateW / 2f).roundToInt()
        val cardY = (visualCenterY - plateH / 2f).roundToInt()
        val fromDx = itemBounds.center.x - visualCenterX
        val fromDy = itemBounds.center.y - visualCenterY

        layout(width, height) {
            backdrop.place(0, 0)
            card.placeWithLayer(cardX, cardY) {
                val p = progress()
                val scale = lerp(1f, liftScale, p)
                scaleX = scale
                scaleY = scale
                translationX = lerp(fromDx, 0f, p)
                translationY = lerp(fromDy, 0f, p)
                // Quick, because it starts over the very card it copies.
                alpha = (p * 3f).coerceIn(0f, 1f)
            }
            menu.placeWithLayer(menuX, menuY) {
                val p = progress()
                alpha = ((p - .1f) / .9f).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(
                    pivotFractionX = if (alignEnd) 1f else 0f,
                    pivotFractionY = if (menuBelow) 0f else 1f
                )
                val scale = lerp(.55f, 1f, p)
                scaleX = scale
                scaleY = scale
                translationX = lerp(fromDx, 0f, p)
                translationY = lerp(fromDy, 0f, p)
            }
        }
    }
}
