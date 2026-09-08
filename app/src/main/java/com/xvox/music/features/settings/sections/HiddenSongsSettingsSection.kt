package com.xvox.music.features.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongArtwork

@Composable
fun HiddenSongsSettingsSection(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = XvoxTheme.colors
    val byId = remember(state.hiddenSongs) { state.hiddenSongs.associateBy { it.id } }
    val ids = remember(state.hiddenSongIds, byId) { state.hiddenSongIds.sortedBy { byId[it]?.title.orEmpty().lowercase() } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Songs removed with ‘Remove from XVOX’ stay hidden here. Restore returns them to the library without changing likes or playlists.", color = colors.secondaryText, fontSize = 12.sp)
        Text("Files deleted from the device cannot be recovered here. Other duration, size and folder filters still apply after restoring.", color = colors.mutedText, fontSize = 11.sp)
        if (state.loading) Text("Loading the device library…", color = colors.secondaryText)
        else if (ids.isEmpty()) Text("No hidden songs", color = colors.primaryText, modifier = Modifier.padding(vertical = 18.dp))
        else {
            Button(onClick = viewModel::restoreAllHiddenSongs, modifier = Modifier.fillMaxWidth()) { Text("Restore all (${ids.size})") }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(ids, key = { it }) { id ->
                    val song = byId[id]
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        XvoxSongArtwork(song?.artworkUri, requestSize = 96, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(song?.title ?: "Unavailable file #$id", color = colors.primaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song?.artist ?: "Reconnect storage or rescan to locate it", color = colors.secondaryText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { viewModel.restoreSong(id) }) { Text(if (song != null) "Restore" else "Unhide", fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}
