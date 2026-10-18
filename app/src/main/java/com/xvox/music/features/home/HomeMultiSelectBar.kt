package com.xvox.music.features.home

import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.player.playback.MainPlayerViewModel

/** Direction of the movable Home selection rail. */
internal enum class XvoxSelectionRailSide { LEFT, RIGHT }

/**
 * A direct edge-entering selection rail. Its height is derived from the available screen space,
 * so every action remains visible even in a short landscape viewport. Long press delegates each
 * action name to the one universal XVOX pill.
 */
@Composable
fun HomeMultiSelectBar(
    selectedSongs: List<Song>,
    selectedPlaylist: XvoxPlaylist?,
    libraryMode: XvoxHomeLibraryMode,
    viewModel: HomeViewModel,
    playerViewModel: MainPlayerViewModel,
    overlays: XvoxOverlayController,
    context: Context,
    categoryName: String? = null,
    allInScopeSelected: Boolean = false,
    /** Selects or clears only the source/category resolved by HomeScreen. */
    onToggleSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val chrome = com.xvox.music.core.ui.chrome.LocalXvoxChromeStyle.current
    // Match the Default Mini Player's low-transparency material rather than introducing a harsh
    // selection-only surface. The persisted Mini Player alpha is .94 by default (6% transparent).
    val railSurface = colors.background.copy(alpha = chrome.miniBgAlpha.coerceIn(0f, 1f))
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    var railOnLeft by rememberSaveable { mutableStateOf(false) }
    val railSide = if (railOnLeft) XvoxSelectionRailSide.LEFT else XvoxSelectionRailSide.RIGHT
    // The rail, counter, and every action use one shared radius instead of a large edge-only arc.
    val railShape = RoundedCornerShape(10.dp)
    val sourceLabel = categoryName?.takeIf { it.isNotBlank() } ?: "Selection"
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    // Select, Play next, Queue, Playlist, Like/remove, Share, Clear, and the lower side switch;
    // Recent/device contexts add one destructive action. Keep all actions reachable in short views.
    val actionCount = 8 + if (categoryName == "Recently Played" || onDeleteSelected != null) 1 else 0
    val railSpacing = if (screenHeightDp < 560) 3.dp else 6.dp
    val countBoxHeight = 30.dp
    val titleHeight = 22.dp
    val actionHeight = (
        (
            screenHeightDp - 14f - titleHeight.value - countBoxHeight.value -
                (actionCount + 1) * railSpacing.value
            ) / actionCount
        ).coerceIn(20f, 48f).dp

    // Use a full-screen transition host so side changes can fully leave one edge before they
    // enter from the other. A Popup would remeasure and turn that transfer into diagonal motion.
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = entered,
            modifier = Modifier.fillMaxSize(),
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth ->
                    if (railSide == XvoxSelectionRailSide.RIGHT) fullWidth else -fullWidth
                },
                animationSpec = tween(190)
            ) + fadeIn(tween(120)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth ->
                    if (railSide == XvoxSelectionRailSide.RIGHT) fullWidth else -fullWidth
                },
                animationSpec = tween(150)
            ) + fadeOut(tween(100))
        ) {
            AnimatedContent(
                targetState = railSide,
                transitionSpec = {
                    val exitDirection = if (initialState == XvoxSelectionRailSide.RIGHT) 1 else -1
                    val enterDirection = if (targetState == XvoxSelectionRailSide.RIGHT) 1 else -1
                    // The incoming rail waits until the previous edge has cleared. This produces
                    // right-out → left-in and left-out → right-in instead of overlapping rails.
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth * enterDirection },
                        animationSpec = tween(190, delayMillis = 150)
                    ) + fadeIn(tween(110, delayMillis = 150))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth * exitDirection },
                            animationSpec = tween(150)
                        ) + fadeOut(tween(100))
                    )
                },
                modifier = Modifier.fillMaxSize(),
                label = "songSelectionRailSide"
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
                            .clip(railShape)
                            .background(railSurface)
                            .border(.8.dp, colors.cardBorder, railShape)
                            .padding(start = 3.dp, top = 7.dp, end = 3.dp, bottom = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(railSpacing)
                    ) {
                        Text(
                            text = sourceLabel,
                            color = colors.secondaryText,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                        SelectionRailCountBox(selectedSongs.size, countBoxHeight)

                        MultiActionItem(actionHeight,
                            R.drawable.ic_xvox_done_all,
                            if (allInScopeSelected) "Unselect all in $sourceLabel" else "Select all in $sourceLabel",
                            onToggleSelectAll
                        )
                        MultiActionItem(actionHeight, R.drawable.ic_xvox_play, "Play next") {
                            val msg = if (selectedSongs.size == 1) {
                                playerViewModel.playNextInQueue(selectedSongs[0])
                            } else {
                                playerViewModel.playNextInQueue(selectedSongs)
                            }
                            overlays.showP(msg)
                            onClearSelection()
                        }
                        MultiActionItem(actionHeight, R.drawable.ic_xvox_queue, "Add to queue") {
                            overlays.showP(playerViewModel.addToQueue(selectedSongs))
                            onClearSelection()
                        }
                        MultiActionItem(actionHeight, R.drawable.ic_xvox_playlist, "Add to playlist") {
                            showMultiAddToPlaylistOverlay(
                                overlays = overlays,
                                viewModel = viewModel,
                                songs = selectedSongs,
                                onDone = onClearSelection
                            )
                        }
                        when {
                            selectedPlaylist != null -> MultiActionItem(actionHeight, R.drawable.ic_xvox_delete, "Remove from ${selectedPlaylist.name}") {
                                viewModel.removeMultipleFromPlaylist(selectedPlaylist.id, selectedSongs) {
                                    overlays.showP("${selectedSongs.size} songs removed from ${selectedPlaylist.name}")
                                    onClearSelection()
                                }
                            }
                            libraryMode == XvoxHomeLibraryMode.LIKED -> MultiActionItem(actionHeight, R.drawable.ic_xvox_heart_outline, "Remove from liked") {
                                viewModel.removeMultipleFromLiked(selectedSongs)
                                overlays.showP("${selectedSongs.size} songs removed from Liked")
                                onClearSelection()
                            }
                            else -> MultiActionItem(actionHeight, R.drawable.ic_xvox_heart, "Add to liked") {
                                viewModel.addMultipleToLiked(selectedSongs)
                                overlays.showP("${selectedSongs.size} songs added to Liked")
                                onClearSelection()
                            }
                        }
                        if (categoryName == "Recently Played") {
                            MultiActionItem(actionHeight, R.drawable.ic_xvox_delete, "Remove from recently played") {
                                viewModel.removeMultipleFromRecent(selectedSongs)
                                overlays.showP("${selectedSongs.size} removed from Recently Played")
                                onClearSelection()
                            }
                        } else if (onDeleteSelected != null) {
                            MultiActionItem(actionHeight, R.drawable.ic_xvox_delete, "Delete selected songs", onDeleteSelected)
                        }
                        MultiActionItem(actionHeight, R.drawable.ic_xvox_share, "Share selected songs") {
                            XvoxSongActions.shareMultiple(context, selectedSongs)
                            onClearSelection()
                        }
                        MultiActionItem(actionHeight, R.drawable.ic_xvox_close, "Clear selection", onClearSelection)
                        MultiActionItem(
                            actionHeight,
                            R.drawable.ic_xvox_arrow_left,
                            if (side == XvoxSelectionRailSide.RIGHT) {
                                "Move selection rail to left"
                            } else {
                                "Move selection rail to right"
                            },
                            onClick = { railOnLeft = side == XvoxSelectionRailSide.RIGHT },
                            mirrorIcon = side == XvoxSelectionRailSide.LEFT
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionRailCountBox(
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
private fun MultiActionItem(
    height: androidx.compose.ui.unit.Dp,
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    mirrorIcon: Boolean = false
) {
    val colors = XvoxTheme.colors
    val overlays = LocalXvoxOverlayController.current
    val actionShape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(actionShape)
            .background(colors.background.copy(alpha = .72f))
            .border(.8.dp, colors.cardBorder.copy(alpha = .88f), actionShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { overlays.showP(label) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = colors.primaryAccent,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { scaleX = if (mirrorIcon) -1f else 1f }
        )
    }
}
