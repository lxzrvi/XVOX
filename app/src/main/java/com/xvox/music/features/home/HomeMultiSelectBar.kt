package com.xvox.music.features.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Selection chrome is a screen-left, vertically centred tool rail. It is intentionally not a
 * top-page Header: selection actions stay available at every scroll depth without competing with
 * the shared Home/Header motion. The left edge is square and flush; only the right side rounds.
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
    val railShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 22.dp, bottomEnd = 22.dp)

    val headerLabel = when {
        !categoryName.isNullOrBlank() -> categoryName
        selectedPlaylist != null -> selectedPlaylist.name
        libraryMode == XvoxHomeLibraryMode.LIKED -> "Liked"
        libraryMode == XvoxHomeLibraryMode.ARTISTS -> "Artists"
        libraryMode == XvoxHomeLibraryMode.PLAYLISTS -> "Playlists"
        else -> "All Songs"
    }

    Column(
        modifier = modifier
            .width(86.dp)
            .heightIn(max = 460.dp)
            .clip(railShape)
            .background(colors.cardElevated.copy(alpha = .98f))
            .verticalScroll(actionsScroll)
            .padding(horizontal = 7.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(31.dp)
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
        Text(
            text = headerLabel,
            color = colors.primaryText,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        MultiActionItem(
            iconRes = R.drawable.ic_xvox_check,
            label = "Select all",
            onClick = onSelectAll
        )
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp)
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
            lineHeight = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
