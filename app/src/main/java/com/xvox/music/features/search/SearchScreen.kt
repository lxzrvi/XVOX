package com.xvox.music.features.search

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.features.artist.ArtistSquareItem
import com.xvox.music.features.artist.XvoxArtist
import com.xvox.music.features.home.HomePlaylistOverlays
import com.xvox.music.features.home.HomeScreenOverlays
import com.xvox.music.features.home.HomeViewModel
import com.xvox.music.features.home.XvoxPlaylist
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor
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
    val overlays = LocalXvoxOverlayController.current
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val bottomInset = LocalXvoxBottomInset.current

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

    val trimmedQuery = query.trim()
    val matchingSongs = remember(trimmedQuery, homeState.songs) {
        if (trimmedQuery.isEmpty()) emptyList()
        else {
            homeState.songs.filter { song ->
                song.title.contains(trimmedQuery, ignoreCase = true) ||
                    song.artist.contains(trimmedQuery, ignoreCase = true) ||
                    song.album.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val matchingArtists = remember(trimmedQuery, homeState.artists) {
        if (trimmedQuery.isEmpty()) emptyList()
        else {
            homeState.artists.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
        }
    }

    val matchingPlaylists = remember(trimmedQuery, homeState.playlists) {
        if (trimmedQuery.isEmpty()) emptyList()
        else {
            homeState.playlists.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
        }
    }

    if (isLandscape) {
        // Landscape 2-pane layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left pane: Search bar + quick hints/history
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxSize()
            ) {
                SearchBarComponent(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    focusRequester = focusRequester,
                    onSearch = { focusManager.clearFocus() }
                )

                Spacer(Modifier.height(16.dp))

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
                    .weight(0.58f)
                    .fillMaxSize()
            ) {
                SearchResultsList(
                    query = trimmedQuery,
                    matchingSongs = matchingSongs,
                    matchingArtists = matchingArtists,
                    matchingPlaylists = matchingPlaylists,
                    currentSongId = playerState.currentSongId,
                    isPlaying = playerState.isPlaying,
                    bottomInset = bottomInset,
                    onSongClick = { song ->
                        playerViewModel.playSongFromQueue(song, matchingSongs.ifEmpty { listOf(song) })
                    },
                    onSongLongClick = { song ->
                        HomeScreenOverlays.showSongOptionsOverlay(
                            overlays = overlays,
                            context = context,
                            song = song,
                            isLiked = song.id in homeState.likedSongIds,
                            viewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            playlists = homeState.playlists,
                            songs = homeState.songs,
                            deleteLauncher = deleteLauncher,
                            onPendingDelete = { pendingDeleteSong = it }
                        )
                    },
                    onArtistClick = { artist ->
                        val artistSongs = homeState.songs.filter { it.artist.equals(artist.name, ignoreCase = true) }
                        if (artistSongs.isNotEmpty()) {
                            playerViewModel.setQueue(artistSongs)
                            playerViewModel.play(artistSongs.first())
                        }
                    },
                    onPlaylistClick = { playlist ->
                        onPlaylistSelected?.invoke(playlist.id)
                    },
                    onPlaylistLongClick = { playlist ->
                        HomePlaylistOverlays.showPlaylistActions(
                            overlays = overlays,
                            viewModel = homeViewModel,
                            playlist = playlist,
                            onDeleted = {}
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
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                SearchBarComponent(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    focusRequester = focusRequester,
                    onSearch = { focusManager.clearFocus() }
                )
            }

            SearchResultsList(
                query = trimmedQuery,
                matchingSongs = matchingSongs,
                matchingArtists = matchingArtists,
                matchingPlaylists = matchingPlaylists,
                currentSongId = playerState.currentSongId,
                isPlaying = playerState.isPlaying,
                bottomInset = bottomInset,
                onSongClick = { song ->
                    playerViewModel.playSongFromQueue(song, matchingSongs.ifEmpty { listOf(song) })
                },
                onSongLongClick = { song ->
                    HomeScreenOverlays.showSongOptionsOverlay(
                        overlays = overlays,
                        context = context,
                        song = song,
                        isLiked = song.id in homeState.likedSongIds,
                        viewModel = homeViewModel,
                        playerViewModel = playerViewModel,
                        playlists = homeState.playlists,
                        songs = homeState.songs,
                        deleteLauncher = deleteLauncher,
                        onPendingDelete = { pendingDeleteSong = it }
                    )
                },
                onArtistClick = { artist ->
                    val artistSongs = homeState.songs.filter { it.artist.equals(artist.name, ignoreCase = true) }
                    if (artistSongs.isNotEmpty()) {
                        playerViewModel.setQueue(artistSongs)
                        playerViewModel.play(artistSongs.first())
                    }
                },
                onPlaylistClick = { playlist ->
                    onPlaylistSelected?.invoke(playlist.id)
                },
                onPlaylistLongClick = { playlist ->
                    HomePlaylistOverlays.showPlaylistActions(
                        overlays = overlays,
                        viewModel = homeViewModel,
                        playlist = playlist,
                        onDeleted = {}
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
    onSearch: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.card)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_xvox_search),
            contentDescription = null,
            tint = colors.secondaryText,
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
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(colors.primaryAccent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search songs, artists, playlists...",
                        color = colors.secondaryText,
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
private fun SearchResultsList(
    query: String,
    matchingSongs: List<Song>,
    matchingArtists: List<XvoxArtist>,
    matchingPlaylists: List<XvoxPlaylist>,
    currentSongId: Long?,
    isPlaying: Boolean,
    bottomInset: androidx.compose.ui.unit.Dp,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onArtistClick: (XvoxArtist) -> Unit,
    onPlaylistClick: (XvoxPlaylist) -> Unit,
    onPlaylistLongClick: (XvoxPlaylist) -> Unit
) {
    val colors = XvoxTheme.colors

    if (query.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Type to search your music",
                color = colors.secondaryText,
                fontSize = 14.sp
            )
        }
        return
    }

    if (matchingSongs.isEmpty() && matchingArtists.isEmpty() && matchingPlaylists.isEmpty()) {
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
        if (matchingPlaylists.isNotEmpty()) {
            item {
                Text(
                    text = "Playlists (${matchingPlaylists.size})",
                    color = colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            items(matchingPlaylists, key = { "pl_${it.id}" }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale(
                            onClick = { onPlaylistClick(playlist) },
                            onLongClick = { onPlaylistLongClick(playlist) }
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_playlist),
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            color = colors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlist.songIds.size} songs",
                            color = colors.secondaryText,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        if (matchingArtists.isNotEmpty()) {
            item {
                Text(
                    text = "Artists (${matchingArtists.size})",
                    color = colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            items(matchingArtists, key = { "art_${it.name}" }) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .xvoxPressScale(onClick = { onArtistClick(artist) })
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_artist),
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = artist.name,
                        color = colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (matchingSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Songs (${matchingSongs.size})",
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
                            song = song,
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
