package com.xvox.music.features.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.home.HomeGreeting
import com.xvox.music.features.home.HomeProfileAvatar
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.SongOptionsSheet
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.showPlaylistActions
import com.xvox.music.features.home.HomeFooter
import com.xvox.music.features.playlist.XvoxPlaylistCover
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

@Composable
private fun SearchHeaderNoPill(
    profile: com.xvox.music.data.preferences.UserPreferences
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeProfileAvatar(
            profile = profile,
            modifier = Modifier.size(42.dp),
            onClick = {}
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.username,
                color = colors.primaryText,
                fontFamily = com.xvox.music.core.design.theme.XvoxPersonalFont,
                fontSize = 18.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HomeGreeting()
        }
        // Intentionally no pill (7)
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
    val homeState by homeViewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val recentSearches by prefs.recentSearches.collectAsState(initial = emptyList())
    val overlays = LocalXvoxOverlayController.current
    val scope = rememberCoroutineScope()

    val filteredSongsRaw = remember(homeState.songs, query) {
        if (query.isBlank()) emptyList()
        else homeState.songs.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }.sortedByDescending { songRelevance(it, query) }
    }
    val filteredPlaylistsRaw = remember(homeState.playlists, query) {
        if (query.isBlank()) emptyList()
        else homeState.playlists.filter { it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { playlistRelevance(it, query) }
    }

    // 6 – relevance-based section order
    val topIsPlaylists = remember(filteredSongsRaw, filteredPlaylistsRaw, query) {
        if (filteredSongsRaw.isEmpty()) true
        else if (filteredPlaylistsRaw.isEmpty()) false
        else {
            val bestSong = filteredSongsRaw.firstOrNull()?.let { songRelevance(it, query) } ?: 0
            val bestPl = filteredPlaylistsRaw.firstOrNull()?.let { playlistRelevance(it, query) } ?: 0
            // if playlist stronger, playlists first; tie -> songs first (preserve familiar)
            bestPl > bestSong
        }
    }
    val filteredSongs = filteredSongsRaw
    val filteredPlaylists = filteredPlaylistsRaw

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
        // 7 — Home-style header WITHOUT pill, scrolls with content
        item(key = "search_home_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background.copy(alpha = 0.78f))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(bottom = 8.dp)
            ) {
                SearchHeaderNoPill(profile = homeState.profile)
            }
        }

        item(key = "search_title") {
            Text(
                text = "Search",
                color = colors.primaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        item(key = "search_bar") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .background(colors.card, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(painter = painterResource(R.drawable.ic_xvox_search), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                        cursorBrush = SolidColor(colors.primaryText),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(text = "Search any song or playlist", color = colors.mutedText, fontSize = 14.sp)
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
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { query = "" }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (query.isBlank()) {
            if (recentSearches.isNotEmpty()) {
                item(key = "recent_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Recent Searches", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            text = "Clear",
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { scope.launch { prefs.clearRecentSearches() } }.padding(4.dp)
                        )
                    }
                }
                items(recentSearches, key = { "recent_$it" }) { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { query = recent }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_xvox_search), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(14.dp))
                        Text(text = recent, color = colors.primaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = "Remove",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(14.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { scope.launch { prefs.removeRecentSearch(recent) } }
                        )
                    }
                }
            } else {
                item(key = "recent_empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Search any song or playlist", color = colors.secondaryText, fontSize = 13.sp)
                    }
                }
            }
            // 1 – bottom brand space even in empty recent state
            item(key = "search_brand_empty") {
                Spacer(Modifier.height(24.dp))
                HomeFooter(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(bottom = 20.dp)
                )
            }
        } else {
            if (filteredSongs.isEmpty() && filteredPlaylists.isEmpty()) {
                item(key = "no_results") {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No results for \"$query\"", color = colors.secondaryText, fontSize = 13.sp)
                    }
                }
                item(key = "brand_no_results") {
                    Spacer(Modifier.height(24.dp))
                    HomeFooter(modifier = Modifier.fillMaxWidth().height(220.dp).padding(bottom = 20.dp))
                }
            } else {
                // Render sections in relevance order
                if (!topIsPlaylists) {
                    // Songs first
                    if (filteredSongs.isNotEmpty()) {
                        item(key = "songs_header") {
                            Text(
                                text = "Songs",
                                color = colors.primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                        items(filteredSongs, key = { it.id }) { song ->
                            val isLiked = song.id in homeState.likedSongIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.card)
                                    .combinedClickable(
                                        onClick = {
                                            addRecent(query)
                                            playerViewModel.playFromSource(song, filteredSongs, "Search")
                                        },
                                        onLongClick = {
                                            // 3 – Global XvoxL with Play not clipped (height increased globally 0.78)
                                            overlays.showL {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
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
                                    Icon(painter = painterResource(R.drawable.ic_xvox_heart), contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                                }
                                Icon(painter = painterResource(R.drawable.ic_xvox_play), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (filteredPlaylists.isNotEmpty()) {
                        item(key = "playlists_header") {
                            Text(
                                text = "Playlists",
                                color = colors.primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                        items(filteredPlaylists, key = { it.id }) { playlist ->
                            val coverSongs = homeViewModel.playlistSongs(playlist)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.card)
                                    .combinedClickable(
                                        onClick = {
                                            addRecent(query)
                                            if (onPlaylistSelected != null) onPlaylistSelected(playlist.id)
                                        },
                                        onLongClick = { showPlaylistActions(overlays, homeViewModel, playlist) {} }
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                XvoxPlaylistCover(
                                    songs = coverSongs,
                                    coverSongIds = playlist.coverSongIds,
                                    customCoverUri = playlist.customCoverUri,
                                    requestSize = 96,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
                                    Text(text = playlist.name, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "${playlist.songIds.size} songs", color = colors.secondaryText, fontSize = 11.sp, maxLines = 1)
                                }
                                Icon(painter = painterResource(R.drawable.ic_xvox_playlist), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    // Playlists first (relevance)
                    if (filteredPlaylists.isNotEmpty()) {
                        item(key = "playlists_header_top") {
                            Text(
                                text = "Playlists",
                                color = colors.primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                        items(filteredPlaylists, key = { it.id }) { playlist ->
                            val coverSongs = homeViewModel.playlistSongs(playlist)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.card)
                                    .combinedClickable(
                                        onClick = {
                                            addRecent(query)
                                            if (onPlaylistSelected != null) onPlaylistSelected(playlist.id)
                                        },
                                        onLongClick = { showPlaylistActions(overlays, homeViewModel, playlist) {} }
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                XvoxPlaylistCover(
                                    songs = coverSongs,
                                    coverSongIds = playlist.coverSongIds,
                                    customCoverUri = playlist.customCoverUri,
                                    requestSize = 96,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
                                    Text(text = playlist.name, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "${playlist.songIds.size} songs", color = colors.secondaryText, fontSize = 11.sp, maxLines = 1)
                                }
                                Icon(painter = painterResource(R.drawable.ic_xvox_playlist), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (filteredSongs.isNotEmpty()) {
                        item(key = "songs_header_bottom") {
                            Text(
                                text = "Songs",
                                color = colors.primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                        items(filteredSongs, key = { it.id }) { song ->
                            val isLiked = song.id in homeState.likedSongIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.card)
                                    .combinedClickable(
                                        onClick = {
                                            addRecent(query)
                                            playerViewModel.playFromSource(song, filteredSongs, "Search")
                                        },
                                        onLongClick = {
                                            overlays.showL {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
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
                                    Icon(painter = painterResource(R.drawable.ic_xvox_heart), contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                                }
                                Icon(painter = painterResource(R.drawable.ic_xvox_play), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                // 1 – bottom brand space after results
                item(key = "search_brand_results") {
                    Spacer(Modifier.height(24.dp))
                    HomeFooter(modifier = Modifier.fillMaxWidth().height(220.dp).padding(bottom = 20.dp))
                }
            }
        }

        item(key = "search_bottom_inset") { Spacer(Modifier.height(80.dp)) }
    }
}
