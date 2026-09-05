package com.xvox.music.features.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.BasicTextField
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
    onAdd: (Song) -> Unit,
    playlist: XvoxPlaylist? = null,
    playlistSongs: List<Song> = emptyList(),
) {
    val colors =
        XvoxTheme.colors

    var query by remember { mutableStateOf("") }

    val available =
        remember(
            songs,
            existingSongIds,
            query
        ) {
            val filtered = songs.filterNot {
                it.id in existingSongIds
            }
            if (query.isBlank()) filtered else filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
            }
        }

    Column(
        modifier =
            Modifier.fillMaxWidth().imePadding()
    ) {
        if (available.isEmpty()) {
            // Header still shown even if empty, with overlay treatment
            Box(modifier = Modifier.fillMaxWidth().padding(end = 36.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(colors.cardElevated.copy(alpha = 0.82f), RoundedCornerShape(12.dp)).padding(10.dp)) {
                    if (playlist != null) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            XvoxPlaylistCover(
                                songs = playlistSongs.ifEmpty { songs.filter { it.id in playlist.songIds } },
                                coverSongIds = playlist.coverSongIds,
                                customCoverUri = playlist.customCoverUri,
                                requestSize = 128,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(text = playlist.name, color = colors.primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = "${playlist.songIds.size} songs", color = colors.secondaryText, fontSize = 11.sp)
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
                    }
                    Text(text = "Add songs", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).background(colors.card, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(painter = painterResource(R.drawable.ic_xvox_search), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                            BasicTextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                textStyle = TextStyle(color = colors.primaryText, fontSize = 13.sp),
                                cursorBrush = SolidColor(colors.primaryText),
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                decorationBox = { inner -> if (query.isEmpty()) Text(text = "Search songs", color = colors.mutedText, fontSize = 13.sp); inner() }
                            )
                            if (query.isNotEmpty()) {
                                Icon(painter = painterResource(R.drawable.ic_xvox_close), contentDescription = "Clear", tint = colors.secondaryText, modifier = Modifier.size(16.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { query = "" })
                            }
                        }
                    }
                }
            }
            Text(
                text = if (query.isNotBlank()) "No songs match \"$query\"" else "All songs are already in this playlist",
                color = colors.secondaryText, fontSize = 12.sp,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )
            return
        }

        // Layered header-behind scroll: minimal functional padding, no extra outer/inner - use available box area
        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = if (playlist != null) 136.dp else 80.dp, bottom = 12.dp)
            ) {
            items(
                items =
                    available,
                key = {
                    it.id
                }
            ) { song ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null
                    ) { onAdd(song) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XvoxSongArtwork(
                        artwork = song.artworkUri,
                        requestSize = 96,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(9.dp))
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 11.dp, end = 10.dp)) {
                        Text(text = song.title, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = song.artist, color = colors.secondaryText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(modifier = Modifier.size(32.dp).background(colors.card, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painter = painterResource(R.drawable.ic_xvox_plus), contentDescription = "Add", tint = colors.primaryText, modifier = Modifier.size(16.dp))
                    }
                }
            }
            }
            // Header overlay translucent - list scrolls behind, no hard clip, continuous rounded surface
            Column(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().background(colors.cardElevated.copy(alpha = 0.82f), RoundedCornerShape(12.dp)).padding(10.dp)
            ) {
                if (playlist != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        XvoxPlaylistCover(
                            songs = playlistSongs.ifEmpty { songs.filter { it.id in playlist.songIds } },
                            coverSongIds = playlist.coverSongIds,
                            customCoverUri = playlist.customCoverUri,
                            requestSize = 128,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(text = playlist.name, color = colors.primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "${playlist.songIds.size} songs", color = colors.secondaryText, fontSize = 11.sp)
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
                }
                Text(text = "Add songs", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 36.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).background(colors.card, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(painter = painterResource(R.drawable.ic_xvox_search), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                        BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = TextStyle(color = colors.primaryText, fontSize = 13.sp),
                            cursorBrush = SolidColor(colors.primaryText),
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            decorationBox = { inner -> if (query.isEmpty()) Text(text = "Search songs", color = colors.mutedText, fontSize = 13.sp); inner() }
                        )
                        if (query.isNotEmpty()) {
                            Icon(painter = painterResource(R.drawable.ic_xvox_close), contentDescription = "Clear", tint = colors.secondaryText, modifier = Modifier.size(16.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { query = "" })
                        }
                    }
                }
            }
        }
    }
}
