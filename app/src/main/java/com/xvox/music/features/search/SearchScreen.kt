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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.features.artist.ArtistSquareItem
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.HomeMultiSelectBar
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongActions
import com.xvox.music.features.home.XvoxSongArtwork
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
                    Text("Play ${artist.name}", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            overlays.hideBox()
                            playerViewModel.playNextInQueue(artist.songs)
                            overlays.showP("Playing after current song")
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Play next (After current song)", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Inserts at front of queue without clearing", color = colors.mutedText, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            overlays.hideBox()
                            playerViewModel.addToQueue(artist.songs)
                            overlays.showP("Added to queue")
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Play next (After queue ends)", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Appends artist tracks to end of queue", color = colors.mutedText, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            overlays.hideBox()
                            homeViewModel.addHiddenSearchArtist(artist.name)
                            overlays.showP("${artist.name} hidden from search")
                        }
                        .padding(14.dp)
                ) {
                    Text("Never show in search", color = colors.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    fun showSearchPlaylistOptions(playlist: com.xvox.music.data.preferences.XvoxPlaylist, coverSongs: List<Song>) {
        overlays.showBox(playlist.name) {
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
                            val song = coverSongs.firstOrNull()
                            if (song != null) {
                                homeViewModel.recordPlayedFromLibrary(song, playerState.currentSongId, playlist.name)
                                playerViewModel.playFromSource(song, coverSongs, playlist.name)
                            }
                        }
                        .padding(14.dp)
                ) {
                    Text("Play ${playlist.name}", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            overlays.hideBox()
                            playerViewModel.playNextInQueue(coverSongs)
                            overlays.showP("Playing after current song")
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Play next (After current song)", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Inserts at front of queue without clearing", color = colors.mutedText, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            overlays.hideBox()
                            playerViewModel.addToQueue(coverSongs)
                            overlays.showP("Added to queue")
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Play next (After queue ends)", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Appends playlist tracks to end of queue", color = colors.mutedText, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale {
                            overlays.hideBox()
                            homeViewModel.addHiddenSearchPlaylist(playlist.id)
                            overlays.showP("${playlist.name} hidden from search")
                        }
                        .padding(14.dp)
                ) {
                    Text("Never show in search", color = colors.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
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

        Spacer(Modifier.height(topInset))

        // Search label matching Home headers exactly
        Text(
            text = "Search",
            color = colors.primaryAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
        )

        // Search Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(colors.card)
                    .border(0.6.dp, colors.cardBorder, RoundedCornerShape(23.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_search),
                    contentDescription = null,
                    tint = if (query.isNotBlank()) colors.primaryAccent else colors.mutedText,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(10.dp))

                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { addRecent(query) }),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search songs, artists, playlists…",
                                color = colors.mutedText,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                            .clickable { query = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = "Clear",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (selecting) {
            HomeMultiSelectBar(
                selectedSongs = selectedSongs,
                selectedPlaylist = null,
                libraryMode = com.xvox.music.features.playlist.XvoxHomeLibraryMode.ALL_SONGS,
                viewModel = homeViewModel,
                overlays = overlays,
                context = context,
                onClearSelection = { selectedIds = emptySet() },
                onDeleteSelected = ::requestDeleteSelected
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + 30.dp)
        ) {
            if (query.isBlank()) {
                if (recentSearches.isNotEmpty()) {
                    item(key = "recents_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                color = colors.primaryAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clear",
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable {
                                    scope.launch { prefs.clearRecentSearches() }
                                }
                            )
                        }
                    }

                    items(recentSearches, key = { "recent_$it" }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = item }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_search),
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = item,
                                color = colors.primaryText,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_close),
                                contentDescription = "Remove",
                                tint = colors.mutedText,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        scope.launch { prefs.removeRecentSearch(item) }
                                    }
                            )
                        }
                    }
                }
            } else {
                if (filteredArtists.isNotEmpty()) {
                    item(key = "artists_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Artists (${filteredArtists.size})",
                                color = colors.primaryAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_caret_right),
                                contentDescription = "Artists",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    item(key = "search_artists_row") {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val gap = 8.dp
                            val artistWidth = ((maxWidth - gap * 4 - 24.dp) / 5).coerceAtLeast(72.dp)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                items(filteredArtists, key = { "search_artist_${it.name}" }) { artist ->
                                    ArtistSquareItem(
                                        artist = artist,
                                        onClick = {
                                            addRecent(query)
                                            val song = artist.songs.firstOrNull()
                                            if (song != null) {
                                                homeViewModel.recordPlayedFromLibrary(song, playerState.currentSongId, "Playing by " + artist.name)
                                                playerViewModel.playFromSource(song, artist.songs, "Playing by " + artist.name)
                                            }
                                        },
                                        onLongClick = { showSearchArtistOptions(artist) },
                                        modifier = Modifier.width(artistWidth)
                                    )
                                }
                            }
                        }
                    }
                    item(key = "search_artist_bottom_spacer") { Spacer(Modifier.height(8.dp)) }
                }

                if (filteredPlaylists.isNotEmpty()) {
                    item(key = "playlists_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Playlists (${filteredPlaylists.size})",
                                color = colors.primaryAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_caret_right),
                                contentDescription = "Playlists",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                                    onClick = {
                                        addRecent(query)
                                        onPlaylistSelected?.invoke(playlist.id)
                                    },
                                    onLongClick = {
                                        showSearchPlaylistOptions(playlist, coverSongs)
                                    },
                                    modifier = Modifier.width(220.dp).height(140.dp),
                                    longCard = true
                                )
                            }
                        }
                    }
                    item(key = "playlist_bottom_spacer") { Spacer(Modifier.height(10.dp)) }
                }

                if (filteredSongs.isNotEmpty()) {
                    item(key = "songs_header") {
                        Text(
                            text = "Songs (${filteredSongs.size})",
                            color = colors.primaryAccent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
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

                if (filteredArtists.isEmpty() && filteredPlaylists.isEmpty() && filteredSongs.isEmpty()) {
                    item(key = "no_results") {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text(text = "No songs, artists, or playlists found", color = colors.mutedText, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
