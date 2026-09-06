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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun CreatePlaylistBox(
    songs: List<Song>,
    initialSong: Song?,
    onCreate: (String, Set<Long>) -> Unit
) {
    val colors = XvoxTheme.colors

    var name by remember { mutableStateOf("") }
    val safeSongs = remember(songs) { songs.distinctBy { it.id } }

    val selected = remember(initialSong?.id) {
        mutableStateListOf<Long>().apply {
            initialSong?.let { add(it.id) }
        }
    }

    val selectedSongs = remember(selected.toList(), safeSongs) {
        safeSongs.filter { it.id in selected }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp, max = 520.dp)
            .imePadding()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_plus),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )

            Text(
                text = "Create Playlist",
                color = colors.primaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedSongs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card),
                    contentAlignment = Alignment.Center
                ) {
                    XvoxPlaylistCover(
                        songs = selectedSongs,
                        coverSongIds = selected.take(4),
                        customCoverUri = null,
                        requestSize = 128,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))
            }

            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.primaryAccent),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card),
                decorationBox = { field ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
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
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Select Songs (${selected.size} selected)",
            color = colors.secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(Modifier.height(4.dp))

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
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (checked) colors.primaryAccent else colors.card),
                        contentAlignment = Alignment.Center
                    ) {
                        if (checked) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_check),
                                contentDescription = null,
                                tint = colors.background,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_plus),
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (name.isNotBlank()) colors.primaryAccent else colors.cardElevated)
                .clickable(enabled = name.isNotBlank()) {
                    onCreate(name.trim(), selected.toSet())
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Create",
                color = if (name.isNotBlank()) colors.background else colors.mutedText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
