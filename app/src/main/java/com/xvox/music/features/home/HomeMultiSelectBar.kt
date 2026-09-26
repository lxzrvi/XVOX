package com.xvox.music.features.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.player.playback.MainPlayerViewModel

/**
 * Fixed Home selection chrome. It deliberately has no move handle or drag state: HomeScreen pins
 * it above the page list, so selection actions are available even when the normal Header has
 * already scrolled away.
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
    onClearSelection: () -> Unit,
    onDeleteSelected: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val actionsScroll = rememberScrollState()

    val headerLabel = when {
        !categoryName.isNullOrBlank() -> categoryName
        selectedPlaylist != null -> selectedPlaylist.name
        libraryMode == XvoxHomeLibraryMode.LIKED -> "Liked"
        libraryMode == XvoxHomeLibraryMode.ARTISTS -> "Artists"
        libraryMode == XvoxHomeLibraryMode.PLAYLISTS -> "Playlists"
        else -> "All Songs"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.cardElevated.copy(alpha = .98f))
            .padding(start = 8.dp, top = 5.dp, end = 6.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
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
        Spacer(Modifier.width(7.dp))
        Text(
            text = headerLabel,
            color = colors.primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp)
        )

        // All existing selection operations stay reachable in one fixed header rail. Horizontal
        // scrolling is intentional on narrow phones and never moves the selection bar itself.
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(actionsScroll),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MultiActionItem(
                iconRes = R.drawable.ic_xvox_play,
                label = "Next",
                onClick = {
                    val msg = if (selectedSongs.size == 1) playerViewModel.playNextInQueue(selectedSongs[0])
                    else playerViewModel.playNextInQueue(selectedSongs)
                    overlays.showP(msg)
                    onClearSelection()
                }
            )
            MultiActionItem(
                iconRes = R.drawable.ic_xvox_queue,
                label = "Queue",
                onClick = {
                    overlays.showP(playerViewModel.addToQueue(selectedSongs))
                    onClearSelection()
                }
            )
            MultiActionItem(
                iconRes = R.drawable.ic_xvox_playlist,
                label = "Playlist",
                onClick = {
                    showMultiAddToPlaylistOverlay(
                        overlays = overlays,
                        viewModel = viewModel,
                        songs = selectedSongs,
                        onDone = onClearSelection
                    )
                }
            )
            if (selectedPlaylist != null) {
                MultiActionItem(
                    iconRes = R.drawable.ic_xvox_delete,
                    label = "Remove",
                    onClick = {
                        viewModel.removeMultipleFromPlaylist(selectedPlaylist.id, selectedSongs) {
                            overlays.showP("${selectedSongs.size} songs removed from ${selectedPlaylist.name}")
                            onClearSelection()
                        }
                    }
                )
            } else if (libraryMode == XvoxHomeLibraryMode.LIKED) {
                MultiActionItem(
                    iconRes = R.drawable.ic_xvox_heart_outline,
                    label = "Unlike",
                    onClick = {
                        viewModel.removeMultipleFromLiked(selectedSongs)
                        overlays.showP("${selectedSongs.size} songs removed from Liked")
                        onClearSelection()
                    }
                )
            } else {
                MultiActionItem(
                    iconRes = R.drawable.ic_xvox_heart,
                    label = "Like",
                    onClick = {
                        viewModel.addMultipleToLiked(selectedSongs)
                        overlays.showP("${selectedSongs.size} songs added to Liked")
                        onClearSelection()
                    }
                )
            }
            if (categoryName == "Recently Played") {
                MultiActionItem(
                    iconRes = R.drawable.ic_xvox_delete,
                    label = "Remove",
                    onClick = {
                        viewModel.removeMultipleFromRecent(selectedSongs)
                        overlays.showP("${selectedSongs.size} removed from Recently Played")
                        onClearSelection()
                    }
                )
            } else if (onDeleteSelected != null) {
                MultiActionItem(
                    iconRes = R.drawable.ic_xvox_delete,
                    label = "Delete",
                    onClick = onDeleteSelected
                )
            }
            MultiActionItem(
                iconRes = R.drawable.ic_xvox_share,
                label = "Share",
                onClick = {
                    XvoxSongActions.shareMultiple(context, selectedSongs)
                    onClearSelection()
                }
            )
            MultiActionItem(
                iconRes = R.drawable.ic_xvox_close,
                label = "Cancel",
                onClick = onClearSelection
            )
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = colors.primaryAccent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = colors.primaryText,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
