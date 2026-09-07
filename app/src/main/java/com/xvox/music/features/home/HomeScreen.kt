package com.xvox.music.features.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
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
import com.xvox.music.features.home.allsongs.allSongsItems
import com.xvox.music.features.home.allsongs.buildMosaicPagePlans
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

    var internalSelectedPlaylistId by remember { mutableStateOf<String?>(null) }
    val effectiveSelectedPlaylistId = if (onSelectedPlaylistIdChange != null) selectedPlaylistId else internalSelectedPlaylistId

    fun setSelectedPlaylistId(value: String?) {
        if (onSelectedPlaylistIdChange != null) onSelectedPlaylistIdChange(value)
        else internalSelectedPlaylistId = value
    }

    var selectedSongIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectionLibraryMode by remember { mutableStateOf(state.libraryMode) }
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
    val config by prefs.homePresentation.collectAsState(initial = HomePresentation())
    val plans = remember(state.songs, config.style, config.rows) {
        buildMosaicPagePlans(state.songs, config.rows, config.style == "uniform", config.style == "mosaic1")
    }
    val likedSongs = remember(state.songs, state.likedSongIds) { state.songs.filter { it.id in state.likedSongIds } }
    val songsById = remember(state.songs) { state.songs.associateBy { it.id } }
    val playlistContents = remember(songsById, state.playlists) {
        state.playlists.associate { it.id to it.songIds.mapNotNull(songsById::get) }
    }

    LaunchedEffect(state.songs) {
        selectedSongIds = selectedSongIds.intersect(state.songs.mapTo(HashSet()) { it.id })
        if (!state.loading) onQueueReady(state.songs)
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
    }

    BackHandler(enabled = isSelectionMode) { selectedSongIds = emptySet() }
    BackHandler(enabled = !isSelectionMode && selectedPlaylist != null) { setSelectedPlaylistId(null) }
    BackHandler(enabled = !isSelectionMode && selectedPlaylist == null && state.libraryMode != XvoxHomeLibraryMode.ALL_SONGS) {
        viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
    }

    fun openSingleSongOptions(song: Song, playlist: XvoxPlaylist? = null, recent: Boolean = false, selectionSource: XvoxHomeLibraryMode = state.libraryMode) {
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
            onPendingDelete = { songToDelete: Song -> pendingDeleteSongs = listOf(songToDelete) },
            onSelect = { selectionLibraryMode = selectionSource; selectedSongIds = selectedSongIds + song.id }
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

    fun androidx.compose.foundation.lazy.LazyListScope.recentSection() {
        item(key = "recent") {
            XvoxRecentlyPlayedSection(
                songs = state.recentlyPlayed, currentSongId = currentSongId, isPlaying = isPlaying,
                transition = state.recentTransition,
                onSongClick = { song ->
                    if (isSelectionMode) handleSongLongClick(song)
                    else if (song.id == currentSongId) playerViewModel.togglePlay()
                    else {
                        viewModel.recordPlayedFromRecent(song, currentSongId)
                        playerViewModel.playFromSource(song, state.recentlyPlayed, "Recently Played")
                    }
                },
                onSongOptions = { song -> openSingleSongOptions(song, recent = true) }
            )
        }
    }
    fun androidx.compose.foundation.lazy.LazyListScope.likedSection() {
        librarySongItems("liked", "Liked Songs", likedSongs, currentSongId, isPlaying, selectedSongIds,
            onPlay = { handleSongClick(it, likedSongs, "Liked Songs") },
            onOptions = { if (isSelectionMode) handleSongLongClick(it) else openSingleSongOptions(it, selectionSource = XvoxHomeLibraryMode.LIKED) })
    }
    fun androidx.compose.foundation.lazy.LazyListScope.playlistsSection() {
        playlistCollectionItems(state.playlists, { playlistContents[it.id].orEmpty() },
            onCreate = { showCreatePlaylistOverlay(overlays, viewModel, state.songs) },
            onOpen = { setSelectedPlaylistId(it.id) },
            onOptions = { playlist -> showPlaylistActions(overlays, viewModel, playlist) {
                if (effectiveSelectedPlaylistId == playlist.id) setSelectedPlaylistId(null)
            } })
    }

    val topInset = LocalXvoxTopInset.current
    val bottomInset = LocalXvoxBottomInset.current
    val targetKey = effectiveSelectedPlaylistId ?: state.libraryMode
    Column(Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            Spacer(Modifier.height(topInset))
            HomeMultiSelectBar(selectedSongsList, selectedPlaylist, selectionLibraryMode,
                viewModel, overlays, context, onClearSelection = { selectedSongIds = emptySet() })
        }
        // One complete lazy surface per library destination; only a fade, never a slide or reveal.
        AnimatedContent(targetState = targetKey,
            transitionSpec = { (fadeIn(tween(180)) togetherWith fadeOut(tween(140))).using(null) },
            modifier = Modifier.weight(1f), label = "libraryFade") { target ->
            val listState = rememberLazyListState()
            LaunchedEffect(homeResetKey) { if (homeResetKey > 0L) listState.scrollToItem(0) }
            val targetPlaylist = (target as? String)?.let { id -> state.playlists.firstOrNull { it.id == id } }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = if (isSelectionMode) 4.dp else topInset + 4.dp, bottom = bottomInset)) {
                if (targetPlaylist != null) {
                    val tracks = playlistContents[targetPlaylist.id].orEmpty()
                    librarySongItems("playlist_detail", targetPlaylist.name, tracks, currentSongId, isPlaying, selectedSongIds,
                        onPlay = { handleSongClick(it, tracks, targetPlaylist.name) },
                        onOptions = { if (isSelectionMode) handleSongLongClick(it) else openSingleSongOptions(it, targetPlaylist) },
                        onAdd = { showAddPlaylistSongs(overlays, viewModel, targetPlaylist) })
                } else when (target as? XvoxHomeLibraryMode ?: XvoxHomeLibraryMode.ALL_SONGS) {
                    XvoxHomeLibraryMode.LIKED -> likedSection()
                    XvoxHomeLibraryMode.PLAYLISTS -> playlistsSection()
                    XvoxHomeLibraryMode.ALL_SONGS -> {
                        val sections = HomeSections.visible(config)
                        sections.forEach { section ->
                            when (section) {
                                HomeSections.ALL -> allSongsItems(state.songs, plans, config, currentSongId, isPlaying, selectedSongIds,
                                    onSongClick = { handleSongClick(it, state.songs, "All Songs") },
                                    onSongLongClick = { if (isSelectionMode) handleSongLongClick(it) else openSingleSongOptions(it) },
                                    onPrefetch = viewModel::prefetchFrom)
                                HomeSections.RECENT -> recentSection()
                                HomeSections.LIKED -> likedSection()
                                HomeSections.PLAYLISTS -> playlistsSection()
                            }
                        }
                        if (sections.isEmpty()) item(key = "hidden_home") {
                            androidx.compose.material3.Text("All Home sections are hidden. Show them in Settings → Home → Merge.",
                                color = com.xvox.music.core.design.theme.XvoxTheme.colors.secondaryText,
                                modifier = Modifier.padding(20.dp))
                        }
                    }
                }
            }
        }
    }
}
