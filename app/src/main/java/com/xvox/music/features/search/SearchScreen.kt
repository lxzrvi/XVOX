package com.xvox.music.features.search

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.artist.XvoxArtist
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
import com.xvox.music.features.home.showPlaylistActions
import com.xvox.music.features.home.showSongOptionsOverlay
import com.xvox.music.player.playback.MainPlayerViewModel

@Composable
fun SearchScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: MainPlayerViewModel = viewModel(),
    topResetKey: Long = 0L,
    onPlaylistSelected: ((String) -> Unit)? = null
) {
    val colors = XvoxTheme.colors
    val homeState by homeViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    val recentSearches by homeViewModel.recentSearches.collectAsState()
    val overlays = LocalXvoxOverlayController.current
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val bottomInset = LocalXvoxBottomInset.current
    val topInset = LocalXvoxTopInset.current

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    var pendingDeleteSong by remember { mutableStateOf<Song?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingDeleteSong != null) {
            playerViewModel.removeFromQueue(pendingDeleteSong!!.id)
            homeViewModel.refresh()
            overlays.showP("Song deleted from device")
        }
        pendingDeleteSong = null
    }

    LaunchedEffect(topResetKey) {
        if (topResetKey > 0) {
            listState.scrollToItem(0)
        }
    }

    val artists = remember(homeState.songs, homeState.customArtistImages, homeState.hiddenArtists, homeState.artistRenames) {
        val songsWithRenames = homeState.songs.map { song ->
            val renamed = homeState.artistRenames[song.artist]
            if (renamed != null) song.copy(artist = renamed) else song
        }
        songsWithRenames
            .filterNot { it.artist in homeState.hiddenArtists }
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { (artistName, songList) ->
                val cover = songList.firstOrNull { it.artworkUri != null } ?: songList.firstOrNull()
                XvoxArtist(
                    name = artistName,
                    songs = songList,
                    coverSong = cover,
                    customImageUri = homeState.customArtistImages[artistName]
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val trimmedQuery = query.trim()
    val matchingSongs = remember(trimmedQuery, homeState.songs) {
        if (trimmedQuery.isEmpty()) homeState.songs
        else {
            homeState.songs.filter { song ->
                song.title.contains(trimmedQuery, ignoreCase = true) ||
                    song.artist.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingArtists = remember(trimmedQuery, artists) {
        if (trimmedQuery.isEmpty()) artists
        else {
            artists.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
        }
    }

    val matchingPlaylists = remember(trimmedQuery, homeState.playlists) {
        if (trimmedQuery.isEmpty()) homeState.playlists
        else {
            homeState.playlists.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
        }
    }

    fun handleSongClick(song: Song) {
        if (trimmedQuery.isNotBlank()) {
            homeViewModel.addRecentSearch(trimmedQuery)
        }
        val targetList = if (trimmedQuery.isEmpty()) homeState.songs else matchingSongs
        playerViewModel.playFromSource(
            song = song,
            sourceQueue = targetList,
            source = if (trimmedQuery.isEmpty()) "Search: All Songs" else "Search: \"$trimmedQuery\""
        )
    }

    fun handlePlaylistClick(playlist: XvoxPlaylist) {
        if (trimmedQuery.isNotBlank()) {
            homeViewModel.addRecentSearch(trimmedQuery)
        }
        val songs = homeViewModel.playlistSongs(playlist)
        if (songs.isNotEmpty()) {
            playerViewModel.playFromSource(
                song = songs.first(),
                sourceQueue = songs,
                source = "Playlist: ${playlist.name}"
            )
        }
        onPlaylistSelected?.invoke(playlist.id)
    }

    fun handleArtistClick(artist: XvoxArtist) {
        if (trimmedQuery.isNotBlank()) {
            homeViewModel.addRecentSearch(trimmedQuery)
        }
        if (artist.songs.isNotEmpty()) {
            playerViewModel.playFromSource(
                song = artist.songs.first(),
                sourceQueue = artist.songs,
                source = "Artist: ${artist.name}"
            )
        }
    }

    if (isLandscape) {
        // Landscape 2-pane layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset + 20.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left pane: Search bar + Recent Searches + Library summary
            Column(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxSize()
            ) {
                SearchBarComponent(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    focusRequester = focusRequester,
                    onSearch = {
                        if (trimmedQuery.isNotBlank()) homeViewModel.addRecentSearch(trimmedQuery)
                        focusManager.clearFocus()
                    }
                )

                Spacer(Modifier.height(10.dp))

                RecentSearchesSection(
                    searches = recentSearches,
                    onSelect = { q ->
                        query = q
                        focusManager.clearFocus()
                    },
                    onRemove = { q -> homeViewModel.removeRecentSearch(q) },
                    onClearAll = { homeViewModel.clearRecentSearches() }
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.card.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Search Library",
                            color = colors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Search songs, artists, and playlists instantly across your local collection.",
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        if (trimmedQuery.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Found ${matchingSongs.size} songs, ${matchingArtists.size} artists, ${matchingPlaylists.size} playlists",
                                color = colors.primaryAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Right pane: Search results
            Box(
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxSize()
            ) {
                SearchResultsList(
                    query = trimmedQuery,
                    matchingSongs = matchingSongs,
                    matchingArtists = matchingArtists,
                    matchingPlaylists = matchingPlaylists,
                    allSongs = homeState.songs,
                    currentSongId = playerState.currentSongId,
                    isPlaying = playerState.isPlaying,
                    bottomInset = bottomInset,
                    onSongClick = ::handleSongClick,
                    onSongLongClick = { song ->
                        showSongOptionsOverlay(
                            overlays = overlays,
                            context = context,
                            song = song,
                            isLiked = song.id in homeState.likedSongIds,
                            playlist = null,
                            viewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            playlists = homeState.playlists,
                            songs = homeState.songs,
                            deleteLauncher = deleteLauncher,
                            onPendingDelete = { pendingDeleteSong = it }
                        )
                    },
                    onArtistClick = ::handleArtistClick,
                    onArtistLongClick = { artist ->
                        val firstSong = artist.songs.firstOrNull() ?: return@SearchResultsList
                        showSongOptionsOverlay(
                            overlays = overlays,
                            context = context,
                            song = firstSong,
                            isLiked = firstSong.id in homeState.likedSongIds,
                            playlist = null,
                            viewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            playlists = homeState.playlists,
                            songs = homeState.songs,
                            deleteLauncher = deleteLauncher,
                            onPendingDelete = { pendingDeleteSong = it }
                        )
                    },
                    onPlaylistClick = ::handlePlaylistClick,
                    onPlaylistLongClick = { playlist ->
                        showPlaylistActions(
                            overlays = overlays,
                            viewModel = homeViewModel,
                            playlist = playlist,
                            onSelect = { handlePlaylistClick(playlist) },
                            onDeleted = { homeViewModel.refresh() }
                        )
                    }
                )
            }
        }
    } else {
        // Portrait Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset + 20.dp)
        ) {
            SearchBarComponent(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" },
                focusRequester = focusRequester,
                onSearch = {
                    if (trimmedQuery.isNotBlank()) homeViewModel.addRecentSearch(trimmedQuery)
                    focusManager.clearFocus()
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )

            RecentSearchesSection(
                searches = recentSearches,
                onSelect = { q ->
                    query = q
                    focusManager.clearFocus()
                },
                onRemove = { q -> homeViewModel.removeRecentSearch(q) },
                onClearAll = { homeViewModel.clearRecentSearches() }
            )

            SearchResultsList(
                query = trimmedQuery,
                matchingSongs = matchingSongs,
                matchingArtists = matchingArtists,
                matchingPlaylists = matchingPlaylists,
                allSongs = homeState.songs,
                currentSongId = playerState.currentSongId,
                isPlaying = playerState.isPlaying,
                bottomInset = bottomInset,
                onSongClick = ::handleSongClick,
                onSongLongClick = { song ->
                    showSongOptionsOverlay(
                        overlays = overlays,
                        context = context,
                        song = song,
                        isLiked = song.id in homeState.likedSongIds,
                        playlist = null,
                        viewModel = homeViewModel,
                        playerViewModel = playerViewModel,
                        playlists = homeState.playlists,
                        songs = homeState.songs,
                        deleteLauncher = deleteLauncher,
                        onPendingDelete = { pendingDeleteSong = it }
                    )
                },
                onArtistClick = ::handleArtistClick,
                onArtistLongClick = { artist ->
                    val firstSong = artist.songs.firstOrNull() ?: return@SearchResultsList
                    showSongOptionsOverlay(
                        overlays = overlays,
                        context = context,
                        song = firstSong,
                        isLiked = firstSong.id in homeState.likedSongIds,
                        playlist = null,
                        viewModel = homeViewModel,
                        playerViewModel = playerViewModel,
                        playlists = homeState.playlists,
                        songs = homeState.songs,
                        deleteLauncher = deleteLauncher,
                        onPendingDelete = { pendingDeleteSong = it }
                    )
                },
                onPlaylistClick = ::handlePlaylistClick,
                onPlaylistLongClick = { playlist ->
                    showPlaylistActions(
                        overlays = overlays,
                        viewModel = homeViewModel,
                        playlist = playlist,
                        onSelect = { handlePlaylistClick(playlist) },
                        onDeleted = { homeViewModel.refresh() }
                    )
                }
            )
        }
    }
}

@Composable
private fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.card)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_xvox_search),
            contentDescription = "Search",
            tint = colors.primaryAccent,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(10.dp))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(
                color = colors.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(colors.primaryAccent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search songs, artists, playlists...",
                        color = colors.mutedText,
                        fontSize = 14.sp
                    )
                }
                innerTextField()
            }
        )

        AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_close),
                contentDescription = "Clear",
                tint = colors.secondaryText,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptics.tap()
                            onClear()
                        }
                    )
            )
        }
    }
}

@Composable
private fun RecentSearchesSection(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    if (searches.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Searches",
                color = colors.primaryAccent,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Clear all",
                color = colors.secondaryText,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        haptics.tap()
                        onClearAll()
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        searches.take(4).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .border(0.8.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        haptics.tap()
                        onSelect(item)
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item,
                    color = colors.primaryText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(R.drawable.ic_xvox_close),
                    contentDescription = "Remove search",
                    tint = colors.secondaryText,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptics.tap()
                            onRemove(item)
                        }
                        .padding(2.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    query: String,
    matchingSongs: List<Song>,
    matchingArtists: List<XvoxArtist>,
    matchingPlaylists: List<XvoxPlaylist>,
    allSongs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    bottomInset: androidx.compose.ui.unit.Dp,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onArtistClick: (XvoxArtist) -> Unit,
    onArtistLongClick: (XvoxArtist) -> Unit,
    onPlaylistClick: (XvoxPlaylist) -> Unit,
    onPlaylistLongClick: (XvoxPlaylist) -> Unit
) {
    val colors = XvoxTheme.colors

    if (query.isNotEmpty() && matchingSongs.isEmpty() && matchingArtists.isEmpty() && matchingPlaylists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No results found for \"$query\"",
                color = colors.secondaryText,
                fontSize = 14.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = bottomInset + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Horizontal Artists Row with Circular Profile
        if (matchingArtists.isNotEmpty()) {
            item(key = "header_artists") {
                Text(
                    text = if (query.isEmpty()) "Artists" else "Artists (${matchingArtists.size})",
                    color = colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                )
            }
            item(key = "row_artists") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    items(matchingArtists, key = { "art_${it.name}" }) { artist ->
                        SearchArtistCircleItem(
                            artist = artist,
                            onClick = { onArtistClick(artist) },
                            onLongClick = { onArtistLongClick(artist) }
                        )
                    }
                }
            }
        }

        // Playlists matching tab style with 2 columns
        if (matchingPlaylists.isNotEmpty()) {
            item(key = "header_playlists") {
                Text(
                    text = if (query.isEmpty()) "Playlists" else "Playlists (${matchingPlaylists.size})",
                    color = colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            items(matchingPlaylists.chunked(2), key = { chunk -> "pl_chunk_${chunk.first().id}" }) { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    chunk.forEach { playlist ->
                        val firstSong = playlist.songIds.firstOrNull()?.let { sid -> allSongs.firstOrNull { it.id == sid } }
                        Box(modifier = Modifier.weight(1f)) {
                            SearchPlaylistCoverItem(
                                playlist = playlist,
                                coverSong = firstSong,
                                onClick = { onPlaylistClick(playlist) },
                                onLongClick = { onPlaylistLongClick(playlist) }
                            )
                        }
                    }
                    repeat(2 - chunk.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Songs list
        if (matchingSongs.isNotEmpty()) {
            item(key = "header_songs") {
                Text(
                    text = if (query.isEmpty()) "All Songs (${matchingSongs.size})" else "Songs (${matchingSongs.size})",
                    color = colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            items(matchingSongs, key = { "song_${it.id}" }) { song ->
                val isCurrent = song.id == currentSongId
                val cardColor = rememberSongCardColor(song = song, current = isCurrent)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardColor)
                        .xvoxSongPress(
                            onClick = { onSongClick(song) },
                            onLongClick = { onSongLongClick(song) }
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        XvoxSongArtwork(
                            artwork = song.artworkUri,
                            requestSize = 100,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = if (isCurrent) colors.primaryAccent else colors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = colors.secondaryText,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isCurrent && isPlaying) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_waveform),
                            contentDescription = "Playing",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchArtistCircleItem(
    artist: XvoxArtist,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    Column(
        modifier = Modifier
            .width(72.dp)
            .xvoxSongPress(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.cardElevated)
                .border(1.dp, colors.cardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!artist.customImageUri.isNullOrBlank()) {
                AsyncImage(
                    model = artist.customImageUri,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (artist.coverSong?.artworkUri != null) {
                XvoxSongArtwork(
                    artwork = artist.coverSong.artworkUri,
                    requestSize = 128,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_artist),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = artist.name,
            color = colors.primaryText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SearchPlaylistCoverItem(
    playlist: XvoxPlaylist,
    coverSong: Song?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(0.8.dp, colors.cardBorder, RoundedCornerShape(12.dp))
            .xvoxSongPress(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.customCoverUri.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.customCoverUri,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (coverSong?.artworkUri != null) {
                XvoxSongArtwork(
                    artwork = coverSong.artworkUri,
                    requestSize = 256,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_playlist),
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = playlist.name,
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${playlist.songIds.size} songs",
            color = colors.secondaryText,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
