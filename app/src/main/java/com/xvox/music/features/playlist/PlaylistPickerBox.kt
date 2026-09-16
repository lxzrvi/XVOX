package com.xvox.music.features.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.XvoxPlaylist

@Composable
fun PlaylistPickerBox(
    song: Song? = null,
    selectedSongs: List<Song> = emptyList(),
    playlists: List<XvoxPlaylist>,
    onCreate: () -> Unit,
    onAdd: (XvoxPlaylist) -> Unit,
    onRemove: ((XvoxPlaylist) -> Unit)? = null,
    onAddCustomList: ((XvoxPlaylist, List<Song>) -> Unit)? = null,
    songs: List<Song> = emptyList(),
    songsFor: ((XvoxPlaylist) -> List<Song>)? = null
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    val targetSongs = remember(song, selectedSongs) {
        if (selectedSongs.isNotEmpty()) selectedSongs
        else if (song != null) listOf(song)
        else emptyList()
    }

    val isSingleSong = targetSongs.size <= 1
    val singleSong = targetSongs.firstOrNull()

    var singleSongToRemoveFrom by remember { mutableStateOf<XvoxPlaylist?>(null) }
    var batchConflictPlaylist by remember { mutableStateOf<Pair<XvoxPlaylist, List<Song>>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        if (singleSongToRemoveFrom != null && singleSong != null) {
            val target = singleSongToRemoveFrom!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Song Already in ${target.name}",
                    color = colors.primaryAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Do you want to remove \"${singleSong.title}\" from ${target.name} or keep it?",
                    color = colors.secondaryText,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.cardElevated)
                            .xvoxPressScale {
                                haptics.tap()
                                singleSongToRemoveFrom = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Keep / Cancel", color = colors.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFF5252))
                            .xvoxPressScale {
                                haptics.success()
                                singleSongToRemoveFrom = null
                                onRemove?.invoke(target)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Remove", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (batchConflictPlaylist != null) {
            val (target, alreadyIn) = batchConflictPlaylist!!
            val newSongs = targetSongs.filterNot { it.id in target.songIds }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add to ${target.name}?",
                    color = colors.primaryAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${alreadyIn.size} of ${targetSongs.size} selected songs are already in this playlist.",
                    color = colors.secondaryText,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (newSongs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.cardElevated)
                                .xvoxPressScale {
                                    haptics.tap()
                                    batchConflictPlaylist = null
                                    if (onAddCustomList != null) {
                                        onAddCustomList(target, newSongs)
                                    } else {
                                        onAdd(target)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Add remaining ${newSongs.size} songs",
                                color = colors.primaryAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.20f))
                            .xvoxPressScale {
                                haptics.success()
                                batchConflictPlaylist = null
                                if (onAddCustomList != null) {
                                    onAddCustomList(target, targetSongs)
                                } else {
                                    onAdd(target)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Add all ${targetSongs.size} songs",
                            color = colors.primaryAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.card)
                            .xvoxPressScale {
                                haptics.tap()
                                batchConflictPlaylist = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = colors.secondaryText, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSingleSong) "Add / Remove Playlist" else "Add ${targetSongs.size} songs to Playlist",
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
                        val singleContains = isSingleSong && singleSong != null && singleSong.id in playlist.songIds
                        val batchAlreadyIn = if (!isSingleSong) targetSongs.filter { it.id in playlist.songIds } else emptyList()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.tap()
                                    if (isSingleSong) {
                                        if (singleContains) {
                                            singleSongToRemoveFrom = playlist
                                        } else {
                                            onAdd(playlist)
                                        }
                                    } else {
                                        if (batchAlreadyIn.isNotEmpty()) {
                                            batchConflictPlaylist = playlist to batchAlreadyIn
                                        } else {
                                            onAdd(playlist)
                                        }
                                    }
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
                                    text = "${playlist.songIds.size} songs",
                                    color = colors.secondaryText,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            if (isSingleSong && singleContains) {
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
}
