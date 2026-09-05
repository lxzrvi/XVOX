package com.xvox.music.features.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.XvoxPlaylist

@Composable
fun PlaylistPickerBox(
    song: Song,
    playlists: List<XvoxPlaylist>,
    onCreate: () -> Unit,
    onAdd: (XvoxPlaylist) -> Unit,
    onRemove: (XvoxPlaylist) -> Unit,
    songs: List<Song> = emptyList(),
    songsFor: ((XvoxPlaylist) -> List<Song>)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add to Playlist",
                color = colors.primaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .clickable {
                        haptics.tap()
                        onCreate()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_plus),
                    contentDescription = "Create playlist",
                    tint = colors.primaryText,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        haptics.tap()
                        onCreate()
                    }
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.cardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_plus),
                        contentDescription = null,
                        tint = colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Create a new playlist",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val safePlaylists = remember(playlists) { playlists.distinctBy { it.id } }
            LazyColumn(
                modifier = Modifier.heightIn(max = 340.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(items = safePlaylists, key = { "pl_${it.id}" }) { playlist ->
                    val contains = song.id in playlist.songIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.tap()
                                if (contains) onRemove(playlist) else onAdd(playlist)
                            }
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val coverSongs = if (songsFor != null) {
                            songsFor(playlist)
                        } else {
                            if (songs.isNotEmpty()) songs.filter { it.id in playlist.songIds } else emptyList()
                        }
                        XvoxPlaylistCover(
                            songs = coverSongs,
                            coverSongIds = playlist.coverSongIds,
                            customCoverUri = playlist.customCoverUri,
                            requestSize = 96,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp, end = 8.dp)
                        ) {
                            Text(
                                text = playlist.name,
                                color = colors.primaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (contains) "Added" else "${playlist.songIds.size} songs",
                                color = if (contains) colors.primaryAccent else colors.secondaryText,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        if (contains) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_check),
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
