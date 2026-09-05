package com.xvox.music.features.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.SongOptionsSheet
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.player.playback.MainPlayerViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchSongCard(
    song: Song,
    isLiked: Boolean,
    filteredSongs: List<Song>,
    onSearchUsed: () -> Unit,
    homeViewModel: HomeViewModel,
    playerViewModel: MainPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val overlays = LocalXvoxOverlayController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.card)
            .combinedClickable(
                onClick = {
                    haptics.tap()
                    onSearchUsed()
                    playerViewModel.playFromSource(song, filteredSongs, "Search")
                },
                onLongClick = {
                    haptics.heavy()
                    overlays.showL {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        overlays.hideL()
                                        playerViewModel.play(song)
                                        onSearchUsed()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(42.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_xvox_play),
                                        contentDescription = null,
                                        tint = colors.primaryText,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Text(
                                    text = "Play",
                                    color = colors.primaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            SongOptionsSheet(
                                song = song,
                                liked = isLiked,
                                onPlayNext = {
                                    playerViewModel.playNextInQueue(song)
                                    overlays.hideL()
                                    overlays.showP("Playing next")
                                },
                                onAddQueue = {
                                    playerViewModel.addToQueue(song)
                                    overlays.hideL()
                                    overlays.showP("Added to queue")
                                },
                                onPlaylist = { overlays.hideL() },
                                onLiked = {
                                    homeViewModel.toggleLiked(song)
                                    overlays.hideL()
                                },
                                onDelete = { overlays.hideL() },
                                onInfo = { overlays.hideL() },
                                onRingtone = { overlays.hideL() },
                                onShare = { overlays.hideL() }
                            )
                        }
                    }
                }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XvoxSongArtwork(
            artwork = song.artworkUri,
            requestSize = 96,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 8.dp)
        ) {
            Text(
                text = song.title,
                color = colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = colors.secondaryText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isLiked) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_heart),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_xvox_play),
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(14.dp)
        )
    }
}
