package com.xvox.music.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxLikedSongRow
import com.xvox.music.features.playlist.XvoxPlaylistCard

@Composable
fun HomeCollectionHeader(title: String, count: Int, onAdd: (() -> Unit)? = null) {
    val colors = XvoxTheme.colors
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.primaryAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("$count ${if (title == "Playlists") "playlists" else "songs"}", color = colors.mutedText, fontSize = 10.sp)
        }
        onAdd?.let {
            Icon(painterResource(R.drawable.ic_xvox_add), "Add to $title", tint = colors.primaryAccent,
                modifier = Modifier.size(44.dp).xvoxPressScale(onClick = it).padding(12.dp))
        }
    }
}

fun LazyListScope.librarySongItems(
    keyPrefix: String, title: String, songs: List<Song>, currentSongId: Long?, playing: Boolean,
    selected: Set<Long>, onPlay: (Song) -> Unit, onOptions: (Song) -> Unit, onAdd: (() -> Unit)? = null
) {
    item(key = "${keyPrefix}_header") { HomeCollectionHeader(title, songs.size, onAdd) }
    if (songs.isEmpty()) item(key = "${keyPrefix}_empty") {
        Text("No songs here yet", color = XvoxTheme.colors.mutedText, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp))
    }
    items(songs, key = { "${keyPrefix}_${it.id}" }, contentType = { "song_row" }) { song ->
        XvoxLikedSongRow(song, song.id == currentSongId, song.id == currentSongId && playing,
            onClick = { onPlay(song) }, onOptions = { onOptions(song) }, selected = song.id in selected,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp))
    }
}

fun LazyListScope.playlistCollectionItems(
    playlists: List<XvoxPlaylist>, songsFor: (XvoxPlaylist) -> List<Song>,
    onCreate: () -> Unit, onOpen: (XvoxPlaylist) -> Unit, onOptions: (XvoxPlaylist) -> Unit,
    layoutStyle: String = "long", longCardHeight: Int = 0,
    /** "horizontal" = full-width cards you swipe sideways; "vertical" = stacked full-width cards. */
    orientation: String = "vertical"
) {
    item(key = "playlists_header") { HomeCollectionHeader("Playlists", playlists.size, onCreate) }
    if (playlists.isEmpty()) item(key = "playlists_empty") {
        Text("Create your first playlist", color = XvoxTheme.colors.mutedText, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp))
    }
    if (layoutStyle == "long" && orientation == "horizontal") {
        // One horizontally scrolling row of full-width cards.
        item(key = "playlist_horizontal_row") {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(playlists, key = { "playlist_h_${it.id}" }) { playlist ->
                    XvoxPlaylistCard(playlist, songsFor(playlist), { onOpen(playlist) }, { onOptions(playlist) },
                        Modifier.width(300.dp).height(cardHeight(playlist, longCardHeight)), longCard = true)
                }
            }
        }
    } else if (layoutStyle == "long") {
        items(playlists, key = { "playlist_long_${it.id}" }, contentType = { "playlist_long" }) { playlist ->
            BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp)) {
                // 0 keeps the original proportional height; any other value is the chosen dp height.
                val oldCardHeight = if (longCardHeight > 0) longCardHeight.dp else (maxWidth - 6.dp) / 2 + 35.dp
                XvoxPlaylistCard(playlist, songsFor(playlist), { onOpen(playlist) }, { onOptions(playlist) },
                    Modifier.fillMaxWidth().height(oldCardHeight), longCard = true)
            }
        }
    } else {
    items(playlists.chunked(2), key = { "playlist_row_${it.first().id}" }, contentType = { "playlist_row" }) { row ->
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { playlist ->
                XvoxPlaylistCard(playlist, songsFor(playlist), onClick = { onOpen(playlist) },
                    onLongClick = { onOptions(playlist) }, modifier = Modifier.weight(1f))
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    }

}

/** Card height for the horizontal row: Auto keeps a wide-card proportion, otherwise the dp choice. */
private fun cardHeight(playlist: XvoxPlaylist, longCardHeight: Int) =
    if (longCardHeight > 0) longCardHeight.dp else 178.dp
