package com.xvox.music.features.home

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.LocalXvoxOverlayController
import com.xvox.music.player.playback.MainPlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    currentSongId: Long?,
    isPlaying: Boolean,
    onQueueReady: (List<Song>) -> Unit,
    onPlay: (Song) -> Unit,
    playerViewModel:
        MainPlayerViewModel =
        viewModel(),
    viewModel: HomeViewModel =
        viewModel()
) {
    val state by
        viewModel.state.collectAsState()

    val colors = XvoxTheme.colors
    val overlays =
        LocalXvoxOverlayController.current

    val context = LocalContext.current

    val activity =
        context as? Activity

    var pendingDelete =
        remember {
            androidx.compose.runtime
                .mutableStateOf<Song?>(
                    null
                )
        }

    val deleteLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartIntentSenderForResult()
        ) { result ->
            val song =
                pendingDelete.value

            pendingDelete.value = null

            if (
                result.resultCode ==
                Activity.RESULT_OK &&
                song != null
            ) {
                playerViewModel
                    .removeFromQueue(
                        song.id
                    )

                viewModel.refresh()

                overlays.showP(
                    "Deleted from device"
                )
            }
        }

    val statusHeight =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()

    val headerSpace =
        statusHeight +
            54.dp +
            8.dp +
            HomeGeometry.sectionGap

    val screenHeight =
        LocalConfiguration.current
            .screenHeightDp.dp

    LaunchedEffect(state.songs) {
        if (state.songs.isNotEmpty()) {
            onQueueReady(state.songs)
        }
    }

    fun showPlaylistPicker(
        song: Song
    ) {
        overlays.showB {
            PlaylistPickerBox(
                playlists =
                    state.playlists,
                onCreate = {
                    overlays.showB {
                        CreatePlaylistBox(
                            songs =
                                state.songs,
                            initialSong =
                                song,
                            onCreate = {
                                    name,
                                    ids ->

                                viewModel
                                    .createPlaylist(
                                        name,
                                        ids
                                    ) {
                                        playlist ->

                                        overlays.hideB()

                                        if (
                                            playlist !=
                                            null
                                        ) {
                                            overlays.showP(
                                                "Playlist created"
                                            )
                                        }
                                    }
                            }
                        )
                    }
                },
                onChoose = {
                    playlist ->

                    viewModel.addToPlaylist(
                        playlist.id,
                        song
                    ) {
                        updated ->

                        overlays.hideB()

                        if (updated != null) {
                            overlays.showP(
                                "Added to ${updated.name}"
                            )
                        }
                    }
                }
            )
        }
    }

    fun showDelete(
        song: Song
    ) {
        overlays.showB {
            DeleteSongBox(
                song = song,
                onRemoveApp = {
                    playerViewModel
                        .removeFromQueue(
                            song.id
                        )

                    viewModel.hideSong(song)
                    overlays.hideB()

                    overlays.showP(
                        "Removed from XVOX"
                    )
                },
                onDeleteDevice = {
                    overlays.showB {
                        ConfirmDeviceDeleteBox(
                            song = song,
                            onCancel =
                                overlays::hideB,
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
                                                song
                                            )

                                    if (
                                        pending != null
                                    ) {
                                        pendingDelete.value =
                                            song

                                        overlays.hideB()

                                        deleteLauncher.launch(
                                            IntentSenderRequest
                                                .Builder(
                                                    pending
                                                        .intentSender
                                                )
                                                .build()
                                        )
                                    }
                                } else {
                                    val deleted =
                                        XvoxSongActions
                                            .deleteLegacy(
                                                context,
                                                song
                                            )

                                    overlays.hideB()

                                    if (deleted) {
                                        playerViewModel
                                            .removeFromQueue(
                                                song.id
                                            )

                                        viewModel.refresh()

                                        overlays.showP(
                                            "Deleted from device"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    }

    fun showSongOptions(
        song: Song
    ) {
        overlays.showL {
            SongOptionsSheet(
                song = song,
                liked =
                    song.id in
                        state.likedSongIds,
                onPlayNext = {
                    playerViewModel
                        .playNextInQueue(
                            song
                        )

                    overlays.hideL()
                    overlays.showP(
                        "Playing next"
                    )
                },
                onAddQueue = {
                    playerViewModel
                        .addToQueue(song)

                    overlays.hideL()
                    overlays.showP(
                        "Added to queue"
                    )
                },
                onPlaylist = {
                    overlays.hideL()
                    showPlaylistPicker(song)
                },
                onLiked = {
                    val wasLiked =
                        song.id in
                            state.likedSongIds

                    viewModel.toggleLiked(song)
                    overlays.hideL()

                    overlays.showP(
                        if (wasLiked) {
                            "Removed from liked"
                        } else {
                            "Added to liked"
                        }
                    )
                },
                onDelete = {
                    overlays.hideL()
                    showDelete(song)
                },
                onInfo = {
                    overlays.hideL()

                    viewModel.loadInfo(song) {
                        info ->

                        overlays.showB {
                            SongInfoBox(info)
                        }
                    }
                },
                onRingtone = {
                    overlays.hideL()

                    if (
                        XvoxSongActions
                            .canWriteSettings(
                                context
                            )
                    ) {
                        val success =
                            XvoxSongActions
                                .setRingtone(
                                    context,
                                    song
                                )

                        overlays.showP(
                            if (success) {
                                "Ringtone set"
                            } else {
                                "Couldn't set ringtone"
                            }
                        )
                    } else {
                        XvoxSongActions
                            .openWriteSettings(
                                context
                            )

                        overlays.showP(
                            "Allow modify system settings"
                        )
                    }
                },
                onShare = {
                    overlays.hideL()

                    XvoxSongActions.share(
                        context,
                        song
                    )
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = headerSpace
                )
        ) {
            item(
                key = "songs"
            ) {
                AllSongsSection(
                    songs = state.songs,
                    currentSongId =
                        currentSongId,
                    isPlaying =
                        isPlaying,
                    onSongClick = {
                        song ->

                        viewModel
                            .recordPlayedFromLibrary(
                                song,
                                currentSongId
                            )

                        onPlay(song)
                    },
                    onSongLongClick =
                        ::showSongOptions,
                    onPrefetch =
                        viewModel::prefetchFrom
                )
            }

            item(
                key = "recent"
            ) {
                RecentlyPlayedSection(
                    songs =
                        state.recentlyPlayed,
                    currentSongId =
                        currentSongId,
                    isPlaying =
                        isPlaying,
                    transition =
                        state.recentTransition,
                    onSongClick = {
                        song ->

                        viewModel
                            .recordPlayedFromRecent(
                                song,
                                currentSongId
                            )

                        onPlay(song)
                    }
                )
            }

            item(
                key = "footer"
            ) {
                HomeFooter(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .heightIn(
                            min = screenHeight
                        )
                )
            }
        }

        HomeGlassHeader(
            profile = state.profile,
            showPlaylists =
                state.showPlaylists,
            onRefresh =
                viewModel::refresh,
            onHeartClick = {},
            onLibraryModeClick =
                viewModel::toggleLibraryMode
        )
    }
}
