package com.xvox.music.features.search

import android.os.Build
import androidx.activity.result.IntentSenderRequest
import com.xvox.music.features.home.XvoxSongActions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import com.xvox.music.features.home.HomeMultiSelectBar
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxPlaylist
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
    topResetKey: Long = 0L,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val homeState by homeViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesRepository(context) }
    val recentSearches by prefs.recentSearches.collectAsState(initial = emptyList())
    val overlays = LocalXvoxOverlayController.current
    val scope = rememberCoroutineScope()

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val selectedSongs = homeState.songs.filter { it.id in selectedIds }
    val selecting = selectedIds.isNotEmpty()
    val topInset = LocalXvoxTopInset.current
    val bottomInset = LocalXvoxBottomInset.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // Every fresh entry into the Search tab lands on the top, wherever it was left.
    LaunchedEffect(topResetKey) { if (topResetKey > 0L) listState.scrollToItem(0) }
    BackHandler(selecting) { selectedIds = emptySet() }
    LaunchedEffect(query) { selectedIds = emptySet() }
    LaunchedEffect(homeState.songs) { selectedIds = selectedIds.intersect(homeState.songs.mapTo(HashSet()) { it.id }) }
    var pendingDelete by remember { mutableStateOf<List<Song>>(emptyList()) }
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingDelete.isNotEmpty()) {
            pendingDelete.forEach { playerViewModel.removeFromQueue(it.id) }
            val count = pendingDelete.size
            homeViewModel.refresh()
            overlays.showP("$count ${if (count == 1) "song" else "songs"} deleted from device")
        }
        pendingDelete = emptyList()
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

    Column(modifier.fillMaxSize().imePadding()) {
        fun requestDeleteSelected() {
            if (selectedSongs.isEmpty()) return
            overlays.showBox("Delete ${selectedSongs.size} songs?") {
                com.xvox.music.shell.XvoxConfirmBox(
                    question = "Permanently delete ${selectedSongs.size} songs?",
                    detail = "The files will be removed from storage. This cannot be undone.",
                    confirmLabel = "Delete",
                    danger = true,
                    onCancel = overlays::hideBox,
                    onConfirm = {
                        overlays.hideBox()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val pending = XvoxSongActions.deleteMultiplePendingIntent(context, selectedSongs)
                            if (pending != null) {
                                pendingDelete = selectedSongs
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(pending.intentSender).build()
                                )
                            }
                        } else {
                            var count = 0
                            selectedSongs.forEach { s ->
                                if (XvoxSongActions.deleteLegacy(context, s)) {
                                    playerViewModel.removeFromQueue(s.id)
                                    count++
                                }
                            }
                            selectedIds = emptySet()
                            homeViewModel.refresh()
                            overlays.showP("$count songs deleted from device")
                        }
                    }
                )
            }
        }
        if (selecting) {
            Spacer(Modifier.height(topInset))
            HomeMultiSelectBar(selectedSongs, null, XvoxHomeLibraryMode.ALL_SONGS, homeViewModel,
                overlays, context, onClearSelection = { selectedIds = emptySet() },
                onDeleteSelected = ::requestDeleteSelected)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = if (selecting) 8.dp else topInset + 14.dp, bottom = bottomInset)
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
                    fontWeight = FontWeight.Bold
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
                    .background(colors.card)
                    .border(0.7.dp, colors.cardBorder, searchBarShape)
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
                            .background(colors.card)
                            .border(0.7.dp, colors.cardBorder, recentShape)
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
                                    onPlaylistSelected?.invoke(playlist.id)
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
                        selected = song.id in selectedIds,
                        onClick = {
                            if (selecting) selectedIds = if (song.id in selectedIds) selectedIds - song.id else selectedIds + song.id
                            else {
                                addRecent(query)
                                homeViewModel.recordPlayedFromLibrary(song, playerState.currentSongId, "Search")
                                val songQueue = if (homeState.songs.isNotEmpty()) homeState.songs else filteredSongs
                                playerViewModel.playFromSource(song, songQueue, "Search")
                            }
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
                                onPendingDelete = { pendingDelete = listOf(it) },
                                onSelect = { selectedIds = selectedIds + song.id }
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

        item(key = "search_bottom_spacer") {
            Spacer(Modifier.height(8.dp))
        }
    }
}

}
