// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/HomeCollectionMultiSelectBar.kt
package com.xvox.music.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.xvox.music.core.ui.blur.xvoxLiveBlur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController

/** One icon-only entity action exposed in the Artist/Playlist outer-selection rail. */
data class XvoxCollectionSelectionAction(
    val iconRes: Int,
    val label: String,
    val onClick: () -> Unit
)

/**
 * Shared outer-collection selection surface. It deliberately differs from song selection only in
 * its action set: Artist and Playlist cards are selected as entities, while tracks inside their
 * detail pages start their own independent song-selection surface.
 */
@Composable
fun HomeCollectionMultiSelectBar(
    title: String,
    selectedCount: Int,
    allSelected: Boolean,
    actions: List<XvoxCollectionSelectionAction>,
    onToggleAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    var railOnLeft by rememberSaveable { mutableStateOf(false) }
    val railSide = if (railOnLeft) XvoxSelectionRailSide.LEFT else XvoxSelectionRailSide.RIGHT
    // Match the counter and action-box radius; the rail is no longer a separate large edge arc.
    val railShape = RoundedCornerShape(10.dp)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val actionCount = actions.size + 3 // select/unselect all + clear + side switch
    val railSpacing = if (screenHeightDp < 560) 3.dp else 6.dp
    val countBoxHeight = 30.dp
    val titleHeight = 22.dp
    val actionHeight = (
        (
            screenHeightDp - 14f - titleHeight.value - countBoxHeight.value -
                (actionCount + 1) * railSpacing.value
            ) / actionCount
        ).coerceIn(20f, 48f).dp

    // A full-screen transition host gives the side switch a real sequential edge transfer rather
    // than remeasuring a popup and producing a diagonal/overlapping rail.
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = entered,
            modifier = Modifier.fillMaxSize(),
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth ->
                    if (railSide == XvoxSelectionRailSide.RIGHT) fullWidth else -fullWidth
                },
                animationSpec = tween(360, easing = androidx.compose.animation.core.CubicBezierEasing(.22f, 1f, .36f, 1f))
            ) + fadeIn(tween(160)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth ->
                    if (railSide == XvoxSelectionRailSide.RIGHT) fullWidth else -fullWidth
                },
                animationSpec = tween(240, easing = androidx.compose.animation.core.CubicBezierEasing(.4f, 0f, .9f, .6f))
            ) + fadeOut(tween(140))
        ) {
            AnimatedContent(
                targetState = railSide,
                transitionSpec = {
                    val exitDirection = if (initialState == XvoxSelectionRailSide.RIGHT) 1 else -1
                    val enterDirection = if (targetState == XvoxSelectionRailSide.RIGHT) 1 else -1
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth * enterDirection },
                        animationSpec = tween(340, delayMillis = 170, easing = androidx.compose.animation.core.CubicBezierEasing(.22f, 1f, .36f, 1f))
                    ) + fadeIn(tween(150, delayMillis = 170))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth * exitDirection },
                            animationSpec = tween(230, easing = androidx.compose.animation.core.CubicBezierEasing(.4f, 0f, .9f, .6f))
                        ) + fadeOut(tween(130))
                    )
                },
                modifier = Modifier.fillMaxSize(),
                label = "collectionSelectionRailSide"
            ) { side ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = if (side == XvoxSelectionRailSide.LEFT) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                ) {
                    Column(
                        modifier = modifier
                            .width(70.dp)
                            .xvoxLiveBlur(
                                shape = railShape,
                                level = com.xvox.music.core.ui.blur.XvoxBlurLevel.Chrome,
                                baseColor = colors.cardElevated.copy(alpha = .94f),
                                // Sits inside the page it floats over, so no live sample (that would
                                // recurse); a frosted glass tint + the shared rim keeps it glassy.
                                sky = null,
                                glassEdge = true
                            )                            .padding(start = 3.dp, top = 7.dp, end = 3.dp, bottom = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(railSpacing)
                    ) {
                        Text(
                            text = title,
                            color = colors.secondaryText,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                        CollectionSelectionRailCountBox(selectedCount, countBoxHeight)
                        CollectionSelectionAction(
                            height = actionHeight,
                            iconRes = R.drawable.ic_xvox_done_all,
                            label = if (allSelected) "Unselect all $title" else "Select all $title",
                            onClick = onToggleAll
                        )
                        actions.forEach { action ->
                            CollectionSelectionAction(actionHeight, action.iconRes, action.label, action.onClick)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                            CollectionSelectionAction(actionHeight, R.drawable.ic_xvox_close, "Clear $title selection", onClear, modifier = Modifier.fillMaxWidth())
                            CollectionSelectionAction(
                                height = actionHeight,
                                iconRes = R.drawable.ic_xvox_arrow_left,
                                label = if (side == XvoxSelectionRailSide.RIGHT) "Move selection rail to left" else "Move selection rail to right",
                                onClick = { railOnLeft = side == XvoxSelectionRailSide.RIGHT },
                                mirrorIcon = side == XvoxSelectionRailSide.LEFT,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionSelectionRailCountBox(
    count: Int,
    height: androidx.compose.ui.unit.Dp
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(colors.primaryAccent)
            .border(.8.dp, colors.cardBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            color = colors.background,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionSelectionAction(
    height: androidx.compose.ui.unit.Dp,
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    mirrorIcon: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val colors = XvoxTheme.colors
    val overlays = LocalXvoxOverlayController.current
    val actionShape = RoundedCornerShape(10.dp)
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) .88f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = .55f, stiffness = 700f),
        label = "railPress"
    )
    val destructive = label.startsWith("Delete") || label.startsWith("Remove")
    val actionTint = if (destructive) androidx.compose.ui.graphics.Color(0xFFE85B5B) else colors.primaryAccent
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .height(height)
            .clip(actionShape)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = { overlays.showP(label) }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = actionTint,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { scaleX = if (mirrorIcon) -1f else 1f }
        )
    }
}
