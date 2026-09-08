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

/** "Deleted songs": what "Delete from XVOX" moves aside. Restoring puts a song straight back. */
@Composable
fun HiddenSongsSettingsSection(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = XvoxTheme.colors
    val byId = remember(state.hiddenSongs) { state.hiddenSongs.associateBy { it.id } }
    val ids = remember(state.hiddenSongIds, byId) { state.hiddenSongIds.sortedBy { byId[it]?.title.orEmpty().lowercase() } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.loading) Text("Loading…", color = colors.secondaryText)
        else if (ids.isEmpty()) Text("Nothing deleted", color = colors.primaryText, modifier = Modifier.padding(vertical = 18.dp))
        else {
            Button(onClick = viewModel::restoreAllHiddenSongs, modifier = Modifier.fillMaxWidth()) { Text("Restore all (${ids.size})") }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(ids, key = { it }) { id ->
                    val song = byId[id]
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        XvoxSongArtwork(song?.artworkUri, requestSize = 96, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(song?.title ?: "File #$id", color = colors.primaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song?.artist ?: "Not found", color = colors.secondaryText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { viewModel.restoreSong(id) }) { Text("Restore", fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}
