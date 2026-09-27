package com.xvox.music.features.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.player.playback.MainPlayerViewModel

private val MultiActionRowHeight = 42.dp
private val MultiActionViewportHeight = MultiActionRowHeight * 4

/**
 * A compact right-entering selection overlay. Popup deliberately gives it a window-level layer so
 * its controls remain above the shared Header, navigation bar, and Mini Player at every scroll
 * depth; the selection count stays fixed while only the action viewport scrolls.
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
    /** Selects only the current source/category resolved by HomeScreen, never the whole library. */
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val actionsScroll = rememberScrollState()
    val density = LocalDensity.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val headerLabel = when {
        !categoryName.isNullOrBlank() -> categoryName
        selectedPlaylist != null -> selectedPlaylist.name
        libraryMode == XvoxHomeLibraryMode.LIKED -> "Liked"
        libraryMode == XvoxHomeLibraryMode.ARTISTS -> "Artists"
        libraryMode == XvoxHomeLibraryMode.PLAYLISTS -> "Playlists"
        else -> "All Songs"
    }

    // The scrollbar's geometry follows the actual ScrollState (px), while its drawing remains in
    // dp so it stays a clean, attached 2dp right rail on every density.
    val viewportPx = with(density) { MultiActionViewportHeight.toPx() }
    val contentPx = viewportPx + actionsScroll.maxValue.toFloat()
    val visibleShare = if (contentPx <= 0f) 1f else (viewportPx / contentPx).coerceIn(0f, 1f)
    val thumbHeight = (MultiActionViewportHeight * visibleShare).coerceIn(16.dp, MultiActionViewportHeight)
    val scrollShare = if (actionsScroll.maxValue <= 0) 0f else {
        actionsScroll.value.toFloat() / actionsScroll.maxValue.toFloat()
    }
    val thumbOffset = (MultiActionViewportHeight - thumbHeight) * scrollShare
    // The flush outer edge is square; the inner edge rounds toward page content.
    val railShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 0.dp, bottomEnd = 0.dp)

    Popup(alignment = Alignment.CenterEnd) {
        AnimatedVisibility(
            visible = entered,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(190)
            ) + fadeIn(tween(120)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(150)
            ) + fadeOut(tween(100))
        ) {
            Column(
                modifier = modifier
                    // Tight for its row labels but not so narrow that actions wrap into a tall rail.
                    .widthIn(min = 112.dp, max = 138.dp)
                    .clip(railShape)
                    .background(colors.cardElevated.copy(alpha = .99f))
                    .padding(start = 9.dp, top = 8.dp, end = 7.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Fixed metadata header: scrolling actions never move or obscure the selection count.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.primaryAccent.copy(alpha = .20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${selectedSongs.size}",
                            color = colors.primaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = headerLabel,
                        color = colors.primaryText,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Exactly four action rows are visible; subsequent actions use this
                        // internal viewport rather than expanding across the page.
                        .height(MultiActionViewportHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 5.dp)
                            .verticalScroll(actionsScroll)
                    ) {
                        MultiActionItem(R.drawable.ic_xvox_check, "Select all", onSelectAll)
                        MultiActionItem(
                            R.drawable.ic_xvox_play,
                            "Next"
                        ) {
                            val msg = if (selectedSongs.size == 1) {
                                playerViewModel.playNextInQueue(selectedSongs[0])
                            } else {
                                playerViewModel.playNextInQueue(selectedSongs)
                            }
                            overlays.showP(msg)
                            onClearSelection()
                        }
                        MultiActionItem(R.drawable.ic_xvox_queue, "Queue") {
                            overlays.showP(playerViewModel.addToQueue(selectedSongs))
                            onClearSelection()
                        }
                        MultiActionItem(R.drawable.ic_xvox_playlist, "Playlist") {
                            showMultiAddToPlaylistOverlay(
                                overlays = overlays,
                                viewModel = viewModel,
                                songs = selectedSongs,
                                onDone = onClearSelection
                            )
                        }
                        if (selectedPlaylist != null) {
                            MultiActionItem(R.drawable.ic_xvox_delete, "Remove") {
                                viewModel.removeMultipleFromPlaylist(selectedPlaylist.id, selectedSongs) {
                                    overlays.showP("${selectedSongs.size} songs removed from ${selectedPlaylist.name}")
                                    onClearSelection()
                                }
                            }
                        } else if (libraryMode == XvoxHomeLibraryMode.LIKED) {
                            MultiActionItem(R.drawable.ic_xvox_heart_outline, "Unlike") {
                                viewModel.removeMultipleFromLiked(selectedSongs)
                                overlays.showP("${selectedSongs.size} songs removed from Liked")
                                onClearSelection()
                            }
                        } else {
                            MultiActionItem(R.drawable.ic_xvox_heart, "Like") {
                                viewModel.addMultipleToLiked(selectedSongs)
                                overlays.showP("${selectedSongs.size} songs added to Liked")
                                onClearSelection()
                            }
                        }
                        if (categoryName == "Recently Played") {
                            MultiActionItem(R.drawable.ic_xvox_delete, "Remove") {
                                viewModel.removeMultipleFromRecent(selectedSongs)
                                overlays.showP("${selectedSongs.size} removed from Recently Played")
                                onClearSelection()
                            }
                        } else if (onDeleteSelected != null) {
                            MultiActionItem(R.drawable.ic_xvox_delete, "Delete", onDeleteSelected)
                        }
                        MultiActionItem(R.drawable.ic_xvox_share, "Share") {
                            XvoxSongActions.shareMultiple(context, selectedSongs)
                            onClearSelection()
                        }
                        MultiActionItem(R.drawable.ic_xvox_close, "Cancel", onClearSelection)
                    }

                    // Permanently attached track plus a proportional thumb, on the true right edge.
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(colors.cardBorder.copy(alpha = .62f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = thumbOffset)
                            .width(2.dp)
                            .height(thumbHeight)
                            .background(colors.primaryAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiActionItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MultiActionRowHeight)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = colors.primaryAccent,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            color = colors.primaryText,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
    }
}
