package com.xvox.music.features.home

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.xvox.music.player.playback.MainPlayerViewModel

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
    onScrollProgress: (Int, Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
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

    var selectedSongIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectionLibraryMode by remember { mutableStateOf(state.libraryMode) }
    var selectionCategoryName by remember { mutableStateOf<String?>(null) }
    val isSelectionMode = selectedSongIds.isNotEmpty()
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
                selectedSongIds = currentArtist.songs.map { it.id }.toSet()
                selectionCategoryName = currentArtist.name
                selectionLibraryMode = XvoxHomeLibraryMode.ARTISTS
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

    val plans = remember(state.songs, config.style, config.rows, isLandscape) {
        val cols = if (isLandscape) 8 else 4
        buildMosaicPagePlans(state.songs, config.rows, config.style == "uniform", config.style == "mosaic1", cols = cols)
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
            selectedArtistName = null
            setSelectedPlaylistId(null)
            viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
        }
    }

    LaunchedEffect(effectiveSelectedPlaylistId, state.libraryMode) {
        selectedSongIds = emptySet()
        if (state.libraryMode != XvoxHomeLibraryMode.ALL_SONGS) {
            selectedArtistName = null
        }
    }

    BackHandler(enabled = isSelectionMode) { selectedSongIds = emptySet() }
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
            recent -> {
                {
                    overlays.showBox("Recently Played") {
                        RecentLayoutBoxContent(config, viewModel)
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
                songs = state.recentlyPlayed,
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
                    handleSongLongClick(song, "Recently Played")
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
                onArtistClick = { selectedArtistName = it.name },
                onArtistLongClick = { showArtistInfo = it },
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
            onCreate = { showCreatePlaylistOverlay(overlays, viewModel, state.songs) },
            onOpen = { setSelectedPlaylistId(it.id) },
            onOptions = { playlist ->
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
                    onDeleted = {
                        if (effectiveSelectedPlaylistId == playlist.id) setSelectedPlaylistId(null)
                    }
                )
            }
        )
    }

    val topInset = LocalXvoxTopInset.current
    val bottomInset = LocalXvoxBottomInset.current
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

        LaunchedEffect(homeResetKey, scrollResetKey) {
            if (homeResetKey > 0L || scrollResetKey > 0L) {
                homeScrollState.scrollToItem(0)
            }
        }

        AnimatedContent(
            targetState = targetKey,
            transitionSpec = { (fadeIn(tween(180)) togetherWith fadeOut(tween(140))).using(null) },
            modifier = Modifier.fillMaxSize(),
            label = "libraryFade"
        ) { target ->
            val targetArtistName = (target as? String)?.takeIf { it.startsWith("artist_") }?.removePrefix("artist_")
            val targetArtist = remember(targetArtistName, artists) {
                targetArtistName?.let { name -> artists.firstOrNull { it.name.equals(name, ignoreCase = true) } }
            }

            val targetPlaylist = (target as? String)?.takeIf { !it.startsWith("artist_") }?.let { id ->
                state.playlists.firstOrNull { it.id == id }
            }
            val detailTracks = remember(targetPlaylist, playlistContents) {
                targetPlaylist?.let { playlistContents[it.id].orEmpty() } ?: emptyList()
            }

            val listState = when {
                targetArtist != null -> detailScrollState
                targetPlaylist != null -> detailScrollState
                target == XvoxHomeLibraryMode.LIKED -> likedScrollState
                target == XvoxHomeLibraryMode.PLAYLISTS -> playlistsScrollState
                target == XvoxHomeLibraryMode.ARTISTS -> artistsScrollState
                else -> homeScrollState
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
                contentPadding = PaddingValues(
                    top = if (isSelectionMode) 8.dp else topInset + 10.dp,
                    bottom = bottomInset
                )
            ) {
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
                } else when (target as? XvoxHomeLibraryMode ?: XvoxHomeLibraryMode.ALL_SONGS) {
                    XvoxHomeLibraryMode.LIKED -> likedSection()
                    XvoxHomeLibraryMode.PLAYLISTS -> playlistsSection(standalone = true)
                    XvoxHomeLibraryMode.ARTISTS -> artistsSection(standalone = true)
                    XvoxHomeLibraryMode.SPLIT -> { }
                    XvoxHomeLibraryMode.ALL_SONGS -> {
                        val sections = HomeSections.visible(config)
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
        }

        if (isSelectionMode) {
            HomeMultiSelectBar(
                selectedSongs = selectedSongsList,
                selectedPlaylist = selectedPlaylist,
                libraryMode = selectionLibraryMode,
                viewModel = viewModel,
                playerViewModel = playerViewModel,
                overlays = overlays,
                context = context,
                categoryName = selectionCategoryName,
                onClearSelection = { selectedSongIds = emptySet() },
                onDeleteSelected = { requestDeleteSelected() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset + (if (currentSongId != null) 72.dp else 16.dp))
                    .zIndex(9999f)
            )
        }
    }
}
