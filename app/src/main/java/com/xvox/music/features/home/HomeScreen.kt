package com.xvox.music.features.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.allsongs.XvoxAllSongsSection
import com.xvox.music.features.home.recent.XvoxRecentlyPlayedSection
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.features.playlist.XvoxLikedSongsSection
import com.xvox.music.features.playlist.XvoxPlaylistDetail
import com.xvox.music.features.playlist.XvoxPlaylistsSection
import com.xvox.music.player.playback.MainPlayerViewModel

@Composable
fun HomeScreen(
    currentSongId: Long?,
    isPlaying: Boolean,
    homeResetKey: Long = 0L,
    selectedPlaylistId: String? = null,
    onSelectedPlaylistIdChange: ((String?) -> Unit)? = null,
    onQueueReady: (List<Song>) -> Unit,
    onPlay: (Song) -> Unit,
    playerViewModel: MainPlayerViewModel = viewModel(),
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = XvoxTheme.colors
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current

    var internalSelectedPlaylistId by remember { mutableStateOf<String?>(null) }
    val effectiveSelectedPlaylistId = if (onSelectedPlaylistIdChange != null) selectedPlaylistId else internalSelectedPlaylistId

    fun setSelectedPlaylistId(value: String?) {
        if (onSelectedPlaylistIdChange != null) {
            onSelectedPlaylistIdChange(value)
        } else {
            internalSelectedPlaylistId = value
        }
    }

    var pendingDelete by remember { mutableStateOf<Song?>(null) }

    val selectedPlaylist = effectiveSelectedPlaylistId?.let { id ->
        state.playlists.firstOrNull { it.id == id }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val song = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && song != null) {
            playerViewModel.removeFromQueue(song.id)
            viewModel.refresh()
            overlays.showP("Deleted from device")
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    LaunchedEffect(state.songs) {
        if (state.songs.isNotEmpty()) {
            onQueueReady(state.songs)
        }
    }

    LaunchedEffect(homeResetKey) {
        if (homeResetKey > 0L) {
            setSelectedPlaylistId(null)
            viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
        }
    }

    BackHandler(enabled = selectedPlaylist != null) {
        setSelectedPlaylistId(null)
    }

    BackHandler(enabled = selectedPlaylist == null && state.libraryMode != XvoxHomeLibraryMode.ALL_SONGS) {
        viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
    }

    fun openSongOptions(song: Song, playlist: XvoxPlaylist? = null, recent: Boolean = false) {
        showSongOptionsOverlay(
            overlays = overlays,
            context = context,
            song = song,
            isLiked = song.id in state.likedSongIds,
            playlist = playlist,
            recent = recent,
            viewModel = viewModel,
            playerViewModel = playerViewModel,
            playlists = state.playlists,
            songs = state.songs,
            deleteLauncher = deleteLauncher,
            onPendingDelete = { pendingDelete = it }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp)
        ) {
            item(key = "library") {
                AnimatedContent(
                    targetState = effectiveSelectedPlaylistId ?: state.libraryMode,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                            .togetherWith(fadeOut(animationSpec = tween(160)))
                    },
                    label = "home_library_mode_switch"
                ) { _ ->
                    if (selectedPlaylist != null) {
                        XvoxPlaylistDetail(
                            playlist = selectedPlaylist,
                            songs = viewModel.playlistSongs(selectedPlaylist),
                            currentSongId = currentSongId,
                            isPlaying = isPlaying,
                            onPlay = { song ->
                                viewModel.recordPlayedFromLibrary(song, currentSongId)
                                playerViewModel.playFromSource(song, viewModel.playlistSongs(selectedPlaylist), selectedPlaylist.name)
                            },
                            onOptions = { song -> openSongOptions(song, selectedPlaylist) },
                            onAddSongs = {
                                showAddPlaylistSongs(
                                    overlays = overlays,
                                    viewModel = viewModel,
                                    playlist = selectedPlaylist
                                )
                            },
                            onClosed = { setSelectedPlaylistId(null) }
                        )
                    } else {
                        when (state.libraryMode) {
                            XvoxHomeLibraryMode.ALL_SONGS -> {
                                XvoxAllSongsSection(
                                    songs = state.songs,
                                    currentSongId = currentSongId,
                                    isPlaying = isPlaying,
                                    onSongClick = { song ->
                                        viewModel.recordPlayedFromLibrary(song, currentSongId)
                                        playerViewModel.playFromSource(song, state.songs, "All Songs")
                                    },
                                    onSongLongClick = { song -> openSongOptions(song) },
                                    onPrefetch = viewModel::prefetchFrom
                                )
                            }
                            XvoxHomeLibraryMode.LIKED -> {
                                XvoxLikedSongsSection(
                                    songs = viewModel.likedSongs(),
                                    currentSongId = currentSongId,
                                    isPlaying = isPlaying,
                                    onPlay = { song ->
                                        viewModel.recordPlayedFromLibrary(song, currentSongId)
                                        playerViewModel.playFromSource(song, viewModel.likedSongs(), "Liked Songs")
                                    },
                                    onOptions = { song -> openSongOptions(song) }
                                )
                            }
                            XvoxHomeLibraryMode.PLAYLISTS -> {
                                XvoxPlaylistsSection(
                                    playlists = state.playlists,
                                    songsFor = viewModel::playlistSongs,
                                    onCreate = { showCreatePlaylistOverlay(overlays, viewModel, state.songs) },
                                    onOpen = { playlist -> setSelectedPlaylistId(playlist.id) },
                                    onOptions = { playlist ->
                                        showPlaylistActions(
                                            overlays = overlays,
                                            viewModel = viewModel,
                                            playlist = playlist,
                                            onDeleted = {
                                                if (effectiveSelectedPlaylistId == playlist.id) {
                                                    setSelectedPlaylistId(null)
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (effectiveSelectedPlaylistId == null && state.libraryMode == XvoxHomeLibraryMode.ALL_SONGS) {
                item(key = "recent") {
                    XvoxRecentlyPlayedSection(
                        songs = state.recentlyPlayed,
                        currentSongId = currentSongId,
                        isPlaying = isPlaying,
                        transition = state.recentTransition,
                        onSongClick = { song ->
                            viewModel.recordPlayedFromRecent(song, currentSongId)
                            playerViewModel.playFromSource(song, state.recentlyPlayed, "Recently Played")
                        },
                        onSongOptions = { song -> openSongOptions(song = song, recent = true) }
                    )
                }
            }

            item(key = "footer") {
                HomeFooter(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .heightIn(min = screenHeight)
                )
            }
        }
    }
}
