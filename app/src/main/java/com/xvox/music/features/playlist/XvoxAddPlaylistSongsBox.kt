package com.xvox.music.features.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun XvoxAddPlaylistSongsBox(
    songs: List<Song>,
    existingSongIds: Set<Long>,
    onAddMultiple: (List<Song>) -> Unit,
    onCancel: () -> Unit,
    playlist: XvoxPlaylist? = null,
    playlistSongs: List<Song> = emptyList(),
) {
    val colors = XvoxTheme.colors
    val selectedSongIds = remember { mutableStateListOf<Long>() }

    val availableSongs = remember(songs, existingSongIds) {
        songs.filterNot { it.id in existingSongIds }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 660.dp)
            .imePadding()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add Songs to Playlist",
                    color = colors.primaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                if (playlist != null) {
                    Text(
                        text = "To ${playlist.name}",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selectedSongIds.isNotEmpty()) {
                Text(
                    text = "${selectedSongIds.size} selected",
                    color = colors.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (availableSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp, max = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All songs are already in this playlist",
                    color = colors.mutedText,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(availableSongs, key = { it.id }) { song ->
                    val isSelected = song.id in selectedSongIds

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.card.copy(alpha = 0.95f) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isSelected) {
                                    selectedSongIds.remove(song.id)
                                } else {
                                    selectedSongIds.add(song.id)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XvoxSongArtwork(
                            artwork = song.artworkUri,
                            requestSize = 96,
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp, end = 10.dp)
                        ) {
                            Text(
                                text = song.title,
                                color = if (isSelected) colors.primaryAccent else colors.primaryText,
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

                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primaryAccent else colors.card)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colors.primaryAccent else colors.cardBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_check),
                                    contentDescription = "Selected",
                                    tint = colors.background,
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_plus),
                                    contentDescription = "Add",
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.card,
                    contentColor = colors.primaryText
                )
            ) {
                Text(text = "Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    val selected = songs.filter { it.id in selectedSongIds }
                    if (selected.isNotEmpty()) {
                        onAddMultiple(selected)
                    }
                },
                enabled = selectedSongIds.isNotEmpty(),
                modifier = Modifier.weight(1.5f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.background,
                    disabledContainerColor = colors.cardElevated,
                    disabledContentColor = colors.mutedText
                )
            ) {
                Text(
                    text = if (selectedSongIds.isEmpty()) "Select Songs" else "Add (${selectedSongIds.size}) Songs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
