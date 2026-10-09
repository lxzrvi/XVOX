package com.xvox.music.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.design.theme.xvoxGlassReflection
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
    val railShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val actionCount = actions.size + 2 // select/unselect all + clear
    val railSpacing = if (screenHeightDp < 560) 3.dp else 6.dp
    val actionHeight = (
        ((screenHeightDp - 76f - (actionCount + 1) * railSpacing.value) / actionCount)
            .coerceIn(20f, 48f)
        ).dp

    Popup(alignment = Alignment.CenterEnd) {
        AnimatedVisibility(
            visible = entered,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(190)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(150)
            )
        ) {
            Column(
                modifier = modifier
                    .width(70.dp)
                    .clip(railShape)
                    .xvoxGlassReflection(shape = railShape, radius = 20)
                    .background(colors.cardElevated)
                    .padding(start = 7.dp, top = 8.dp, end = 2.dp, bottom = 8.dp),
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
                Box(
                    modifier = Modifier
                        .size(33.dp)
                        .clip(CircleShape)
                        .background(colors.primaryAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$selectedCount",
                        color = colors.background,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                CollectionSelectionAction(
                    height = actionHeight,
                    iconRes = R.drawable.ic_xvox_done_all,
                    label = if (allSelected) "Unselect all $title" else "Select all $title",
                    onClick = onToggleAll
                )
                actions.forEach { action ->
                    CollectionSelectionAction(actionHeight, action.iconRes, action.label, action.onClick)
                }
                CollectionSelectionAction(actionHeight, R.drawable.ic_xvox_close, "Clear $title selection", onClear)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionSelectionAction(
    height: androidx.compose.ui.unit.Dp,
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val overlays = LocalXvoxOverlayController.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = { overlays.showP(label) }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = colors.primaryAccent,
            modifier = Modifier.size(20.dp)
        )
    }
}
