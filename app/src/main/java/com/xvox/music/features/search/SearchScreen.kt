package com.xvox.music.features.search

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxGlass
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.HomeFooter
import com.xvox.music.features.home.HomeGeometry
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.showPlaylistActions
import com.xvox.music.features.home.showSongOptionsOverlay
import com.xvox.music.features.playlist.XvoxPlaylistCard
import com.xvox.music.player.playback.MainPlayerViewModel
import kotlinx.coroutines.launch

private fun songRelevance(song: Song, query: String): Int {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return 0
    val title = song.title.lowercase()
    val artist = song.artist.lowercase()
    var score = 0
    if (title == q) score = maxOf(score, 100)
    else if (title.startsWith(q)) score = maxOf(score, 90)
    else if (title.contains(" $q") || title.contains("$q ")) score = maxOf(score, 80)
    else if (title.contains(q)) score = maxOf(score, 70)
    if (artist == q) score = maxOf(score, 95)
    else if (artist.startsWith(q)) score = maxOf(score, 85)
    else if (artist.contains(q)) score = maxOf(score, 60)
    return score
}

private fun playlistRelevance(pl: XvoxPlaylist, query: String): Int {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return 0
    val name = pl.name.lowercase()
    return when {
        name == q -> 100
        name.startsWith(q) -> 90
        name.contains(" $q") -> 80
        name.contains(q) -> 70
        else -> 0
    }
}

@Composable
fun SearchScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: MainPlayerViewModel = viewModel(),
    onPlaylistSelected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val homeState by homeViewModel.state.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val recentSearches by prefs.recentSearches.collectAsState(initial = emptyList())
    val overlays = LocalXvoxOverlayController.current
    val scope = rememberCoroutineScope()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDelete?.let { song ->
                playerViewModel.removeFromQueue(song.id)
                homeViewModel.refresh()
                overlays.showP("Deleted from device")
            }
        }
        pendingDelete = null
    }

    val filteredSongs = remember(homeState.songs, query) {
        if (query.isBlank()) emptyList()
        else homeState.songs.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }.sortedByDescending { songRelevance(it, query) }
    }

    val filteredPlaylists = remember(homeState.playlists, query) {
        if (query.isBlank()) emptyList()
        else homeState.playlists.filter { it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { playlistRelevance(it, query) }
    }

    fun addRecent(q: String) {
        val clean = q.trim()
        if (clean.isNotBlank()) {
            scope.launch { prefs.addRecentSearch(clean) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(top = 4.dp)
    ) {
        item(key = "search_header_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = HomeGeometry.sectionGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search",
                    color = colors.primaryAccent,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item(key = "search_bar") {
            val searchBarShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(searchBarShape)
                    .xvoxGlass(
                        shape = searchBarShape,
                        tint = colors.card.copy(alpha = 0.65f),
                        solidFallback = colors.card
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_search),
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(17.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { addRecent(query) }),
                        textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                        cursorBrush = SolidColor(colors.primaryAccent),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(text = "Search songs or playlists", color = colors.mutedText, fontSize = 14.sp)
                                }
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = "Clear",
                            tint = colors.secondaryText,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { query = "" }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (query.isEmpty()) {
            if (recentSearches.isNotEmpty()) {
                item(key = "recent_searches_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            color = colors.primaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Clear",
                            color = colors.secondaryText,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable {
                                scope.launch { prefs.clearRecentSearches() }
                            }
                        )
                    }
                }
                items(recentSearches, key = { "recent_$it" }) { item ->
                    val recentShape = RoundedCornerShape(8.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 3.dp)
                            .clip(recentShape)
                            .xvoxGlass(
                                shape = recentShape,
                                tint = colors.card.copy(alpha = 0.5f),
                                solidFallback = colors.card
                            )
                            .clickable { query = item }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_xvox_search), contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(14.dp))
                        Text(text = item, color = colors.primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = null,
                            tint = colors.mutedText,
                            modifier = Modifier.size(13.dp).clickable {
                                scope.launch { prefs.removeRecentSearch(item) }
                            }
                        )
                    }
                }
            }
        } else {
            if (filteredPlaylists.isNotEmpty()) {
                item(key = "playlists_header") {
                    Text(
                        text = "Playlists (${filteredPlaylists.size})",
                        color = colors.primaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                val playlistChunks = filteredPlaylists.chunked(2)
                items(playlistChunks, key = { chunk -> "pl_row_${chunk.first().id}" }) { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunk.forEach { playlist ->
                            val coverSongs = homeViewModel.playlistSongs(playlist)
                            XvoxPlaylistCard(
                                playlist = playlist,
                                songs = coverSongs,
                                onClick = {
                                    addRecent(query)
                                    if (onPlaylistSelected != null) onPlaylistSelected(playlist.id)
                                },
                                onLongClick = {
                                    showPlaylistActions(overlays, homeViewModel, playlist) {}
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (chunk.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item(key = "playlist_bottom_spacer") { Spacer(Modifier.height(10.dp)) }
            }

            if (filteredSongs.isNotEmpty()) {
                item(key = "songs_header") {
                    Text(
                        text = "Songs (${filteredSongs.size})",
                        color = colors.primaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                items(filteredSongs, key = { it.id }) { song ->
                    val isCurrent = song.id == playerState.currentSongId
                    val isPlaying = isCurrent && playerState.isPlaying
                    SearchSongCard(
                        song = song,
                        current = isCurrent,
                        playing = isPlaying,
                        onClick = {
                            addRecent(query)
                            playerViewModel.playFromSource(song, filteredSongs, "Search")
                        },
                        onOptions = {
                            showSongOptionsOverlay(
                                overlays = overlays,
                                context = context,
                                song = song,
                                isLiked = song.id in homeState.likedSongIds,
                                playlist = null,
                                recent = false,
                                viewModel = homeViewModel,
                                playerViewModel = playerViewModel,
                                playlists = homeState.playlists,
                                songs = homeState.songs,
                                deleteLauncher = deleteLauncher,
                                onPendingDelete = { pendingDelete = it }
                            )
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                    )
                }
            }

            if (filteredPlaylists.isEmpty() && filteredSongs.isEmpty()) {
                item(key = "no_results") {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No songs or playlists found", color = colors.mutedText, fontSize = 13.sp)
                    }
                }
            }
        }

        item(key = "search_footer") {
            HomeFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = screenHeight)
                    .padding(bottom = 20.dp)
            )
        }

        item(key = "search_bottom_inset") { Spacer(Modifier.height(80.dp)) }
    }
}
