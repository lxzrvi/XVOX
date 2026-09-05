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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
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
    var query by remember { mutableStateOf("") }
    val selectedSongIds = remember { mutableStateListOf<Long>() }

    val availableSongs = remember(songs, existingSongIds, query) {
        val filtered = songs.filterNot { it.id in existingSongIds }
        if (query.isBlank()) filtered
        else filtered.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        // UNIFIED FLAT HEADER (NO border radius on title and search bar container)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add Songs",
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

            // Search Bar inside flat header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.card)
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_search),
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.primaryText, fontSize = 13.sp),
                        cursorBrush = SolidColor(colors.primaryAccent),
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(text = "Search songs to add...", color = colors.mutedText, fontSize = 13.sp)
                            }
                            inner()
                        }
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = "Clear",
                            tint = colors.secondaryText,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { query = "" }
                        )
                    }
                }
            }
        }

        // Song List with rounded item corners
        if (availableSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isNotBlank()) "No matching songs found" else "All songs are already in this playlist",
                    color = colors.mutedText,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 320.dp),
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

                        // Checkmark multi-select tick
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primaryAccent else Color.Transparent)
                                .border(
                                    width = 1.5.dp,
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
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Bottom Action Buttons
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
