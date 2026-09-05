package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxPlaylistCover

@Composable
fun PlaylistPickerBox(
    song: Song,
    playlists: List<XvoxPlaylist>,
    onCreate: () -> Unit,
    onAdd: (XvoxPlaylist) -> Unit,
    onRemove: (XvoxPlaylist) -> Unit,
    songs: List<Song> = emptyList(),
    songsFor: ((XvoxPlaylist) -> List<Song>)? = null,
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
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
                        val coverSongs = if (songsFor != null) songsFor(playlist) else {
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

@Composable
fun CreatePlaylistBox(
    songs: List<Song>,
    initialSong: Song?,
    onCreate: (String, Set<Long>) -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    var name by remember { mutableStateOf("") }
    val safeSongs = remember(songs) { songs.distinctBy { it.id } }

    val selected = remember(initialSong?.id) {
        mutableStateListOf<Long>().apply {
            initialSong?.let { add(it.id) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 380.dp, max = 520.dp)
            .imePadding()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // Header
        Text(
            text = "Create Playlist",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Name input
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
            cursorBrush = SolidColor(colors.primaryAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card),
            decorationBox = { field ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (name.isEmpty()) {
                        Text(text = "Playlist name", color = colors.secondaryText, fontSize = 13.sp)
                    }
                    field()
                }
            }
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Select Songs (${selected.size} selected)",
            color = colors.secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(Modifier.height(6.dp))

        // Songs list with distinct items and keys
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 6.dp)
        ) {
            items(items = safeSongs, key = { "create_pl_${it.id}" }) { song ->
                val checked = song.id in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            haptics.tap()
                            if (checked) selected.remove(song.id) else selected.add(song.id)
                        }
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        requestSize = 96,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp, end = 10.dp)
                    ) {
                        Text(
                            text = song.title,
                            color = colors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = colors.secondaryText,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (checked) colors.primaryAccent else colors.cardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (checked) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_check),
                                contentDescription = null,
                                tint = colors.background,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Compact Create Button with text-according width and bottom margin
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .wrapContentWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (name.isNotBlank()) colors.primaryAccent else colors.cardElevated)
                .clickable(enabled = name.isNotBlank()) {
                    runCatching {
                        haptics.success()
                        onCreate(name.trim(), selected.toSet())
                    }
                }
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Create",
                color = if (name.isNotBlank()) colors.background else colors.mutedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(10.dp))
    }
}
