package com.xvox.music.features.search

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.artist.ArtistSquareItem
import com.xvox.music.features.home.HomeMultiSelectBar
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongActions
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
    val listState = rememberLazyListState()

    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(topResetKey) {
        if (topResetKey > 0L) listState.scrollToItem(0)
    }

    BackHandler(selecting) { selectedIds = emptySet() }
    LaunchedEffect(query) { selectedIds = emptySet() }
    LaunchedEffect(homeState.songs) {
        selectedIds = selectedIds.intersect(homeState.songs.mapTo(HashSet()) { it.id })
    }

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

    val allArtists = remember(homeState.songs, homeState.customArtistImages, homeState.hiddenArtists, homeState.artistRenames) {
        val songsWithRenames = homeState.songs.map { song ->
            val renamed = homeState.artistRenames[song.artist]
            if (renamed != null) song.copy(artist = renamed) else song
        }
        songsWithRenames
            .filterNot { it.artist in homeState.hiddenArtists }
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { (artistName, songList) ->
                val cover = songList.firstOrNull { it.artworkUri != null } ?: songList.firstOrNull()
                com.xvox.music.features.artist.XvoxArtist(
                    name = artistName,
                    songs = songList,
                    coverSong = cover,
                    customImageUri = homeState.customArtistImages[artistName]
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val filteredArtists = remember(allArtists, query, homeState.hiddenSearchArtists) {
        if (query.isBlank()) emptyList()
        else allArtists
            .filterNot { it.name in homeState.hiddenSearchArtists }
            .filter { it.name.contains(query, ignoreCase = true) }
    }

    val filteredPlaylists = remember(homeState.playlists, query, homeState.hiddenSearchPlaylists) {
        if (query.isBlank()) emptyList()
        else homeState.playlists
            .filterNot { it.id in homeState.hiddenSearchPlaylists }
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { playlistRelevance(it, query) }
    }

    fun addRecent(q: String) {
        val clean = q.trim()
        if (clean.isNotBlank()) {
            scope.launch { prefs.addRecentSearch(clean) }
        }
    }

    fun showSearchPlaylistOptions(playlist: XvoxPlaylist, playlistTracks: List<Song>) {
        showPlaylistActions(
            overlays = overlays,
            context = context,
            playlist = playlist,
            songs = playlistTracks,
            allSongs = homeState.songs,
            viewModel = homeViewModel,
            playerViewModel = playerViewModel,
            onHide = {
                homeViewModel.hideSearchPlaylist(playlist.id)
                overlays.showP("Hidden from search: ${playlist.name}")
            }
        )
    }

    fun showSearchArtistOptions(artist: com.xvox.music.features.artist.XvoxArtist) {
        overlays.showBox(artist.name) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            overlays.hideBox()
                            val song = artist.songs.firstOrNull()
                            if (song != null) {
                                homeViewModel.recordPlayedFromLibrary(song, playerState.currentSongId, "Playing by " + artist.name)
                                playerViewModel.playFromSource(song, artist.songs, "Playing by " + artist.name)
                            }
                        }
                        .padding(14.dp)
                ) {
                    Text("Play all (${artist.songs.size})", color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            overlays.hideBox()
                            homeViewModel.hideSearchArtist(artist.name)
                            overlays.showP("Hidden from search: ${artist.name}")
                        }
                        .padding(14.dp)
                ) {
                    Text("Hide from search", color = colors.secondaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    @Composable
    fun SearchInputField() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .border(1.dp, colors.cardBorder.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_search),
                contentDescription = null,
                tint = colors.primaryAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(
                    color = colors.primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(colors.primaryAccent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { addRecent(query) }),
                decorationBox = { innerTextField ->
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search songs, artists, playlists...",
                                color = colors.mutedText,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_close),
                    contentDescription = "Clear",
                    tint = colors.secondaryText,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { query = "" }
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        if (selecting) {
            Spacer(Modifier.height(topInset))
            HomeMultiSelectBar(
                selectedSongs = selectedSongs,
                selectedPlaylist = null,
                libraryMode = com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS,
                categoryName = "Search",
                viewModel = homeViewModel,
                playerViewModel = playerViewModel,
                overlays = overlays,
                onClearSelection = { selectedIds = emptySet() },
                onDeleteSelected = {
                    overlays.showBox("Delete ${selectedSongs.size} songs?") {
                        com.xvox.music.shell.XvoxConfirmBox(
                            question = "Permanently delete ${selectedSongs.size} songs?",
                            detail = "The files will be removed from device storage.",
                            confirmLabel = "Delete",
                            danger = true,
                            onCancel = overlays::hideBox,
                            onConfirm = {
                                overlays.hideBox()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val pending = XvoxSongActions.deleteMultiplePendingIntent(context, selectedSongs)
                                    if (pending != null) {
                                        pendingDelete = selectedSongs
                                        deleteLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
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
            )
        }

        if (isLandscape) {
            // Responsive 2-Pane Split in Landscape Mode
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset, bottom = bottomInset, start = 14.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane (~38%): Search input + Recent Searches list
                Column(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SearchInputField()

                    if (recentSearches.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recent Searches", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Clear",
                                color = colors.mutedText,
                                fontSize = 11.5.sp,
                                modifier = Modifier.clickable { scope.launch { prefs.clearRecentSearches() } }
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(recentSearches, key = { "recent_$it" }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.card)
                                        .clickable { query = item; addRecent(item) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = item, color = colors.primaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Icon(
                                        painter = painterResource(R.drawable.ic_xvox_close),
                                        contentDescription = "Remove",
                                        tint = colors.mutedText,
                                        modifier = Modifier.size(15.dp).clickable { scope.launch { prefs.removeRecentSearch(item) } }
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Pane (~62%): Search Results
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(0.62f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    renderSearchResults(
                        query = query,
                        colors = colors,
                        filteredArtists = filteredArtists,
                        filteredPlaylists = filteredPlaylists,
                        filteredSongs = filteredSongs,
                        playerState = playerState,
                        homeState = homeState,
                        selectedIds = selectedIds,
                        selecting = selecting,
                        onAddRecent = ::addRecent,
                        onArtistClick = { artist ->
                            addRecent(query)
                            val song = artist.songs.firstOrNull()
                            if (song != null) {
                                homeViewModel.recordPlayedFromLibrary(song, playerState.currentSongId, "Playing by " + artist.name)
                                playerViewModel.playFromSource(song, artist.songs, "Playing by " + artist.name)
                            }
                        },
                        onArtistLongClick = ::showSearchArtistOptions,
                        onPlaylistClick = { playlist ->
                            addRecent(query)
                            onPlaylistSelected?.invoke(playlist.id)
                        },
                        onPlaylistLongClick = { pl, songs -> showSearchPlaylistOptions(pl, songs) },
                        homeViewModel = homeViewModel,
                        playerViewModel = playerViewModel,
                        overlays = overlays,
                        context = context,
                        deleteLauncher = deleteLauncher,
                        onSongSelect = { id ->
                            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                        },
                        onPendingDelete = { pendingDelete = listOf(it) }
                    )
                }
            }
        } else {
            // Portrait Layout
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = if (selecting) 8.dp else topInset + 4.dp,
                    bottom = bottomInset
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "search_field") {
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        SearchInputField()
                    }
                }

                if (query.isBlank()) {
                    if (recentSearches.isNotEmpty()) {
                        item(key = "recent_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Recent Searches", color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Clear all",
                                    color = colors.mutedText,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { scope.launch { prefs.clearRecentSearches() } }
                                )
                            }
                        }

                        items(recentSearches, key = { "recent_$it" }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.card)
                                    .clickable { query = item; addRecent(item) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = item, color = colors.primaryText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Icon(
                                    painter = painterResource(R.drawable.ic_xvox_close),
                                    contentDescription = "Remove",
                                    tint = colors.mutedText,
                                    modifier = Modifier.size(16.dp).clickable { scope.launch { prefs.removeRecentSearch(item) } }
                                )
                            }
                        }
                    }
                } else {
                    renderSearchResults(
                        query = query,
                        colors = colors,
                        filteredArtists = filteredArtists,
                        filteredPlaylists = filteredPlaylists,
                        filteredSongs = filteredSongs,
                        playerState = playerState,
                        homeState = homeState,
                        selectedIds = selectedIds,
                        selecting = selecting,
                        onAddRecent = ::addRecent,
                        onArtistClick = { artist ->
                            addRecent(query)
                            val song = artist.songs.firstOrNull()
                            if (song != null) {
                                homeViewModel.recordPlayedFromLibrary(song, playerState.currentSongId, "Playing by " + artist.name)
                                playerViewModel.playFromSource(song, artist.songs, "Playing by " + artist.name)
                            }
                        },
                        onArtistLongClick = ::showSearchArtistOptions,
                        onPlaylistClick = { playlist ->
                            addRecent(query)
                            onPlaylistSelected?.invoke(playlist.id)
                        },
                        onPlaylistLongClick = { pl, songs -> showSearchPlaylistOptions(pl, songs) },
                        homeViewModel = homeViewModel,
                        playerViewModel = playerViewModel,
                        overlays = overlays,
                        context = context,
                        deleteLauncher = deleteLauncher,
                        onSongSelect = { id ->
                            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                        },
                        onPendingDelete = { pendingDelete = listOf(it) }
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderSearchResults(
    query: String,
    colors: com.xvox.music.core.design.theme.XvoxPalette,
    filteredArtists: List<com.xvox.music.features.artist.XvoxArtist>,
    filteredPlaylists: List<XvoxPlaylist>,
    filteredSongs: List<Song>,
    playerState: com.xvox.music.player.playback.MainPlayerUiState,
    homeState: com.xvox.music.features.home.HomeUiState,
    selectedIds: Set<Long>,
    selecting: Boolean,
    onAddRecent: (String) -> Unit,
    onArtistClick: (com.xvox.music.features.artist.XvoxArtist) -> Unit,
    onArtistLongClick: (com.xvox.music.features.artist.XvoxArtist) -> Unit,
    onPlaylistClick: (XvoxPlaylist) -> Unit,
    onPlaylistLongClick: (XvoxPlaylist, List<Song>) -> Unit,
    homeViewModel: HomeViewModel,
    playerViewModel: MainPlayerViewModel,
    overlays: com.xvox.music.core.ui.overlay.XvoxOverlayController,
    context: android.content.Context,
    deleteLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    onSongSelect: (Long) -> Unit,
    onPendingDelete: (Song) -> Unit
) {
    if (filteredArtists.isNotEmpty()) {
        item(key = "artists_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Artists (${filteredArtists.size})", color = colors.primaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        item(key = "search_artists_row") {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 8.dp
                val artistWidth = ((maxWidth - gap * 4 - 24.dp) / 5).coerceAtLeast(68.dp)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    items(filteredArtists, key = { "search_artist_${it.name}" }) { artist ->
                        ArtistSquareItem(
                            artist = artist,
                            onClick = { onArtistClick(artist) },
                            onLongClick = { onArtistLongClick(artist) },
                            modifier = Modifier.width(artistWidth)
                        )
                    }
                }
            }
        }
    }

    if (filteredPlaylists.isNotEmpty()) {
        item(key = "playlists_header") {
            Text("Playlists (${filteredPlaylists.size})", color = colors.primaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp))
        }

        item(key = "search_playlists_row") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPlaylists, key = { "search_pl_${it.id}" }) { playlist ->
                    val coverSongs = homeViewModel.playlistSongs(playlist)
                    XvoxPlaylistCard(
                        playlist = playlist,
                        songs = coverSongs,
                        onClick = { onPlaylistClick(playlist) },
                        onLongClick = { onPlaylistLongClick(playlist, coverSongs) },
                        modifier = Modifier.width(200.dp).height(130.dp),
                        longCard = true
                    )
                }
            }
        }
    }

    if (filteredSongs.isNotEmpty()) {
        item(key = "songs_header") {
            Text("Songs (${filteredSongs.size})", color = colors.primaryAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp))
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
                    if (selecting) onSongSelect(song.id)
                    else {
                        onAddRecent(query)
                        homeViewModel.recordPlayedFromLibrary(song, playerState.currentSongId, "Search")
                        val songQueue = if (homeState.songs.isNotEmpty()) homeState.songs else filteredSongs
                        playerViewModel.playFromSource(song, songQueue, "Search")
                    }
                },
                onOptions = {
                    showSongOptionsOverlay(
                        overlays = overlays,
                        context = context,
                        song = song.copy(source = "Search"),
                        isLiked = song.id in homeState.likedSongIds,
                        playlist = null,
                        recent = false,
                        viewModel = homeViewModel,
                        playerViewModel = playerViewModel,
                        playlists = homeState.playlists,
                        songs = homeState.songs,
                        deleteLauncher = deleteLauncher,
                        onPendingDelete = onPendingDelete,
                        onSelect = { onSongSelect(song.id) }
                    )
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
            )
        }
    }

    if (filteredArtists.isEmpty() && filteredPlaylists.isEmpty() && filteredSongs.isEmpty()) {
        item(key = "no_results") {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Text(text = "No songs, artists, or playlists found", color = colors.mutedText, fontSize = 13.sp)
            }
        }
    }
}
