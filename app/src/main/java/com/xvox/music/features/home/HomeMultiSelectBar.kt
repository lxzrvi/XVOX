package com.xvox.music.features.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val headerLabel = when {
        !categoryName.isNullOrBlank() -> categoryName
        selectedPlaylist != null -> selectedPlaylist.name
        libraryMode == XvoxHomeLibraryMode.LIKED -> "Liked Songs"
        libraryMode == XvoxHomeLibraryMode.ARTISTS -> "Artists"
        libraryMode == XvoxHomeLibraryMode.PLAYLISTS -> "Playlists"
        else -> "All Songs"
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX
                translationY = offsetY
            }
            .padding(top = 2.dp, bottom = 6.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Indicator Bar with Category Name & Selection Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$headerLabel · ${selectedSongs.size} selected",
                color = colors.primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Tap songs to add/remove",
                color = colors.secondaryText,
                fontSize = 11.sp
            )
        }

        // Action Row with Floating Draggable 6-Dot Handle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.cardElevated.copy(alpha = 0.96f))
                .padding(start = 4.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MultiActionItem(
                iconRes = R.drawable.ic_xvox_play,
                label = "Play next",
                onClick = {
                    val msg = if (selectedSongs.size == 1) {
                        playerViewModel.playNextInQueue(selectedSongs[0])
                    } else {
                        playerViewModel.playNextInQueue(selectedSongs)
                    }
                    overlays.showP(msg)
                    onClearSelection()
                }
            )

            MultiActionItem(
                iconRes = R.drawable.ic_xvox_queue,
                label = "Add queue",
                onClick = {
                    showMultiAddToQueueOverlay(
                        overlays = overlays,
                        playerViewModel = playerViewModel,
                        songs = selectedSongs,
                        onDone = onClearSelection
                    )
                }
            )

            MultiActionItem(
                iconRes = R.drawable.ic_xvox_playlist,
                label = "Playlist",
                onClick = {
                    showMultiAddToPlaylistOverlay(
                        overlays = overlays,
                        viewModel = viewModel,
                        songs = selectedSongs
                    ) {
                        onClearSelection()
                    }
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
                    iconRes = R.drawable.ic_xvox_close,
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

            // 6-Dot Draggable Move Handle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(Modifier.size(3.dp).clip(CircleShape).background(colors.primaryAccent))
                            Box(Modifier.size(3.dp).clip(CircleShape).background(colors.primaryAccent))
                        }
                    }
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = colors.primaryAccent,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = colors.primaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
