package com.xvox.music.features.home

import android.content.Context
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
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.player.playback.MainPlayerViewModel

/**
 * A direct right-entering selection rail. Its height is derived from the available screen space,
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
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val railShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    val sourceLabel = categoryName?.takeIf { it.isNotBlank() } ?: "Selection"
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    // Select, Play next, Queue, Playlist, Like/remove, Share, Clear; Recent/device contexts add
    // one destructive action. Derive an icon row height rather than clipping below the edge.
    val actionCount = 7 + if (categoryName == "Recently Played" || onDeleteSelected != null) 1 else 0
    val railSpacing = if (screenHeightDp < 560) 3.dp else 6.dp
    val actionHeight = (
        ((screenHeightDp - 76f - (actionCount + 1) * railSpacing.value) / actionCount)
            .coerceIn(20f, 48f)
        ).dp

    Popup(alignment = Alignment.CenterEnd) {
        AnimatedVisibility(
            visible = entered,
            // Pure horizontal motion: no vertical offset or scale can make this rail enter
            // diagonally when a long press begins selection.
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(190)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
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
                    text = sourceLabel,
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
                        text = "${selectedSongs.size}",
                        color = colors.background,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Keep the actual action list at intrinsic height: every action stays visible,
                // full-height and icon-only. The labels remain available through the XVOX pill on
                // long press rather than consuming horizontal rail space.
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
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiActionItem(
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
            modifier = Modifier.size(20.dp)
        )
    }
}
