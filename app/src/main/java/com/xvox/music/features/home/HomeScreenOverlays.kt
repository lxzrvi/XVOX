package com.xvox.music.features.home

import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxBoxPresentation
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.CreatePlaylistBox
import com.xvox.music.features.playlist.PlaylistPickerBox
import com.xvox.music.player.playback.MainPlayerViewModel

fun showCreatePlaylistOverlay(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    songs: List<Song>,
    initialSong: Song? = null
) {
    overlays.showBox("Create playlist") {
        CreatePlaylistBox(
            songs = songs,
            initialSong = initialSong,
            onCreate = { name, ids ->
                viewModel.createPlaylist(name, ids) { playlist ->
                    overlays.hideBox()
                    if (playlist != null) {
                        overlays.showP("Playlist created")
                    }
                }
            }
        )
    }
}

fun showPlaylistPickerOverlay(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    song: Song,
    playlists: List<XvoxPlaylist>,
    songs: List<Song>
) {
    overlays.showBox("Add / Remove from Playlist") {
        PlaylistPickerBox(
            song = song,
            playlists = playlists,
            onCreate = { showCreatePlaylistOverlay(overlays, viewModel, songs, song) },
            onAdd = { playlist ->
                viewModel.addToPlaylist(playlist.id, song) { updated ->
                    if (updated != null) {
                        overlays.hideBox()
                        overlays.showP("Added to ${updated.name}")
                    }
                }
            },
            onRemove = { playlist ->
                viewModel.removeFromPlaylist(playlist.id, song) { updated ->
                    if (updated != null) {
                        overlays.hideBox()
                        overlays.showP("Removed from ${updated.name}")
                    }
                }
            },
            songs = songs,
            songsFor = viewModel::playlistSongs
        )
    }
}

fun showDeleteOverlay(
    overlays: XvoxOverlayController,
    context: Context,
    song: Song,
    playerViewModel: MainPlayerViewModel,
    viewModel: HomeViewModel,
    deleteLauncher: ActivityResultLauncher<IntentSenderRequest>,
    onPendingDelete: (Song) -> Unit
) {
    overlays.showBox("Remove song") {
        DeleteSongBox(
            song = song,
            onRemoveApp = {
                // Same confirmation shape as leaving the app.
                overlays.showBox("Delete from XVOX?") {
                    com.xvox.music.shell.XvoxConfirmBox(
                        question = "Delete \"${song.title}\" from XVOX?",
                        detail = "It moves to Settings › Deleted songs. The file stays on your device.",
                        confirmLabel = "Delete",
                        onConfirm = {
                            playerViewModel.removeFromQueue(song.id)
                            viewModel.hideSong(song)
                            overlays.hideBox()
                            overlays.showP("Moved to Deleted songs")
                        },
                        onCancel = overlays::hideBox
                    )
                }
            },
            onDeleteDevice = {
                overlays.showBox("Delete from device?") {
                    com.xvox.music.shell.XvoxConfirmBox(
                        question = "Permanently delete \"${song.title}\"?",
                        detail = "The file is removed from storage. This cannot be undone.",
                        confirmLabel = "Delete",
                        danger = true,
                        onCancel = overlays::hideBox,
                        onConfirm = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val pending = XvoxSongActions.deletePendingIntent(context, song)
                                if (pending != null) {
                                    onPendingDelete(song)
                                    overlays.hideBox()
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(pending.intentSender).build()
                                    )
                                }
                            } else {
                                val deleted = XvoxSongActions.deleteLegacy(context, song)
                                overlays.hideBox()
                                if (deleted) {
                                    playerViewModel.removeFromQueue(song.id)
                                    viewModel.refresh()
                                    overlays.showP("Deleted from device")
                                }
                            }
                        }
                    )
                }
            }
        )
    }
}

fun showAddToQueueOverlay(
    overlays: XvoxOverlayController,
    playerViewModel: MainPlayerViewModel,
    song: Song
) {
    val liveState = playerViewModel.state.value
    overlays.showBox("Add to queue") {
        AddToQueueBox(
            activeQueueName = liveState.activeQueueName,
            savedQueues = liveState.savedQueues,
            onAddToCurrent = {
                val msg = playerViewModel.addToQueue(song)
                overlays.hideBox()
                overlays.showP(msg)
            },
            onAddToSaved = { saved ->
                val msg = playerViewModel.addToSavedQueue(saved.id, song)
                overlays.hideBox()
                overlays.showP(msg)
            },
            onAddToNew = {
                val msg = playerViewModel.addToNewQueue(song)
                overlays.hideBox()
                overlays.showP(msg)
            }
        )
    }
}

fun showMultiAddToQueueOverlay(
    overlays: XvoxOverlayController,
    playerViewModel: MainPlayerViewModel,
    songs: List<Song>,
    onDone: () -> Unit = {}
) {
    val liveState = playerViewModel.state.value
    overlays.showBox("Add to queue") {
        AddToQueueBox(
            activeQueueName = liveState.activeQueueName,
            savedQueues = liveState.savedQueues,
            songCount = songs.size,
            onAddToCurrent = {
                val msg = playerViewModel.addToQueue(songs)
                overlays.hideBox()
                overlays.showP(msg)
                onDone()
            },
            onAddToSaved = { saved ->
                val msg = playerViewModel.addToSavedQueue(saved.id, songs)
                overlays.hideBox()
                overlays.showP(msg)
                onDone()
            },
            onAddToNew = {
                val msg = playerViewModel.addToNewQueue(songs)
                overlays.hideBox()
                overlays.showP(msg)
                onDone()
            }
        )
    }
}

fun showSongOptionsOverlay(
    overlays: XvoxOverlayController,
    context: Context,
    song: Song,
    isLiked: Boolean,
    playlist: XvoxPlaylist? = null,
    playlistMembership: List<XvoxPlaylist> = emptyList(),
    recent: Boolean = false,
    viewModel: HomeViewModel,
    playerViewModel: MainPlayerViewModel,
    playlists: List<XvoxPlaylist>,
    songs: List<Song>,
    deleteLauncher: ActivityResultLauncher<IntentSenderRequest>,
    onPendingDelete: (Song) -> Unit,
    onSelect: (() -> Unit)? = null,
    sectionSettingsLabel: String? = null,
    onSectionSettings: (() -> Unit)? = null
) {
    val actualSource = when {
        recent -> "Recently Played"
        playlist != null -> playlist.name
        song.source.isNotBlank() -> song.source
        else -> "All Songs"
    }
    val sourcedSong = song.copy(source = actualSource)

    overlays.showBox(
        title = "Song Options",
        onSettings = onSectionSettings?.let { act -> { act() } },
        presentation = XvoxBoxPresentation.SONG_OPTIONS
    ) {
        SongOptionsBox(
            song = sourcedSong,
            liked = isLiked,
            onSelect = onSelect?.let { select -> { overlays.hideBox(); select() } },
            onPlayNext = {
                val msg = playerViewModel.playNextInQueue(sourcedSong)
                overlays.hideBox()
                overlays.showP(msg)
            },
            onAddQueue = {
                val msg = playerViewModel.addToQueue(sourcedSong)
                overlays.hideBox()
                overlays.showP(msg)
            },
            onPlaylist = {
                overlays.hideBox()
                showPlaylistPickerOverlay(overlays, viewModel, sourcedSong, playlists, songs)
            },
            playlistName = playlist?.name,
            onRemovePlaylist = playlist?.let { activePlaylist ->
                {
                    viewModel.removeFromPlaylist(activePlaylist.id, sourcedSong) { updated ->
                        if (updated != null) {
                            overlays.hideBox()
                            overlays.showP("Removed from ${updated.name}")
                        }
                    }
                }
            },
            onLiked = {
                viewModel.toggleLiked(sourcedSong)
                overlays.hideBox()
                overlays.showP(if (isLiked) "Removed from liked" else "Added to liked")
            },
            onRemoveRecent = if (recent) {
                {
                    overlays.hideBox()
                    viewModel.removeFromRecent(sourcedSong)
                    overlays.showP("Removed from Recently Played")
                }
            } else null,
            onDelete = {
                overlays.hideBox()
                showDeleteOverlay(overlays, context, sourcedSong, playerViewModel, viewModel, deleteLauncher, onPendingDelete)
            },
            onInfo = {
                overlays.hideBox()
                viewModel.loadInfo(sourcedSong) { info ->
                    overlays.showBox("Song info") { SongInfoBox(info) }
                }
            },
            onRingtone = {
                overlays.hideBox()
                if (XvoxSongActions.canWriteSettings(context)) {
                    val success = XvoxSongActions.setRingtone(context, sourcedSong)
                    overlays.showP(if (success) "Ringtone set" else "Failed to set ringtone")
                } else {
                    XvoxSongActions.openWriteSettings(context)
                    overlays.showP("Allow modify system settings")
                }
            },
            onShare = {
                overlays.hideBox()
                XvoxSongActions.share(context, sourcedSong)
            }
        )
    }
}
