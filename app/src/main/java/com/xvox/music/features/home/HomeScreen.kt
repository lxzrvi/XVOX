package com.xvox.music.features.home

import android.app.Activity
import android.app.PendingIntent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
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
import com.xvox.music.features.home.HomeGeometry
import com.xvox.music.features.home.allsongs.XvoxAllSongsSection
import com.xvox.music.features.home.recent.RecentTransitionMode
import com.xvox.music.features.home.recent.RecentTransitionRequest
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
    playerViewModel: MainPlayerViewModel =
        viewModel(),
    viewModel: HomeViewModel =
        viewModel(),
) {
    val state by
        viewModel.state
            .collectAsState()

    val colors =
        XvoxTheme.colors

    val overlays =
        LocalXvoxOverlayController
            .current

    val context =
        LocalContext.current

    // Hoisted selectedPlaylistId to preserve across tab switches (XvoxMainShell)
    // If hoisted callback provided, use external state; otherwise internal remember for backward compat
    var internalSelectedPlaylistId by
        remember {
            mutableStateOf<String?>(
                null,
            )
        }

    val effectiveSelectedPlaylistId = if (onSelectedPlaylistIdChange != null) selectedPlaylistId else internalSelectedPlaylistId

    fun setSelectedPlaylistId(value: String?) {
        if (onSelectedPlaylistIdChange != null) {
            onSelectedPlaylistIdChange(value)
        } else {
            internalSelectedPlaylistId = value
        }
    }

    var pendingDelete by
        remember {
            mutableStateOf<Song?>(
                null,
            )
        }

    val selectedPlaylist =
        effectiveSelectedPlaylistId
            ?.let { id ->
                state.playlists
                    .firstOrNull {
                        it.id == id
                    }
            }

    val deleteLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartIntentSenderForResult(),
        ) { result ->
            val song =
                pendingDelete

            pendingDelete = null

            if (
                result.resultCode ==
                Activity.RESULT_OK &&
                song != null
            ) {
                playerViewModel
                    .removeFromQueue(
                        song.id,
                    )

                viewModel.refresh()

                overlays.showP(
                    "Deleted from device",
                )
            }
        }

    val statusHeight =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()

    // Fix cold start UI broken behind header: ensure headerSpace at least 24dp + header height even if insets not ready
    // Header height = 54dp + 8dp bottom + statusHeight. Add sectionGap as gap below header.
    val headerSpace =
        (statusHeight + 54.dp + 8.dp + HomeGeometry.sectionGap).coerceAtLeast(76.dp)

    val screenHeight =
        LocalConfiguration.current
            .screenHeightDp.dp

    LaunchedEffect(
        state.songs,
    ) {
        if (
            state.songs.isNotEmpty()
        ) {
            onQueueReady(
                state.songs,
            )
        }
    }

    LaunchedEffect(
        homeResetKey,
    ) {
        if (
            homeResetKey > 0L
        ) {
            setSelectedPlaylistId(null)

            viewModel.setLibraryMode(
                XvoxHomeLibraryMode.ALL_SONGS,
            )
        }
    }

    BackHandler(
        enabled =
            selectedPlaylist != null,
    ) {
        setSelectedPlaylistId(null)
    }

    BackHandler(
        enabled =
            selectedPlaylist == null &&
                state.libraryMode !=
                XvoxHomeLibraryMode.ALL_SONGS,
    ) {
        viewModel.setLibraryMode(
            XvoxHomeLibraryMode.ALL_SONGS,
        )
    }

    fun showCreatePlaylist(initialSong: Song? = null) {
        overlays.showL {
            CreatePlaylistBox(
                songs =
                    state.songs,
                initialSong =
                initialSong,
                onCreate = {
                    name,
                    ids,
                    ->

                    viewModel.createPlaylist(
                        name,
                        ids,
                    ) { playlist ->
                        overlays.hideL()

                        if (
                            playlist != null
                        ) {
                            overlays.showP(
                                "Playlist created",
                            )
                        }
                    }
                },
            )
        }
    }

    fun showPlaylistPicker(song: Song) {
        overlays.showL {
            PlaylistPickerBox(
                song = song,
                playlists =
                    state.playlists,
                onCreate = {
                    showCreatePlaylist(
                        song,
                    )
                },
                onAdd = { playlist ->

                    viewModel.addToPlaylist(
                        playlist.id,
                        song,
                    ) { updated ->
                        if (
                            updated != null
                        ) {
                            overlays.hideL()

                            overlays.showP(
                                "Added to ${updated.name}",
                            )
                        }
                    }
                },
                onRemove = { playlist ->

                    viewModel
                        .removeFromPlaylist(
                            playlist.id,
                            song,
                        ) { updated ->
                            if (
                                updated != null
                            ) {
                                overlays.hideL()

                                overlays.showP(
                                    "Removed from ${updated.name}",
                                )
                            }
                        }
                },
                songs = state.songs,
                songsFor = viewModel::playlistSongs,
            )
        }
    }

    fun showDelete(song: Song) {
        overlays.showL {
            DeleteSongBox(
                song = song,
                onRemoveApp = {
                    playerViewModel
                        .removeFromQueue(
                            song.id,
                        )

                    viewModel.hideSong(
                        song,
                    )

                    overlays.hideL()

                    overlays.showP(
                        "Removed from XVOX",
                    )
                },
                onDeleteDevice = {
                    overlays.showL {
                        ConfirmDeviceDeleteBox(
                            song = song,
                            onCancel =
                                overlays::hideL,
                            onDelete = {
                                if (
                                    Build.VERSION.SDK_INT >=
                                    Build.VERSION_CODES.R
                                ) {
                                    val pending:
                                        PendingIntent? =
                                        XvoxSongActions
                                            .deletePendingIntent(
                                                context,
                                                song,
                                            )

                                    if (
                                        pending != null
                                    ) {
                                        pendingDelete =
                                            song

                                        overlays.hideL()

                                        deleteLauncher.launch(
                                            IntentSenderRequest
                                                .Builder(
                                                    pending
                                                        .intentSender,
                                                ).build(),
                                        )
                                    }
                                } else {
                                    val deleted =
                                        XvoxSongActions
                                            .deleteLegacy(
                                                context,
                                                song,
                                            )

                                    overlays.hideL()

                                    if (deleted) {
                                        playerViewModel
                                            .removeFromQueue(
                                                song.id,
                                            )

                                        viewModel.refresh()

                                        overlays.showP(
                                            "Deleted from device",
                                        )
                                    }
                                }
                            },
                        )
                    }
                },
            )
        }
    }

    fun showSongOptions(
        song: Song,
        playlist: XvoxPlaylist? = null,
        recent: Boolean = false,
    ) {
        overlays.showL {
            SongOptionsSheet(
                song = song,
                liked =
                    song.id in
                        state.likedSongIds,
                playlistName =
                    playlist?.name,
                onPlayNext = {
                    playerViewModel
                        .playNextInQueue(
                            song,
                        )

                    overlays.hideL()

                    overlays.showP(
                        "Playing next",
                    )
                },
                onAddQueue = {
                    playerViewModel
                        .addToQueue(
                            song,
                        )

                    overlays.hideL()

                    overlays.showP(
                        "Added to queue",
                    )
                },
                onPlaylist = {
                    overlays.hideL()

                    showPlaylistPicker(
                        song,
                    )
                },
                onRemovePlaylist =
                    playlist
                        ?.let { target ->

                            {
                                viewModel
                                    .removeFromPlaylist(
                                        target.id,
                                        song,
                                    ) {
                                        overlays.hideL()

                                        overlays.showP(
                                            "Removed from ${target.name}",
                                        )
                                    }
                            }
                        },
                onRemoveRecent =
                    if (recent) {
                        {
                            viewModel
                                .removeFromRecent(
                                    song,
                                )

                            overlays.hideL()

                            overlays.showP(
                                "Removed from recent",
                            )
                        }
                    } else {
                        null
                    },
                onLiked = {
                    val wasLiked =
                        song.id in
                            state.likedSongIds

                    viewModel
                        .toggleLiked(
                            song,
                        )

                    overlays.hideL()

                    overlays.showP(
                        if (wasLiked) {
                            "Removed from liked"
                        } else {
                            "Added to liked"
                        },
                    )
                },
                onDelete = {
                    overlays.hideL()

                    showDelete(
                        song,
                    )
                },
                onInfo = {
                    overlays.hideL()

                    viewModel.loadInfo(
                        song,
                    ) { info ->
                        overlays.showL {
                            SongInfoBox(
                                info,
                            )
                        }
                    }
                },
                onRingtone = {
                    overlays.hideL()

                    if (
                        XvoxSongActions
                            .canWriteSettings(
                                context,
                            )
                    ) {
                        val success =
                            XvoxSongActions
                                .setRingtone(
                                    context,
                                    song,
                                )

                        overlays.showP(
                            if (success) {
                                "Ringtone set"
                            } else {
                                "Couldn't set ringtone"
                            },
                        )
                    } else {
                        XvoxSongActions
                            .openWriteSettings(
                                context,
                            )

                        overlays.showP(
                            "Allow modify system settings",
                        )
                    }
                },
                onShare = {
                    overlays.hideL()

                    XvoxSongActions.share(
                        context,
                        song,
                    )
                },
            )
        }
    }

    fun showProfileEditor() {
        overlays.showL {
            ProfileEditorBox(
                profile =
                    state.profile,
                onCancel =
                    overlays::hideL,
                onSave = {
                    name,
                    selectedPfp,
                    customUri,
                    ->

                    viewModel.saveProfile(
                        username = name,
                        selectedPfp =
                        selectedPfp,
                        customPfpUri =
                        customUri,
                    ) {
                        overlays.hideL()

                        overlays.showP(
                            "Profile updated",
                        )
                    }
                },
            )
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    colors.background,
                ),
    ) {
        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = 4.dp,
                ),
        ) {
            item(
                key = "library",
            ) {
                if (
                    selectedPlaylist !=
                    null
                ) {
                    XvoxPlaylistDetail(
                        playlist =
                        selectedPlaylist,
                        songs =
                            viewModel
                                .playlistSongs(
                                    selectedPlaylist,
                                ),
                        currentSongId =
                        currentSongId,
                        isPlaying =
                        isPlaying,
                        onPlay = { song ->

                            viewModel
                                .recordPlayedFromLibrary(
                                    song,
                                    currentSongId,
                                )

                            playerViewModel.playFromSource(song, viewModel.playlistSongs(selectedPlaylist), selectedPlaylist.name)
                        },
                        onOptions = { song ->

                            showSongOptions(
                                song,
                                selectedPlaylist,
                            )
                        },
                        onAddSongs = {
                            showAddPlaylistSongs(
                                overlays =
                                overlays,
                                viewModel =
                                viewModel,
                                playlist =
                                selectedPlaylist,
                            )
                        },
                        onClosed = {
                            setSelectedPlaylistId(null)
                        },
                    )
                } else {
                    when (
                        state.libraryMode
                    ) {
                        XvoxHomeLibraryMode
                            .ALL_SONGS,
                        -> {
                            XvoxAllSongsSection(
                                songs =
                                    state.songs,
                                currentSongId =
                                currentSongId,
                                isPlaying =
                                isPlaying,
                                onSongClick = { song ->

                                    viewModel
                                        .recordPlayedFromLibrary(
                                            song,
                                            currentSongId,
                                        )

                                    playerViewModel.playFromSource(song, state.songs, "All Songs")
                                },
                                onSongLongClick = { song ->

                                    showSongOptions(
                                        song,
                                    )
                                },
                                onPrefetch =
                                    viewModel::prefetchFrom,
                            )
                        }

                        XvoxHomeLibraryMode
                            .LIKED,
                        -> {
                            XvoxLikedSongsSection(
                                songs =
                                    viewModel
                                        .likedSongs(),
                                currentSongId =
                                currentSongId,
                                isPlaying =
                                isPlaying,
                                onPlay = { song ->

                                    viewModel
                                        .recordPlayedFromLibrary(
                                            song,
                                            currentSongId,
                                        )

                                    playerViewModel.playFromSource(song, viewModel.likedSongs(), "Liked Songs")
                                },
                                onOptions = { song ->

                                    showSongOptions(
                                        song,
                                    )
                                },
                            )
                        }

                        XvoxHomeLibraryMode
                            .PLAYLISTS,
                        -> {
                            XvoxPlaylistsSection(
                                playlists =
                                    state.playlists,
                                songsFor =
                                    viewModel::playlistSongs,
                                onCreate = {
                                    showCreatePlaylist()
                                },
                                onOpen = { playlist ->

                                    setSelectedPlaylistId(playlist.id)
                                },
                                onOptions = { playlist ->

                                    showPlaylistActions(
                                        overlays =
                                        overlays,
                                        viewModel =
                                        viewModel,
                                        playlist =
                                        playlist,
                                        onDeleted = {
                                            if (
                                                effectiveSelectedPlaylistId ==
                                                playlist.id
                                            ) {
                                                setSelectedPlaylistId(null)
                                            }
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (
                effectiveSelectedPlaylistId == null &&
                state.libraryMode ==
                XvoxHomeLibraryMode.ALL_SONGS
            ) {
                item(
                    key = "recent",
                ) {
                    XvoxRecentlyPlayedSection(
                        songs =
                            state.recentlyPlayed,
                        currentSongId =
                        currentSongId,
                        isPlaying =
                        isPlaying,
                        transition =
                            state.recentTransition,
                        onSongClick = { song ->

                            viewModel
                                .recordPlayedFromRecent(
                                    song,
                                    currentSongId,
                                )

                            playerViewModel.playFromSource(song, state.recentlyPlayed, "Recently Played")
                        },
                        onSongOptions = { song ->

                            showSongOptions(
                                song = song,
                                recent = true,
                            )
                        },
                    )
                }
            }

            item(
                key = "footer",
            ) {
                HomeFooter(
                    modifier =
                        Modifier
                            .fillParentMaxWidth()
                            .heightIn(
                                min =
                                screenHeight,
                            ),
                )
            }
        }
    }
}
