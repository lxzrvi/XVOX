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
import androidx.compose.foundation.layout.heightIn
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

    val prefs = remember { UserPreferencesRepository(context) }
    val config by prefs.homePresentation.collectAsState(initial = HomePresentation())

    var selectedArtist by remember { mutableStateOf<XvoxArtist?>(null) }
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
            isCircle = true,
            onCropped = { croppedUri ->
                viewModel.setArtistPhoto(croppingArtistPhotoFor!!.first, croppedUri)
                croppingArtistPhotoFor = null
                showArtistInfo = null
                overlays.showP("Artist photo updated")
            },
            onDismiss = { croppingArtistPhotoFor = null }
        )
    }

    if (showArtistInfo != null) {
        val currentArtist = showArtistInfo!!
        ArtistInfoDialog(
            artist = currentArtist,
            allArtists = artists,
            config = config,
            homeViewModel = viewModel,
            columns = config.artistColumns,
            gap = config.artistGap,
            hideText = config.artistHideText,
            mergedToHome = config.merge && HomeSections.ARTISTS in HomeSections.visible(config),
            onColumnsChange = { viewModel.setArtistColumns(it) },
            onGapChange = { viewModel.setArtistGap(it) },
            onHideTextChange = { viewModel.setArtistHideText(it) },
            onMergeToHomeChange = { mergeOn ->
                if (mergeOn) {
                    viewModel.setHomeMerge(true)
                    viewModel.setHomeSectionVisible(HomeSections.ARTISTS, true)
                } else {
                    viewModel.setHomeSectionVisible(HomeSections.ARTISTS, false)
                }
            },
            onRenameArtist = { oldName, newName, merge ->
                viewModel.renameArtist(oldName, newName, merge)
                overlays.showP("Artist renamed to $newName")
            },
            onSaveArtistPhoto = { name, uri ->
                viewModel.setArtistPhoto(name, uri)
                overlays.showP("Artist photo updated")
            },
            onDismiss = { showArtistInfo = null },
            onPlayNext = {
                playerViewModel.playNextInQueue(currentArtist.songs)
                overlays.showP("Playing by ${currentArtist.name}")
            },
            onEditPhoto = {
                artistPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onHideArtist = {
                viewModel.hideArtist(currentArtist.name)
                overlays.showP("${currentArtist.name} hidden")
            }
        )
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

    val plans = remember(state.songs, config.style, config.rows) {
        buildMosaicPagePlans(state.songs, config.rows, config.style == "uniform", config.style == "mosaic1")
    }
    val likedSongs = remember(state.songs, state.likedSongIds) { state.songs.filter { it.id in state.likedSongIds } }
    val songsById = remember(state.songs) { state.songs.associateBy { it.id } }
    val playlistContents = remember(songsById, state.playlists) {
        state.playlists.associate { it.id to it.songIds.mapNotNull(songsById::get) }
    }

    val artists = remember(state.songs, state.customArtistImages, state.hiddenArtists) {
        state.songs
            .filterNot { it.artist in state.hiddenArtists }
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { (artistName, songList) ->
                XvoxArtist(
                    name = artistName,
                    songs = songList,
                    coverSong = songList.firstOrNull { it.artworkUri != null } ?: songList.firstOrNull(),
                    customImageUri = state.customArtistImages[artistName]
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    LaunchedEffect(state.songs) {
        selectedSongIds = selectedSongIds.intersect(state.songs.mapTo(HashSet()) { it.id })
        if (!state.loading) onQueueReady(state.songs)
    }

    LaunchedEffect(homeResetKey) {
        if (homeResetKey > 0L) {
            selectedSongIds = emptySet()
            selectedArtist = null
            setSelectedPlaylistId(null)
            viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
        }
    }

    LaunchedEffect(effectiveSelectedPlaylistId, state.libraryMode) {
        selectedSongIds = emptySet()
        selectedArtist = null
    }

    BackHandler(enabled = isSelectionMode) { selectedSongIds = emptySet() }
    BackHandler(enabled = !isSelectionMode && selectedArtist != null) { selectedArtist = null }
    BackHandler(enabled = !isSelectionMode && selectedArtist == null && selectedPlaylist != null) { setSelectedPlaylistId(null) }
    BackHandler(enabled = !isSelectionMode && selectedArtist == null && selectedPlaylist == null && state.libraryMode != XvoxHomeLibraryMode.ALL_SONGS) {
        viewModel.setLibraryMode(XvoxHomeLibraryMode.ALL_SONGS)
    }

    fun openSingleSongOptions(song: Song, playlist: XvoxPlaylist? = null, recent: Boolean = false, selectionSource: XvoxHomeLibraryMode = state.libraryMode) {
        val (settingsLabel, settingsAction) = when {
            recent -> "Recently Played Settings" to {
                overlays.showBox("Recently Played") {
                    RecentLayoutBoxContent(config, viewModel)
                }
            }
            selectionSource == XvoxHomeLibraryMode.LIKED -> "Liked Songs Settings" to {
                overlays.showBox("Liked Songs") {
                    LikedSongsLayoutBoxContent(config, viewModel)
                }
            }
            else -> "All Songs Settings" to {
                overlays.showBox("All Songs Layout") {
                    AllSongsLayoutBoxContent(config, viewModel)
                }
            }
        }

        showSongOptionsOverlay(
            overlays = overlays,
            context = context,
            song = song,
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
            onSelect = { selectionLibraryMode = selectionSource; selectedSongIds = selectedSongIds + song.id },
            sectionSettingsLabel = settingsLabel,
            onSectionSettings = settingsAction
        )
    }

    fun handleSongClick(song: Song, list: List<Song>, sourceName: String) {
        if (isSelectionMode) {
            selectedSongIds = if (song.id in selectedSongIds) selectedSongIds - song.id else selectedSongIds + song.id
        } else {
            viewModel.recordPlayedFromLibrary(song, currentSongId, sourceName)
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
                songs = state.recentlyPlayed,
                currentSongId = currentSongId,
                isPlaying = isPlaying,
                transition = state.recentTransition,
                onSongClick = { song ->
                    if (isSelectionMode) handleSongLongClick(song)
                    else if (song.id == currentSongId) playerViewModel.togglePlay()
                    else {
                        viewModel.recordPlayedFromRecent(song, currentSongId)
                        playerViewModel.playFromSource(song, state.recentlyPlayed, "Recently Played")
                    }
                },
                onSongOptions = { song -> openSingleSongOptions(song, recent = true) },
                sources = state.recentSources,
                onSourceClick = { song ->
                    overlays.showP(com.xvox.music.features.home.recent.RecentSource.describe(state.recentSources[song.id]))
                }
            )
        }
    }

    fun androidx.compose.foundation.lazy.LazyListScope.artistsSection() {
        item(key = "artists_header") {
            HomeCollectionHeader("Artists", artists.size, onAdd = null)
        }
        item(key = "artists_grid") {
            XvoxArtistGrid(
                artists = artists,
                columns = config.artistColumns,
                gap = config.artistGap,
                hideText = config.artistHideText,
                onArtistClick = { selectedArtist = it },
                onArtistLongClick = { showArtistInfo = it },
                modifier = Modifier.fillMaxWidth()
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
            onPlay = { handleSongClick(it, likedSongs, "Liked Songs") },
            onOptions = { if (isSelectionMode) handleSongLongClick(it) else openSingleSongOptions(it, selectionSource = XvoxHomeLibraryMode.LIKED) }
        )
    }

    fun androidx.compose.foundation.lazy.LazyListScope.playlistsSection() {
        playlistCollectionItems(
            state.playlists,
            { playlistContents[it.id].orEmpty() },
            layoutStyle = config.playlistStyle,
            longCardHeight = config.playlistLongHeight,
            orientation = config.playlistCardOrientation,
            onCreate = { showCreatePlaylistOverlay(overlays, viewModel, state.songs) },
            onOpen = { setSelectedPlaylistId(it.id) },
            onOptions = { playlist ->
                showPlaylistActions(overlays, viewModel, playlist) {
                    if (effectiveSelectedPlaylistId == playlist.id) setSelectedPlaylistId(null)
                }
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

    Column(Modifier.fillMaxSize()) {
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

        if (isSelectionMode) {
            Spacer(Modifier.height(topInset))
            HomeMultiSelectBar(
                selectedSongsList, selectedPlaylist, selectionLibraryMode,
                viewModel, overlays, context,
                onClearSelection = { selectedSongIds = emptySet() },
                onDeleteSelected = ::requestDeleteSelected
            )
        }

        AnimatedContent(
            targetState = targetKey,
            transitionSpec = { (fadeIn(tween(180)) togetherWith fadeOut(tween(140))).using(null) },
            modifier = Modifier.weight(1f),
            label = "libraryFade"
        ) { target ->
            val listState = rememberLazyListState()
            LaunchedEffect(homeResetKey, scrollResetKey) {
                listState.scrollToItem(0)
            }

            val targetPlaylist = (target as? String)?.takeIf { !it.startsWith("artist_") }?.let { id ->
                state.playlists.firstOrNull { it.id == id }
            }
            val detailTracks = remember(targetPlaylist, playlistContents) {
                targetPlaylist?.let { playlistContents[it.id].orEmpty() } ?: emptyList()
            }

            val currentSelectedArtist = selectedArtist

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = if (isSelectionMode) 8.dp else topInset + 10.dp,
                    bottom = bottomInset
                )
            ) {
                if (currentSelectedArtist != null && target == "artist_${currentSelectedArtist.name}") {
                    librarySongItems(
                        keyPrefix = "artist_detail",
                        title = currentSelectedArtist.name,
                        songs = currentSelectedArtist.songs,
                        currentSongId = currentSongId,
                        playing = isPlaying,
                        selected = selectedSongIds,
                        onPlay = { handleSongClick(it, currentSelectedArtist.songs, "Playing by " + currentSelectedArtist.name) },
                        onOptions = { if (isSelectionMode) handleSongLongClick(it) else openSingleSongOptions(it) }
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
                        onOptions = { if (isSelectionMode) handleSongLongClick(it) else openSingleSongOptions(it, targetPlaylist) },
                        onAdd = { showAddPlaylistSongs(overlays, viewModel, targetPlaylist) }
                    )
                } else when (target as? XvoxHomeLibraryMode ?: XvoxHomeLibraryMode.ALL_SONGS) {
                    XvoxHomeLibraryMode.LIKED -> likedSection()
                    XvoxHomeLibraryMode.PLAYLISTS -> playlistsSection()
                    XvoxHomeLibraryMode.ARTISTS -> artistsSection()
                    XvoxHomeLibraryMode.SPLIT -> { }
                    XvoxHomeLibraryMode.ALL_SONGS -> {
                        val sections = HomeSections.visible(config)
                        sections.forEach { section ->
                            when (section) {
                                HomeSections.ALL -> allSongsItems(
                                    state.songs, plans, config, currentSongId, isPlaying, selectedSongIds,
                                    onSongClick = { handleSongClick(it, state.songs, "All Songs") },
                                    onSongLongClick = { if (isSelectionMode) handleSongLongClick(it) else openSingleSongOptions(it) },
                                    onPrefetch = viewModel::prefetchFrom
                                )
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
    }
}
