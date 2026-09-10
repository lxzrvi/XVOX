package com.xvox.music.features.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxHomeLibraryMode

@Composable
fun HomeMultiSelectBar(
    selectedSongs: List<Song>,
    selectedPlaylist: XvoxPlaylist?,
    libraryMode: XvoxHomeLibraryMode,
    viewModel: HomeViewModel,
    overlays: XvoxOverlayController,
    context: Context,
    onClearSelection: () -> Unit,
    onDeleteSelected: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors

    Box(
        modifier = modifier
            .padding(top = 10.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardElevated.copy(alpha = 0.96f))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            if (onDeleteSelected != null) {
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
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
