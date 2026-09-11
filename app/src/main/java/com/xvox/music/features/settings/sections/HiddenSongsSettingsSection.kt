package com.xvox.music.features.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongArtwork

/** "Deleted songs": what "Delete from XVOX" moves aside. Restoring puts a song or artist straight back. */
@Composable
fun HiddenSongsSettingsSection(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = XvoxTheme.colors
    val byId = remember(state.hiddenSongs) { state.hiddenSongs.associateBy { it.id } }
    val ids = remember(state.hiddenSongIds, byId) { state.hiddenSongIds.sortedBy { byId[it]?.title.orEmpty().lowercase() } }
    val artists = remember(state.hiddenArtists) { state.hiddenArtists.toList().sorted() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.loading) {
            Text("Loading…", color = colors.secondaryText)
        } else if (ids.isEmpty() && artists.isEmpty()) {
            Text("Nothing deleted", color = colors.primaryText, modifier = Modifier.padding(vertical = 18.dp))
        } else {
            val totalCount = ids.size + artists.size
            Button(onClick = viewModel::restoreAllHiddenSongs, modifier = Modifier.fillMaxWidth()) {
                Text("Restore all ($totalCount)")
            }

            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                if (artists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            color = colors.primaryAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(artists, key = { "artist_$it" }) { artist ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.card.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_microphone),
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    artist,
                                    color = colors.primaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Artist",
                                    color = colors.secondaryText,
                                    fontSize = 10.sp
                                )
                            }
                            TextButton(onClick = { viewModel.restoreArtist(artist) }) {
                                Text("Restore", fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (ids.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs",
                            color = colors.primaryAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(ids, key = { "song_$it" }) { id ->
                        val song = byId[id]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.card.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (song != null) {
                                XvoxSongArtwork(
                                    artwork = song.artworkUri,
                                    requestSize = 96,
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(6.dp))
                                )
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    song?.title ?: "File #$id",
                                    color = colors.primaryText,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song?.artist ?: "Not found",
                                    color = colors.secondaryText,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { viewModel.restoreSong(id) }) {
                                Text("Restore", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
