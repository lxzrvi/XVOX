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
