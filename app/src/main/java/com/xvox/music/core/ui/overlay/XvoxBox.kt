package com.xvox.music.core.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val XvoxBoxEasing = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f)

class XvoxBoxScrollState {
    var isScrollable by mutableStateOf(false)
    var scrollFraction by mutableFloatStateOf(0f)
    var visibleFraction by mutableFloatStateOf(1f)

    fun update(scrollOffset: Float, maxScroll: Float, viewportSize: Float) {
        if (maxScroll > 1f && viewportSize > 0f) {
            isScrollable = true
            scrollFraction = (scrollOffset / maxScroll).coerceIn(0f, 1f)
            visibleFraction = (viewportSize / (viewportSize + maxScroll)).coerceIn(0.12f, 0.85f)
        } else {
            isScrollable = false
            scrollFraction = 0f
            visibleFraction = 1f
        }
    }
}

val LocalXvoxBoxScrollState = compositionLocalOf<XvoxBoxScrollState?> { null }

@Composable
fun Modifier.xvoxBoxScroll(scrollState: ScrollState): Modifier {
    val boxScroll = LocalXvoxBoxScrollState.current ?: return this
    LaunchedEffect(scrollState.value, scrollState.maxValue, scrollState.viewportSize) {
        val max = if (scrollState.maxValue == Int.MAX_VALUE) 0f else scrollState.maxValue.toFloat()
        boxScroll.update(
            scrollOffset = scrollState.value.toFloat(),
            maxScroll = max,
            viewportSize = scrollState.viewportSize.toFloat()
        )
    }
    return this
}

@Composable
fun Modifier.xvoxBoxScroll(listState: LazyListState): Modifier {
    val boxScroll = LocalXvoxBoxScrollState.current ?: return this
    val layoutInfo = listState.layoutInfo
    LaunchedEffect(layoutInfo.totalItemsCount, layoutInfo.visibleItemsInfo.size, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val total = layoutInfo.totalItemsCount
        val visible = layoutInfo.visibleItemsInfo.size
        if (total > visible && total > 0) {
            val offset = listState.firstVisibleItemIndex.toFloat() + (listState.firstVisibleItemScrollOffset.toFloat() / 100f)
            val max = (total - visible).coerceAtLeast(1).toFloat()
            boxScroll.update(offset, max, visible.toFloat())
        } else {
            boxScroll.update(0f, 0f, 100f)
        }
    }
    return this
}

/** One app-wide modal: solid opaque card (0 transparency), never see-through with synchronized backdrop dimming and adaptive left scroll pill. */
@Composable
fun XvoxBox(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "XVOX",
    mini: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val swallowInteraction = remember { MutableInteractionSource() }
    val boxScrollState = remember { XvoxBoxScrollState() }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.40f else 0f,
        animationSpec = tween(220, easing = XvoxBoxEasing),
        label = "scrimAlpha"
    )

    fun close() {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(210)
            dismiss()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier.fillMaxSize()) {
            // Synchronized background scrim dim
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { close() }
            )

            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
                    .padding(
                        horizontal = if (mini) 0.dp else 20.dp,
                        vertical = if (mini) 0.dp else 16.dp
                    ),
                contentAlignment = if (mini) Alignment.BottomCenter else Alignment.Center
            ) {
                val availableHeight = maxHeight
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(220, easing = XvoxBoxEasing)) + if (mini) slideInVertically(tween(220, easing = XvoxBoxEasing)) { it }
                    else scaleIn(tween(220, easing = XvoxBoxEasing), initialScale = 0.95f),
                    exit = fadeOut(tween(200, easing = XvoxBoxEasing)) + if (mini) slideOutVertically(tween(200, easing = XvoxBoxEasing)) { it }
                    else scaleOut(tween(200, easing = XvoxBoxEasing), targetScale = 0.96f)
                ) {
                    val shape = if (mini) RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    else RoundedCornerShape(26.dp)
                    val boxFill = colors.cardElevated

                    Column(
                        Modifier
                            .widthIn(max = if (mini) 520.dp else 560.dp)
                            .fillMaxWidth()
                            .heightIn(max = availableHeight)
                            .clip(shape)
                            .background(boxFill)
                            .clickable(swallowInteraction, indication = null) { }
                            .semantics { paneTitle = title }
                    ) {
                        if (mini) {
                            Box(
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 9.dp, bottom = 5.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(width = 38.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(colors.cardBorder.copy(alpha = 0.9f))
                                )
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            onBack?.let { back ->
                                Icon(
                                    painterResource(R.drawable.ic_xvox_arrow_left),
                                    "Back",
                                    tint = colors.primaryText,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .xvoxPressScale(onClick = back)
                                        .padding(10.dp)
                                )
                            }
                            Text(
                                title,
                                color = colors.primaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (onAddClick != null) {
                                Icon(
                                    painterResource(R.drawable.ic_xvox_add),
                                    "Add",
                                    tint = colors.primaryAccent,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .xvoxPressScale(onClick = onAddClick)
                                        .padding(14.dp)
                                )
                            }
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .xvoxPressScale { close() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_xvox_close),
                                    "Close $title",
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.7.dp)
                                .background(colors.cardBorder.copy(alpha = 0.55f))
                        )

                        CompositionLocalProvider(LocalXvoxBoxScrollState provides boxScrollState) {
                            Box(
                                Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth()
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = if (boxScrollState.isScrollable) 16.dp else 14.dp,
                                            end = 14.dp,
                                            top = 10.dp,
                                            bottom = 14.dp
                                        )
                                ) {
                                    content()
                                }

                                // Left-side dynamic pill indicator (visible only when content is scrollable)
                                if (boxScrollState.isScrollable) {
                                    BoxWithConstraints(
                                        Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxHeight()
                                            .padding(start = 5.dp, top = 10.dp, bottom = 14.dp)
                                            .width(3.5.dp)
                                    ) {
                                        val totalH = maxHeight
                                        val pillHeight = (totalH * boxScrollState.visibleFraction).coerceIn(24.dp, (totalH - 4.dp).coerceAtLeast(24.dp))
                                        val maxTravel = (totalH - pillHeight).coerceAtLeast(0.dp)
                                        val currentOffset = maxTravel * boxScrollState.scrollFraction

                                        // Subtle background track
                                        Box(
                                            Modifier
                                                .fillMaxHeight()
                                                .width(3.5.dp)
                                                .clip(CircleShape)
                                                .background(colors.cardBorder.copy(alpha = 0.20f))
                                        )

                                        // Active sliding indicator pill
                                        Box(
                                            Modifier
                                                .offset(y = currentOffset)
                                                .size(width = 3.5.dp, height = pillHeight)
                                                .clip(CircleShape)
                                                .background(colors.primaryAccent.copy(alpha = 0.88f))
                                        )
                                    }
                                }
                            }
                        }

                        if (bottomAction != null) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            ) {
                                bottomAction()
                            }
                        }
                    }
                }
            }
        }
    }
}
