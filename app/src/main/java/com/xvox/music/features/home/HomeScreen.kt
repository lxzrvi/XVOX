package com.xvox.music.features.home

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.R
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.components.XvoxImageCropDialog
import com.xvox.music.core.ui.navigation.LocalXvoxBottomInset
import com.xvox.music.core.ui.navigation.LocalXvoxTopInset
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.artist.ArtistInfoDialog
import com.xvox.music.features.artist.XvoxArtist
import com.xvox.music.features.artist.XvoxArtistGrid
import com.xvox.music.features.home.allsongs.XvoxMosaicPagePlan
import com.xvox.music.features.home.allsongs.allSongsItems
import com.xvox.music.features.home.allsongs.buildMosaicPagePlans
import com.xvox.music.features.home.recent.XvoxRecentlyPlayedSection
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.features.sourcemode.XvoxSourceMode
import com.xvox.music.player.playback.MainPlayerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun HomeScreen(
    currentSongId: Long?,
    isPlaying: Boolean,
    homeResetKey: Long = 0L,
    scrollResetKey: Long = 0L,
    selectedPlaylistId: String? = null,
    onSelectedPlaylistIdChange: ((String?) -> Unit)? = null,
    onQueueReady: (List<Song>) -> Unit,
    onPlay: (Song) -> Unit,
    playerViewModel: MainPlayerViewModel = viewModel(),
    viewModel: HomeViewModel = viewModel(),
    /** Reports the active list position to the one Header owned by XvoxMainShell. */
    onScrollProgress: (Int, Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    // Home only needs this one flag to hide its selection rail beneath Now Playing. Collecting the
    // whole fast-progress player state used to recompose every visible All Songs tile on each
    // playback tick, which made a four-column fling feel behind the finger.
    val nowPlayingVisible by playerViewModel.state
        .map { it.nowPlayingVisible }
        .distinctUntilChanged()
        .collectAsState(initial = false)
    val overlays = LocalXvoxOverlayController.current
    val context = LocalContext.current
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var internalSelectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    val effectiveSelectedPlaylistId = if (onSelectedPlaylistIdChange != null) selectedPlaylistId else internalSelectedPlaylistId

    fun setSelectedPlaylistId(value: String?) {
        if (onSelectedPlaylistIdChange != null) onSelectedPlaylistIdChange(value)
        else internalSelectedPlaylistId = value
    }

    val prefs = remember { UserPreferencesRepository(context) }
    val config by viewModel.homePresentation.collectAsState()

    var showArtistInfo by remember { mutableStateOf<XvoxArtist?>(null) }
    var croppingArtistPhotoFor by remember { mutableStateOf<Pair<String, Uri>?>(null) }

    val artistPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && showArtistInfo != null) {
            croppingArtistPhotoFor = showArtistInfo!!.name to uri
        }
    }

    if (croppingArtistPhotoFor != null) {
        XvoxImageCropDialog(
            sourceUri = croppingArtistPhotoFor!!.second,
            isCircle = false,
            onCropped = { croppedUri ->
                viewModel.setArtistPhoto(croppingArtistPhotoFor!!.first, croppedUri)
                croppingArtistPhotoFor = null
                showArtistInfo = null
                overlays.showP("Artist photo updated")
            },
            onDismiss = { croppingArtistPhotoFor = null }
        )
    }

    val artists = remember(state.songs, state.customArtistImages, state.hiddenArtists, state.artistRenames) {
        val songsWithRenames = state.songs.map { song ->
            val renamed = state.artistRenames[song.artist]
            if (renamed != null) song.copy(artist = renamed) else song
        }
        songsWithRenames
            .filterNot { it.artist in state.hiddenArtists }
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { (artistName, songList) ->
                val cover = songList.firstOrNull { it.artworkUri != null } ?: songList.firstOrNull()
                XvoxArtist(
                    name = artistName,
                    songs = songList,
                    coverSong = cover,
                    customImageUri = state.customArtistImages[artistName]
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedArtist = remember(selectedArtistName, artists) {
        selectedArtistName?.let { name -> artists.firstOrNull { it.name.equals(name, ignoreCase = true) } }
    }

    // Song selection is intentionally independent from outer Artist / Playlist collection
    // selection. A selected artist/playlist receives its own accent outline and actions; opening
    // its detail then begins a fresh song-only selection context.
    var selectedSongIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedArtistNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedPlaylistIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionLibraryMode by remember { mutableStateOf(state.libraryMode) }
    var selectionCategoryName by remember { mutableStateOf<String?>(null) }
    val isSongSelectionMode = selectedSongIds.isNotEmpty()
    val isArtistSelectionMode = selectedArtistNames.isNotEmpty()
    val isPlaylistSelectionMode = selectedPlaylistIds.isNotEmpty()
    val isSelectionMode = isSongSelectionMode || isArtistSelectionMode || isPlaylistSelectionMode
    var pendingDeleteSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    if (showArtistInfo != null) {
        val currentArtist = showArtistInfo!!
        ArtistInfoDialog(
            artist = currentArtist,
            hideText = config.artistHideText,
            onHideTextChange = { viewModel.setArtistHideText(it) },
            onDismiss = { showArtistInfo = null },
            onPlayNext = {
                val artistSongs = currentArtist.songs.map { it.copy(source = currentArtist.name) }
                val msg = playerViewModel.playNextInQueue(artistSongs)
                overlays.showP(if (msg.isNotBlank()) msg else "Playing by ${currentArtist.name}")
            },
            onAddToQueue = {
                val artistSongs = currentArtist.songs.map { it.copy(source = currentArtist.name) }
                showMultiAddToQueueOverlay(overlays, playerViewModel, artistSongs)
            },
            onSelectArtist = {
                selectedSongIds = emptySet()
                selectedPlaylistIds = emptySet()
                selectedArtistNames = setOf(currentArtist.name)
                selectionCategoryName = "Artists"
                selectionLibraryMode = XvoxHomeLibraryMode.ARTISTS
                showArtistInfo = null
            },
            onHideArtist = {
                viewModel.hideArtist(currentArtist.name)
                overlays.showP("${currentArtist.name} hidden")
            }
        )
    }

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

    val activeAllSongsColumns = config.columnsFor(isLandscape)
    // Flowing Home pages deliberately stay viewport-sized. This prevents one tall grid item from
    // composing dozens of cover loaders during a fling; the artwork decode size remains unchanged.
    val flowingPageLimit = if (config.direction == "horizontal") null
    // Keep vertical pages deliberately shallow: only the cards around the viewport are composed
    // during a fling, while the exact same 256px/512px artwork request classes are prefetched.
    // Keep every normal vertical page on a full row boundary. In particular, six columns need
    // twelve items for two complete rows; the old ten-item cap produced a recurring 6 + 4 pattern.
    else (activeAllSongsColumns * 2).coerceIn(6, 12)
    val plans = remember(state.songs, config.style, config.rows, config.direction, activeAllSongsColumns, flowingPageLimit) {
        buildMosaicPagePlans(
            state.songs,
            config.rows.coerceIn(3, 10),
            config.style == "uniform",
            config.style == "mosaic1",
            cols = activeAllSongsColumns,
            maxSongsPerPage = flowingPageLimit
        )
    }
    val likedSongs = remember(state.songs, state.likedSongIds) { state.songs.filter { it.id in state.likedSongIds } }
    val songsById = remember(state.songs) { state.songs.associateBy { it.id } }
    val playlistContents = remember(songsById, state.playlists) {
        state.playlists.associate { it.id to it.songIds.mapNotNull(songsById::get) }
    }

    // Recent history is allowed to update immediately in storage, but the page currently being
    // read remains visually stable. Capture its order when entering Recent and reveal updates only
    // after navigating away, so an advancing/paused player never makes cards jump under a finger.
    var frozenRecentPageSongs by remember { mutableStateOf<List<Song>?>(null) }
    LaunchedEffect(state.libraryMode) {
        frozenRecentPageSongs = if (state.libraryMode == XvoxHomeLibraryMode.RECENT) {
            state.recentlyPlayed.toList()
        } else {
            null
        }
    }
    val visibleRecentPageSongs = if (state.libraryMode == XvoxHomeLibraryMode.RECENT) {
        frozenRecentPageSongs ?: state.recentlyPlayed
    } else {
        state.recentlyPlayed
    }

    LaunchedEffect(state.songs, artists, state.playlists) {
        selectedSongIds = selectedSongIds.intersect(state.songs.mapTo(HashSet()) { it.id })
        selectedArtistNames = selectedArtistNames.intersect(artists.mapTo(HashSet()) { it.name })
        selectedPlaylistIds = selectedPlaylistIds.intersect(state.playlists.mapTo(HashSet()) { it.id })
        if (!state.loading) onQueueReady(state.songs)
    }

    LaunchedEffect(homeResetKey) {
        if (homeResetKey > 0L) {
            selectedSongIds = emptySet()
            selectedArtistNames = emptySet()
            selectedPlaylistIds = emptySet()
            selectionCategoryName = null
            selectedArtistName = null
            setSelectedPlaylistId(null)
            viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
        }
    }

    LaunchedEffect(effectiveSelectedPlaylistId, state.libraryMode, selectedArtistName) {
        selectedSongIds = emptySet()
        // Navigating into a detail/page always ends the outer collection action surface. Songs
        // selected inside that detail begin a separate context with their own available actions.
        if (effectiveSelectedPlaylistId != null || selectedArtistName != null ||
            state.libraryMode != XvoxHomeLibraryMode.ARTISTS && state.libraryMode != XvoxHomeLibraryMode.PLAYLISTS
        ) {
            selectedArtistNames = emptySet()
            selectedPlaylistIds = emptySet()
        }
        // Artist details live under the Artists destination. Clearing the selected name as soon
        // as the destination recomposes made an artist's song page appear for one frame and then
        // vanish. Only another unrelated library destination closes that detail.
        if (state.libraryMode != XvoxHomeLibraryMode.ARTISTS &&
            state.libraryMode != XvoxHomeLibraryMode.ALL_SONGS
        ) {
            selectedArtistName = null
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedSongIds = emptySet()
        selectedArtistNames = emptySet()
        selectedPlaylistIds = emptySet()
        selectionCategoryName = null
    }
    BackHandler(enabled = !isSelectionMode && selectedArtist != null) { selectedArtistName = null }
    BackHandler(enabled = !isSelectionMode && selectedArtist == null && selectedPlaylist != null) { setSelectedPlaylistId(null) }
    BackHandler(enabled = !isSelectionMode && selectedArtist == null && selectedPlaylist == null && state.libraryMode != XvoxHomeLibraryMode.ALL_SONGS) {
        viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
    }

    fun openSingleSongOptions(song: Song, playlist: XvoxPlaylist? = null, recent: Boolean = false, selectionSource: XvoxHomeLibraryMode = state.libraryMode) {
        val actualSource = when {
            recent -> "Recently Played"
            playlist != null -> playlist.name
            selectedArtist != null -> selectedArtist!!.name
            selectionSource == XvoxHomeLibraryMode.LIKED -> "Liked Songs"
            song.source.isNotBlank() -> song.source
            else -> "All Songs"
        }
        val sourcedSong = song.copy(source = actualSource)

        val settingsAction: (() -> Unit)? = when {
            // Recent song options intentionally expose a gear for the persisted history capacity,
            // not the unrelated Home-layout controls used by All Songs.
            recent -> {
                {
                    overlays.showBox(title = "Recent capacity") {
                        RecentCapacityBoxContent(
                            currentCapacity = state.recentHistoryCapacity,
                            onSelect = { capacity ->
                                viewModel.setRecentHistoryCapacity(capacity)
                                overlays.hideBox()
                                overlays.showP("Recent history: ${com.xvox.music.data.preferences.XvoxRecentHistoryCapacity.label(capacity)}")
                            }
                        )
                    }
                }
            }
            selectionSource == XvoxHomeLibraryMode.LIKED -> null
            playlist != null -> {
                {
                    showPlaylistActions(
                        overlays = overlays,
                        viewModel = viewModel,
                        playlist = playlist,
                        onSelect = {
                            val plSongs = viewModel.playlistSongs(playlist)
                            selectedSongIds = plSongs.map { it.id }.toSet()
                            selectionCategoryName = playlist.name
                            selectionLibraryMode = XvoxHomeLibraryMode.PLAYLISTS
                        },
                        onDeleted = {}
                    )
                }
            }
            selectedArtist != null -> null
            else -> {
                {
                    overlays.showBox("All Songs Layout") {
                        AllSongsLayoutBoxContent(config, viewModel)
                    }
                }
            }
        }

        showSongOptionsOverlay(
            overlays = overlays,
            context = context,
            song = sourcedSong,
            isLiked = song.id in state.likedSongIds,
            playlist = playlist,
            playlistMembership = if (playlist == null) {
                state.playlists.filter { pl -> song.id in pl.songIds }
            } else emptyList(),
            recent = recent,
            viewModel = viewModel,
            playerViewModel = playerViewModel,
            playlists = state.playlists,
            songs = state.songs,
            deleteLauncher = deleteLauncher,
            onPendingDelete = { songToDelete: Song -> pendingDeleteSongs = listOf(songToDelete) },
            onSelect = {
                selectedArtistNames = emptySet()
                selectedPlaylistIds = emptySet()
                selectionLibraryMode = selectionSource
                selectionCategoryName = actualSource
                selectedSongIds = selectedSongIds + song.id
            },
            onSectionSettings = settingsAction
        )
    }

    fun handleSongClick(song: Song, list: List<Song>, sourceName: String) {
        if (isSelectionMode) {
            if (selectionCategoryName != null && !selectionCategoryName.equals(sourceName, ignoreCase = true)) {
                overlays.showP("You can't select from another category")
                return
            }
            selectedSongIds = if (song.id in selectedSongIds) selectedSongIds - song.id else selectedSongIds + song.id
            if (selectedSongIds.isEmpty()) {
                selectionCategoryName = null
            }
        } else {
            if (song.id == currentSongId) {
                playerViewModel.seekTo(0L)
                if (!isPlaying) playerViewModel.togglePlay()
            } else {
                viewModel.recordPlayedFromLibrary(song, currentSongId, sourceName)
                playerViewModel.playFromSource(song, list, sourceName)
            }
        }
    }

    fun handleSongLongClick(song: Song, sourceName: String) {
        if (isSelectionMode && selectionCategoryName != null && !selectionCategoryName.equals(sourceName, ignoreCase = true)) {
            overlays.showP("You can't select from another category")
            return
        }
        if (!isSelectionMode) {
            selectedArtistNames = emptySet()
            selectedPlaylistIds = emptySet()
            selectionCategoryName = sourceName
            selectedSongIds = setOf(song.id)
        } else {
            selectedSongIds = if (song.id in selectedSongIds) selectedSongIds - song.id else selectedSongIds + song.id
            if (selectedSongIds.isEmpty()) selectionCategoryName = null
        }
    }

    val selectedSongsList = remember(selectedSongIds, state.songs) {
        state.songs.filter { it.id in selectedSongIds }
    }
    val selectedArtists = remember(selectedArtistNames, artists) {
        artists.filter { it.name in selectedArtistNames }
    }
    val selectedOuterPlaylists = remember(selectedPlaylistIds, state.playlists) {
        state.playlists.filter { it.id in selectedPlaylistIds }
    }
    // The Select all rail action is deliberately source-scoped. Resolve the exact category that
    // started selection rather than falling back to the global library when a name is unfamiliar.
    val selectionScopeSongs = remember(
        selectionCategoryName,
        selectionLibraryMode,
        selectedPlaylist?.id,
        state.playlists,
        playlistContents,
        visibleRecentPageSongs,
        likedSongs,
        artists,
        state.songs
    ) {
        val category = selectionCategoryName.orEmpty()
        when {
            category.equals("All Songs", ignoreCase = true) -> state.songs
            category.equals("Recently Played", ignoreCase = true) -> visibleRecentPageSongs
            category.equals("Liked", ignoreCase = true) || category.equals("Liked Songs", ignoreCase = true) -> likedSongs
            selectedPlaylist != null && category.equals(selectedPlaylist.name, ignoreCase = true) ->
                playlistContents[selectedPlaylist.id].orEmpty()
            else -> {
                val playlist = state.playlists.firstOrNull { it.name.equals(category, ignoreCase = true) }
                when {
                    playlist != null -> playlistContents[playlist.id].orEmpty()
                    else -> {
                        val artistName = category.removePrefix("Playing by ")
                        artists.firstOrNull { it.name.equals(artistName, ignoreCase = true) }?.songs.orEmpty()
                    }
                }
            }
        }.distinctBy { it.id }
    }

    fun resolveOriginatingSongsForRecent(song: Song): Pair<List<Song>, String> {
        val src = state.recentSources[song.id]?.ifBlank { null } ?: song.source.ifBlank { "All Songs" }

        val pl = state.playlists.firstOrNull { it.name.equals(src, ignoreCase = true) }
        if (pl != null) {
            val plSongs = viewModel.playlistSongs(pl)
            if (plSongs.isNotEmpty()) return plSongs to pl.name
        }

        if (src.equals("Liked Songs", ignoreCase = true) || src.equals("Liked", ignoreCase = true)) {
            val liked = state.songs.filter { it.id in state.likedSongIds }
            if (liked.isNotEmpty()) return liked to "Liked Songs"
        }

        val art = artists.firstOrNull { it.name.equals(src, ignoreCase = true) || src.equals("Playing by ${it.name}", ignoreCase = true) }
        if (art != null && art.songs.isNotEmpty()) {
            return art.songs to "Playing by ${art.name}"
        }

        val savedQueue = playerViewModel.state.value.savedQueues.firstOrNull { it.name.equals(src, ignoreCase = true) }
        if (savedQueue != null && savedQueue.songs.isNotEmpty()) {
            return savedQueue.songs to savedQueue.name
        }

        return state.songs to (if (src.isNotBlank()) src else "All Songs")
    }

    fun androidx.compose.foundation.lazy.LazyListScope.recentSection() {
        item(key = "recent") {
            val recentSelected = if (selectionCategoryName == "Recently Played") selectedSongIds else emptySet()
            XvoxRecentlyPlayedSection(
                songs = visibleRecentPageSongs,
                currentSongId = currentSongId,
                isPlaying = isPlaying,
                transition = state.recentTransition,
                selectedSongIds = recentSelected,
                onSongClick = { song ->
                    if (isSelectionMode) handleSongLongClick(song, "Recently Played")
                    else if (song.id == currentSongId) {
                        playerViewModel.seekTo(0L)
                        if (!isPlaying) playerViewModel.togglePlay()
                    } else {
                        viewModel.recordPlayedFromRecent(song, currentSongId)
                        val (queueSongs, queueName) = resolveOriginatingSongsForRecent(song)
                        playerViewModel.playFromSource(song, queueSongs, queueName)
                    }
                },
                onSongOptions = { song ->
                    if (isSelectionMode) handleSongLongClick(song, "Recently Played")
                    else openSingleSongOptions(song, recent = true)
                },
                sources = state.recentSources
            )
        }
    }

    fun androidx.compose.foundation.lazy.LazyListScope.artistsSection(standalone: Boolean = false) {
        item(key = "artists_header") {
            HomeCollectionHeader("Artists", artists.size, onAdd = null)
        }
        item(key = "artists_grid") {
            XvoxArtistGrid(
                artists = artists,
                columns = 4,
                rows = 4,
                direction = "vertical",
                gap = 12,
                hideText = config.artistHideText,
                onArtistClick = { artist ->
                    if (isArtistSelectionMode) {
                        selectedArtistNames = if (artist.name in selectedArtistNames) {
                            selectedArtistNames - artist.name
                        } else {
                            selectedArtistNames + artist.name
                        }
                        if (selectedArtistNames.isEmpty()) selectionCategoryName = null
                    } else {
                        selectedArtistName = artist.name
                    }
                },
                onArtistLongClick = { artist ->
                    if (isArtistSelectionMode) {
                        // Once the outer rail is active, long press remains a normal selection
                        // toggle; it never mixes this collection state with song selection.
                        selectedArtistNames = if (artist.name in selectedArtistNames) {
                            selectedArtistNames - artist.name
                        } else {
                            selectedArtistNames + artist.name
                        }
                        if (selectedArtistNames.isEmpty()) selectionCategoryName = null
                    } else {
                        // Preserve the original artist option sheet. Its Select action enters the
                        // distinct outer artist-selection rail, instead of long press jumping
                        // straight into selection and hiding the prior actions.
                        showArtistInfo = artist
                    }
                },
                selectedArtistNames = selectedArtistNames,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }

    fun androidx.compose.foundation.lazy.LazyListScope.likedSection() {
        librarySongItems(
            keyPrefix = "liked",
            title = "Liked Songs",
            songs = likedSongs,
            currentSongId = currentSongId,
            playing = isPlaying,
            selected = selectedSongIds,
            columns = if (isLandscape) 2 else 1,
            onPlay = { handleSongClick(it, likedSongs, "Liked Songs") },
            onOptions = { if (isSelectionMode) handleSongLongClick(it, "Liked Songs") else openSingleSongOptions(it, selectionSource = XvoxHomeLibraryMode.LIKED) }
        )
    }

    fun androidx.compose.foundation.lazy.LazyListScope.playlistsSection(standalone: Boolean = false) {
        playlistCollectionItems(
            state.playlists,
            { playlistContents[it.id].orEmpty() },
            layoutStyle = config.playlistStyle,
            longCardHeight = config.playlistLongHeight,
            orientation = if (standalone) "vertical" else config.playlistCardOrientation,
            rows = config.playlistRows,
            columns = if (isLandscape) 2 else 1,
            selectedPlaylistIds = selectedPlaylistIds,
            onTogglePlaylistSelection = { playlist ->
                selectedSongIds = emptySet()
                selectedArtistNames = emptySet()
                selectionLibraryMode = XvoxHomeLibraryMode.PLAYLISTS
                selectionCategoryName = "Playlists"
                selectedPlaylistIds = if (playlist.id in selectedPlaylistIds) {
                    selectedPlaylistIds - playlist.id
                } else {
                    selectedPlaylistIds + playlist.id
                }
                if (selectedPlaylistIds.isEmpty()) selectionCategoryName = null
            },
            onCreate = { showCreatePlaylistOverlay(overlays, viewModel, state.songs) },
            onOpen = { setSelectedPlaylistId(it.id) },
            onOptions = { playlist ->
                showPlaylistActions(
                    overlays = overlays,
                    viewModel = viewModel,
                    playlist = playlist,
                    onSelect = {
                        selectedSongIds = emptySet()
                        selectedArtistNames = emptySet()
                        selectedPlaylistIds = setOf(playlist.id)
                        selectionCategoryName = "Playlists"
                        selectionLibraryMode = XvoxHomeLibraryMode.PLAYLISTS
                    },
                    onDeleted = {
                        if (effectiveSelectedPlaylistId == playlist.id) setSelectedPlaylistId(null)
                    },
                    selectTitle = "Select playlist"
                )
            }
        )
    }

    val bottomInset = LocalXvoxBottomInset.current
    // The shell reserves this for its single shared Header; Home itself never emits a duplicate.
    val topInset = LocalXvoxTopInset.current
    val targetKey = when {
        selectedArtist != null -> "artist_${selectedArtist!!.name}"
        effectiveSelectedPlaylistId != null -> effectiveSelectedPlaylistId
        else -> state.libraryMode
    }

    fun requestDeleteSelected() {
        if (selectedSongsList.isEmpty()) return
        overlays.showBox("Delete ${selectedSongsList.size} songs?") {
            com.xvox.music.shell.XvoxConfirmBox(
                question = "Permanently delete ${selectedSongsList.size} songs?",
                detail = "The files will be removed from storage. This cannot be undone.",
                confirmLabel = "Delete",
                danger = true,
                onCancel = overlays::hideBox,
                onConfirm = {
                    overlays.hideBox()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val pending = XvoxSongActions.deleteMultiplePendingIntent(context, selectedSongsList)
                        if (pending != null) {
                            pendingDeleteSongs = selectedSongsList
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(pending.intentSender).build()
                            )
                        }
                    } else {
                        var count = 0
                        selectedSongsList.forEach { s ->
                            if (XvoxSongActions.deleteLegacy(context, s)) {
                                playerViewModel.removeFromQueue(s.id)
                                count++
                            }
                        }
                        selectedSongIds = emptySet()
                        viewModel.refresh()
                        overlays.showP("$count songs deleted from device")
                    }
                }
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        val homeScrollState = rememberLazyListState()
        val likedScrollState = rememberLazyListState()
        val playlistsScrollState = rememberLazyListState()
        val artistsScrollState = rememberLazyListState()
        val detailScrollState = rememberLazyListState()

            val targetArtistName = (targetKey as? String)?.takeIf { it.startsWith("artist_") }?.removePrefix("artist_")
            val targetArtist = remember(targetArtistName, artists) {
                targetArtistName?.let { name -> artists.firstOrNull { it.name.equals(name, ignoreCase = true) } }
            }

            val targetPlaylist = (targetKey as? String)?.takeIf { !it.startsWith("artist_") }?.let { id ->
                state.playlists.firstOrNull { it.id == id }
            }
            val detailTracks = remember(targetPlaylist, playlistContents) {
                targetPlaylist?.let { playlistContents[it.id].orEmpty() } ?: emptyList()
            }

            val listState = when {
                targetArtist != null -> detailScrollState
                targetPlaylist != null -> detailScrollState
                targetKey == XvoxHomeLibraryMode.LIKED -> likedScrollState
                targetKey == XvoxHomeLibraryMode.PLAYLISTS -> playlistsScrollState
                targetKey == XvoxHomeLibraryMode.ARTISTS -> artistsScrollState
                else -> homeScrollState
            }

            // Library pills and navbar returns always start their selected page at the top. The
            // same LazyColumn remains mounted, so the Header is never rebuilt/faded during this.
            LaunchedEffect(targetKey, homeResetKey, scrollResetKey) {
                listState.scrollToItem(0)
            }

            LaunchedEffect(targetArtist?.name, targetPlaylist?.id) {
                if (targetArtist != null || targetPlaylist != null) {
                    detailScrollState.scrollToItem(0)
                }
            }

            val currentOnScrollProgress by androidx.compose.runtime.rememberUpdatedState(onScrollProgress)
            LaunchedEffect(listState) {
                androidx.compose.runtime.snapshotFlow {
                    Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
                }.collect { (index, offset) ->
                    currentOnScrollProgress(index, offset)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomInset)
            ) {
                // A real first list item gives the shell Header the exact same scroll distance as
                // the page beneath it. This avoids a separately animated Header drifting away
                // from Home content or leaving a blank band during a drag.
                item(key = "shell_header_scroll_spacer") {
                    Spacer(Modifier.height(topInset))
                }
                if (targetArtist != null) {
                    librarySongItems(
                        keyPrefix = "artist_detail",
                        title = targetArtist.name,
                        songs = targetArtist.songs,
                        currentSongId = currentSongId,
                        playing = isPlaying,
                        selected = selectedSongIds,
                        onPlay = { handleSongClick(it, targetArtist.songs, "Playing by " + targetArtist.name) },
                        onOptions = { if (isSelectionMode) handleSongLongClick(it, "Playing by " + targetArtist.name) else openSingleSongOptions(it) },
                        avatarUri = null
                    )
                } else if (targetPlaylist != null) {
                    librarySongItems(
                        keyPrefix = "playlist_detail",
                        title = targetPlaylist.name,
                        songs = detailTracks,
                        currentSongId = currentSongId,
                        playing = isPlaying,
                        selected = selectedSongIds,
                        onPlay = { handleSongClick(it, detailTracks, targetPlaylist.name) },
                        onOptions = { if (isSelectionMode) handleSongLongClick(it, targetPlaylist.name) else openSingleSongOptions(it, targetPlaylist) },
                        onAdd = { showAddPlaylistSongs(overlays, viewModel, targetPlaylist) }
                    )
                } else if (state.sourceMode == XvoxSourceMode.ONLINE) {
                    item(key = "online_provider_required") {
                        androidx.compose.material3.Text(
                            "Online mode is ready for an approved music provider. Configure its official discovery and playback API to search and play its catalogue. Device songs stay hidden in Online mode.",
                            color = com.xvox.music.core.design.theme.XvoxTheme.colors.secondaryText,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else when (targetKey as? XvoxHomeLibraryMode ?: XvoxHomeLibraryMode.ALL_SONGS) {
                    XvoxHomeLibraryMode.LIKED -> likedSection()
                    XvoxHomeLibraryMode.PLAYLISTS -> playlistsSection(standalone = true)
                    XvoxHomeLibraryMode.ARTISTS -> artistsSection(standalone = true)
                    XvoxHomeLibraryMode.RECENT -> recentSection()
                    XvoxHomeLibraryMode.SPLIT -> { }
                    XvoxHomeLibraryMode.ALL_SONGS -> {
                        // Recently Played now has its own upper-header destination, never a Home-feed card.
                        val sections = HomeSections.visible(config).filterNot { it == HomeSections.RECENT }
                        sections.forEach { section ->
                            when (section) {
                                HomeSections.ALL -> {
                                    val allSongsSelected = if (selectionCategoryName == "All Songs" || selectionCategoryName == null) selectedSongIds else emptySet()
                                    allSongsItems(
                                        state.songs, plans, config, currentSongId, isPlaying, allSongsSelected,
                                        onSongClick = { handleSongClick(it, state.songs, "All Songs") },
                                        onSongLongClick = { if (isSelectionMode) handleSongLongClick(it, "All Songs") else openSingleSongOptions(it) },
                                        onPrefetch = viewModel::prefetchFrom
                                    )
                                }
                                HomeSections.RECENT -> recentSection()
                                HomeSections.ARTISTS -> artistsSection()
                                HomeSections.LIKED -> likedSection()
                                HomeSections.PLAYLISTS -> playlistsSection()
                            }
                        }
                        if (sections.isEmpty()) {
                            item(key = "hidden_home") {
                                androidx.compose.material3.Text(
                                    "All Home sections are hidden. Show them in Settings → Home → Merge sections.",
                                    color = com.xvox.music.core.design.theme.XvoxTheme.colors.secondaryText,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }
                }
            }

        if (!nowPlayingVisible) {
            when {
                isArtistSelectionMode -> {
                    val artistSongs = selectedArtists.flatMap { it.songs }.distinctBy { it.id }
                    HomeCollectionMultiSelectBar(
                        title = "Artists",
                        selectedCount = selectedArtists.size,
                        allSelected = artists.isNotEmpty() && artists.all { it.name in selectedArtistNames },
                        onToggleAll = {
                            selectedArtistNames = if (artists.isNotEmpty() && artists.all { it.name in selectedArtistNames }) {
                                emptySet()
                            } else {
                                artists.mapTo(linkedSetOf()) { it.name }
                            }
                            if (selectedArtistNames.isEmpty()) selectionCategoryName = null
                            else selectionCategoryName = "Artists"
                        },
                        onClear = {
                            selectedArtistNames = emptySet()
                            selectionCategoryName = null
                        },
                        actions = buildList {
                            if (selectedArtists.size == 1) {
                                add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_info, "Artist options") {
                                    showArtistInfo = selectedArtists.first()
                                })
                            }
                            add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_play, "Play selected artists") {
                                val first = artistSongs.firstOrNull()
                                if (first != null) {
                                    playerViewModel.playFromSource(first, artistSongs, "Artists")
                                    selectedArtistNames = emptySet()
                                    selectionCategoryName = null
                                }
                            })
                            add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_queue, "Add selected artists to queue") {
                                overlays.showP(playerViewModel.addToQueue(artistSongs))
                                selectedArtistNames = emptySet()
                                selectionCategoryName = null
                            })
                            add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_playlist, "Add selected artists to playlist") {
                                showMultiAddToPlaylistOverlay(
                                    overlays = overlays,
                                    viewModel = viewModel,
                                    songs = artistSongs,
                                    onDone = {
                                        selectedArtistNames = emptySet()
                                        selectionCategoryName = null
                                    }
                                )
                            })
                            add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_delete, "Hide selected artists") {
                                selectedArtists.forEach { viewModel.hideArtist(it.name) }
                                overlays.showP("${selectedArtists.size} artists hidden")
                                selectedArtistNames = emptySet()
                                selectionCategoryName = null
                            })
                        }
                    )
                }
                isPlaylistSelectionMode -> {
                    val collectionSongs = selectedOuterPlaylists
                        .flatMap { playlistContents[it.id].orEmpty() }
                        .distinctBy { it.id }
                    HomeCollectionMultiSelectBar(
                        title = "Playlists",
                        selectedCount = selectedOuterPlaylists.size,
                        allSelected = state.playlists.isNotEmpty() && state.playlists.all { it.id in selectedPlaylistIds },
                        onToggleAll = {
                            selectedPlaylistIds = if (state.playlists.isNotEmpty() && state.playlists.all { it.id in selectedPlaylistIds }) {
                                emptySet()
                            } else {
                                state.playlists.mapTo(linkedSetOf()) { it.id }
                            }
                            if (selectedPlaylistIds.isEmpty()) selectionCategoryName = null
                            else selectionCategoryName = "Playlists"
                        },
                        onClear = {
                            selectedPlaylistIds = emptySet()
                            selectionCategoryName = null
                        },
                        actions = buildList {
                            if (selectedOuterPlaylists.size == 1) {
                                val playlist = selectedOuterPlaylists.first()
                                add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_info, "Playlist options") {
                                    showPlaylistActions(
                                        overlays = overlays,
                                        viewModel = viewModel,
                                        playlist = playlist,
                                        onSelect = { },
                                        onDeleted = {
                                            selectedPlaylistIds = selectedPlaylistIds - playlist.id
                                            if (selectedPlaylistIds.isEmpty()) selectionCategoryName = null
                                        }
                                    )
                                })
                            }
                            add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_play, "Play selected playlists") {
                                val first = collectionSongs.firstOrNull()
                                if (first != null) {
                                    val source = selectedOuterPlaylists.singleOrNull()?.name ?: "Playlists"
                                    playerViewModel.playFromSource(first, collectionSongs, source)
                                    selectedPlaylistIds = emptySet()
                                    selectionCategoryName = null
                                }
                            })
                            add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_queue, "Add selected playlists to queue") {
                                overlays.showP(playerViewModel.addToQueue(collectionSongs))
                                selectedPlaylistIds = emptySet()
                                selectionCategoryName = null
                            })
                            add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_share, "Share selected playlist songs") {
                                XvoxSongActions.shareMultiple(context, collectionSongs)
                                selectedPlaylistIds = emptySet()
                                selectionCategoryName = null
                            })
                            add(XvoxCollectionSelectionAction(R.drawable.ic_xvox_delete, "Delete selected playlists") {
                                val count = selectedOuterPlaylists.size
                                selectedOuterPlaylists.forEach { playlist ->
                                    viewModel.deletePlaylist(playlist.id) { }
                                }
                                selectedPlaylistIds = emptySet()
                                selectionCategoryName = null
                                overlays.showP("$count playlists deleted")
                            })
                        }
                    )
                }
                isSongSelectionMode -> {
                    // Song selection uses the same window-level right rail across All Songs,
                    // Recent, Liked, Artist details, and Playlist details.
                    HomeMultiSelectBar(
                        selectedSongs = selectedSongsList,
                        selectedPlaylist = selectedPlaylist,
                        libraryMode = selectionLibraryMode,
                        viewModel = viewModel,
                        playerViewModel = playerViewModel,
                        overlays = overlays,
                        context = context,
                        categoryName = selectionCategoryName,
                        allInScopeSelected = selectionScopeSongs.isNotEmpty() &&
                            selectionScopeSongs.all { it.id in selectedSongIds },
                        onToggleSelectAll = {
                            if (selectionScopeSongs.isEmpty()) {
                                overlays.showP("No songs in this category")
                            } else if (selectionScopeSongs.all { it.id in selectedSongIds }) {
                                selectedSongIds = selectedSongIds - selectionScopeSongs.map { it.id }.toSet()
                                if (selectedSongIds.isEmpty()) selectionCategoryName = null
                            } else {
                                selectedSongIds = selectionScopeSongs.mapTo(linkedSetOf()) { it.id }
                            }
                        },
                        onClearSelection = {
                            selectedSongIds = emptySet()
                            selectionCategoryName = null
                        },
                        onDeleteSelected = { requestDeleteSelected() }
                    )
                }
            }
        }
    }
}
