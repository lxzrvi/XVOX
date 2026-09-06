package com.xvox.music.features.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.UserPreferencesRepository
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
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var internalSelectedPlaylistId by remember { mutableStateOf<String?>(null) }
    val effectiveSelectedPlaylistId = if (onSelectedPlaylistIdChange != null) selectedPlaylistId else internalSelectedPlaylistId

    fun setSelectedPlaylistId(value: String?) {
        if (onSelectedPlaylistIdChange != null) onSelectedPlaylistIdChange(value)
        else internalSelectedPlaylistId = value
    }

    var selectedSongIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedSongIds.isNotEmpty()
    var pendingDeleteSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    val selectedPlaylist = effectiveSelectedPlaylistId?.let { id ->
        state.playlists.firstOrNull { it.id == id }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingDeleteSongs.isNotEmpty()) {
            pendingDeleteSongs.forEach { playerViewModel.removeFromQueue(it.id) }
            val count = pendingDeleteSongs.size
            viewModel.refresh()
            overlays.showP("$count ${if (count == 1) "song" else "songs"} deleted from device")
        }
        pendingDeleteSongs = emptyList()
    }

    val prefs = remember { UserPreferencesRepository(context) }
    val hideRecents by prefs.hideRecentlyPlayed.collectAsState(initial = false)

    LaunchedEffect(state.songs) {
        if (state.songs.isNotEmpty()) onQueueReady(state.songs)
    }

    LaunchedEffect(homeResetKey) {
        if (homeResetKey > 0L) {
            selectedSongIds = emptySet()
            setSelectedPlaylistId(null)
            viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
        }
    }

    LaunchedEffect(effectiveSelectedPlaylistId, state.libraryMode) {
        selectedSongIds = emptySet()
        listState.scrollToItem(0)
    }

    BackHandler(enabled = isSelectionMode) { selectedSongIds = emptySet() }
    BackHandler(enabled = !isSelectionMode && selectedPlaylist != null) { setSelectedPlaylistId(null) }
    BackHandler(enabled = !isSelectionMode && selectedPlaylist == null && state.libraryMode != XvoxHomeLibraryMode.ALL_SONGS) {
        viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
    }

    fun openSingleSongOptions(song: Song, playlist: XvoxPlaylist? = null, recent: Boolean = false) {
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
            onPendingDelete = { songToDelete: Song -> pendingDeleteSongs = listOf(songToDelete) }
        )
    }

    fun handleSongClick(song: Song, list: List<Song>, sourceName: String) {
        if (isSelectionMode) {
            selectedSongIds = if (song.id in selectedSongIds) selectedSongIds - song.id else selectedSongIds + song.id
        } else {
            viewModel.recordPlayedFromLibrary(song, currentSongId)
            playerViewModel.playFromSource(song, list, sourceName)
        }
    }

    fun handleSongLongClick(song: Song) {
        selectedSongIds = if (!isSelectionMode) setOf(song.id)
        else if (song.id in selectedSongIds) selectedSongIds - song.id
        else selectedSongIds + song.id
    }

    val selectedSongsList = remember(selectedSongIds, state.songs) {
        state.songs.filter { it.id in selectedSongIds }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp)
        ) {
            item(key = "library") {
                AnimatedContent(
                    targetState = effectiveSelectedPlaylistId ?: state.libraryMode,
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    label = "home_library_mode_switch"
                ) { _ ->
                    if (selectedPlaylist != null) {
                        val plSongs = viewModel.playlistSongs(selectedPlaylist)
                        XvoxPlaylistDetail(
                            playlist = selectedPlaylist,
                            songs = plSongs,
                            currentSongId = currentSongId,
                            isPlaying = isPlaying,
                            selectedSongIds = selectedSongIds,
                            onPlay = { song -> handleSongClick(song, plSongs, selectedPlaylist.name) },
                            onOptions = { song ->
                                if (isSelectionMode) handleSongClick(song, plSongs, selectedPlaylist.name)
                                else openSingleSongOptions(song, selectedPlaylist)
                            },
                            onLongClick = ::handleSongLongClick,
                            onAddSongs = { showAddPlaylistSongs(overlays, viewModel, selectedPlaylist) },
                            onClosed = {
                                selectedSongIds = emptySet()
                                setSelectedPlaylistId(null)
                            }
                        )
                    } else {
                        when (state.libraryMode) {
                            XvoxHomeLibraryMode.ALL_SONGS -> {
                                XvoxAllSongsSection(
                                    songs = state.songs,
                                    currentSongId = currentSongId,
                                    isPlaying = isPlaying,
                                    selectedSongIds = selectedSongIds,
                                    onSongClick = { song -> handleSongClick(song, state.songs, "All Songs") },
                                    onSongLongClick = ::handleSongLongClick,
                                    onPrefetch = viewModel::prefetchFrom
                                )
                            }
                            XvoxHomeLibraryMode.LIKED -> {
                                val likedList = viewModel.likedSongs()
                                XvoxLikedSongsSection(
                                    songs = likedList,
                                    currentSongId = currentSongId,
                                    isPlaying = isPlaying,
                                    selectedSongIds = selectedSongIds,
                                    onPlay = { song -> handleSongClick(song, likedList, "Liked Songs") },
                                    onOptions = { song ->
                                        if (isSelectionMode) handleSongClick(song, likedList, "Liked Songs")
                                        else openSingleSongOptions(song)
                                    },
                                    onLongClick = ::handleSongLongClick
                                )
                            }
                            XvoxHomeLibraryMode.PLAYLISTS -> {
                                XvoxPlaylistsSection(
                                    playlists = state.playlists,
                                    songsFor = viewModel::playlistSongs,
                                    onCreate = { showCreatePlaylistOverlay(overlays, viewModel, state.songs) },
                                    onOpen = { playlist -> setSelectedPlaylistId(playlist.id) },
                                    onOptions = { playlist ->
                                        showPlaylistActions(overlays, viewModel, playlist) {
                                            if (effectiveSelectedPlaylistId == playlist.id) setSelectedPlaylistId(null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (!hideRecents && effectiveSelectedPlaylistId == null && state.libraryMode == XvoxHomeLibraryMode.ALL_SONGS && !isSelectionMode) {
                item(key = "recent") {
                    XvoxRecentlyPlayedSection(
                        songs = state.recentlyPlayed,
                        currentSongId = currentSongId,
                        isPlaying = isPlaying,
                        transition = state.recentTransition,
                        onSongClick = { song ->
                            if (song.id == currentSongId) playerViewModel.togglePlay()
                            else {
                                viewModel.recordPlayedFromRecent(song, currentSongId)
                                playerViewModel.playFromSource(song, state.recentlyPlayed, "Recently Played")
                            }
                        },
                        onSongOptions = { song -> openSingleSongOptions(song = song, recent = true) }
                    )
                }
            }

            item(key = "home_bottom_spacer") { Spacer(Modifier.height(130.dp)) }
        }

        if (isSelectionMode && selectedSongsList.isNotEmpty()) {
            HomeMultiSelectBar(
                selectedSongs = selectedSongsList,
                selectedPlaylist = selectedPlaylist,
                libraryMode = state.libraryMode,
                viewModel = viewModel,
                overlays = overlays,
                context = context,
                onClearSelection = { selectedSongIds = emptySet() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
