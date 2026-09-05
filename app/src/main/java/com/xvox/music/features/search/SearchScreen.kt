package com.xvox.music.features.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.HomeFooter
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.SongOptionsSheet
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.library.PlaylistCard
import com.xvox.music.features.home.showPlaylistActions
import com.xvox.music.player.playback.MainPlayerViewModel
import kotlinx.coroutines.launch

private fun songRelevance(song: com.xvox.music.core.model.Song, query: String): Int {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return 0
    val title = song.title.lowercase()
    val artist = song.artist.lowercase()
    var score = 0
    if (title == q) score = maxOf(score, 100)
    else if (title.startsWith(q)) score = maxOf(score, 90)
    else if (title.contains(" $q") || title.contains(q + " ")) score = maxOf(score, 80)
    else if (title.contains(q)) score = maxOf(score, 70)
    if (artist == q) score = maxOf(score, 95)
    else if (artist.startsWith(q)) score = maxOf(score, 85)
    else if (artist.contains(q)) score = maxOf(score, 60)
    return score
}

private fun playlistRelevance(pl: com.xvox.music.data.preferences.XvoxPlaylist, query: String): Int {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: MainPlayerViewModel = viewModel(),
    onPlaylistSelected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val homeState by homeViewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val recentSearches by prefs.recentSearches.collectAsState(initial = emptyList())
    val overlays = LocalXvoxOverlayController.current
    val scope = rememberCoroutineScope()

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
        if (q.isBlank()) return
        scope.launch { prefs.addRecentSearch(q) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        // Search Header Title matching All Songs typography & position
        item(key = "search_header_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 10.dp,
                    ),
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

        // Search Bar
        item(key = "search_bar") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
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
                                .clickable {
                                    haptics.tap()
                                    query = ""
                                }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Search Content
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
                                haptics.tap()
                                scope.launch { prefs.clearRecentSearches() }
                            }
                        )
                    }
                }
                items(recentSearches, key = { "recent_$it" }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.tap()
                                query = item
                            }
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
                                haptics.tap()
                                scope.launch { prefs.removeRecentSearch(item) }
                            }
                        )
                    }
                }
            }
        } else {
            // Playlists Results as Box Grid
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
                            PlaylistCard(
                                playlist = playlist,
                                songs = coverSongs,
                                onClick = {
                                    haptics.tap()
                                    addRecent(query)
                                    if (onPlaylistSelected != null) onPlaylistSelected(playlist.id)
                                },
                                onLongClick = {
                                    haptics.heavy()
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

            // Songs Results
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
                    val isLiked = song.id in homeState.likedSongIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .combinedClickable(
                                onClick = {
                                    haptics.tap()
                                    addRecent(query)
                                    playerViewModel.playFromSource(song, filteredSongs, "Search")
                                },
                                onLongClick = {
                                    haptics.heavy()
                                    overlays.showL {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    overlays.hideL()
                                                    playerViewModel.play(song)
                                                    addRecent(query)
                                                }.padding(vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                                    Icon(painter = painterResource(R.drawable.ic_xvox_play), contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(19.dp))
                                                }
                                                Text(text = "Play", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                            SongOptionsSheet(
                                                song = song,
                                                liked = isLiked,
                                                onPlayNext = {
                                                    playerViewModel.playNextInQueue(song)
                                                    overlays.hideL()
                                                    overlays.showP("Playing next")
                                                },
                                                onAddQueue = {
                                                    playerViewModel.addToQueue(song)
                                                    overlays.hideL()
                                                    overlays.showP("Added to queue")
                                                },
                                                onPlaylist = { overlays.hideL() },
                                                onLiked = {
                                                    homeViewModel.toggleLiked(song)
                                                    overlays.hideL()
                                                },
                                                onDelete = { overlays.hideL() },
                                                onInfo = { overlays.hideL() },
                                                onRingtone = { overlays.hideL() },
                                                onShare = { overlays.hideL() }
                                            )
                                        }
                                    }
                                }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XvoxSongArtwork(artwork = song.artworkUri, requestSize = 96, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
                            Text(text = song.title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = song.artist, color = colors.secondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (isLiked) {
                            Icon(painter = painterResource(R.drawable.ic_xvox_heart), contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                        }
                        Icon(painter = painterResource(R.drawable.ic_xvox_play), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(14.dp))
                    }
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
            Spacer(Modifier.height(16.dp))
            HomeFooter(modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 20.dp))
        }

        item(key = "search_bottom_inset") { Spacer(Modifier.height(80.dp)) }
    }
}
