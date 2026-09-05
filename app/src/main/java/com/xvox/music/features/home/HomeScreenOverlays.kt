package com.xvox.music.features.home

import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.xvox.music.core.model.Song
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
    overlays.showL {
        CreatePlaylistBox(
            songs = songs,
            initialSong = initialSong,
            onCreate = { name, ids ->
                viewModel.createPlaylist(name, ids) { playlist ->
                    overlays.hideL()
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
    overlays.showL {
        PlaylistPickerBox(
            song = song,
            playlists = playlists,
            onCreate = { showCreatePlaylistOverlay(overlays, viewModel, songs, song) },
            onAdd = { playlist ->
                viewModel.addToPlaylist(playlist.id, song) { updated ->
                    if (updated != null) {
                        overlays.hideL()
                        overlays.showP("Added to ${updated.name}")
                    }
                }
            },
            onRemove = { playlist ->
                viewModel.removeFromPlaylist(playlist.id, song) { updated ->
                    if (updated != null) {
                        overlays.hideL()
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
    onPendingDelete: (Song?) -> Unit
) {
    overlays.showL {
        DeleteSongBox(
            song = song,
            onRemoveApp = {
                playerViewModel.removeFromQueue(song.id)
                viewModel.hideSong(song)
                overlays.hideL()
                overlays.showP("Removed from XVOX")
            },
            onDeleteDevice = {
                overlays.showL {
                    ConfirmDeviceDeleteBox(
                        song = song,
                        onCancel = overlays::hideL,
                        onDelete = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val pending = XvoxSongActions.deletePendingIntent(context, song)
                                if (pending != null) {
                                    onPendingDelete(song)
                                    overlays.hideL()
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(pending.intentSender).build()
                                    )
                                }
                            } else {
                                val deleted = XvoxSongActions.deleteLegacy(context, song)
                                overlays.hideL()
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

fun showSongOptionsOverlay(
    overlays: XvoxOverlayController,
    context: Context,
    song: Song,
    isLiked: Boolean,
    playlist: XvoxPlaylist? = null,
    recent: Boolean = false,
    viewModel: HomeViewModel,
    playerViewModel: MainPlayerViewModel,
    playlists: List<XvoxPlaylist>,
    songs: List<Song>,
    deleteLauncher: ActivityResultLauncher<IntentSenderRequest>,
    onPendingDelete: (Song?) -> Unit
) {
    overlays.showL {
        SongOptionsSheet(
            song = song,
            liked = isLiked,
            playlistName = playlist?.name,
            onPlayNext = {
                playerViewModel.playNextInQueue(song)
                overlays.hideL()
                overlays.showP("Playing next")
            },
            onAddQueue = {
                playerViewModel.addToQueue(song)
                overlays.hideL()
                overlays.showP("Added to queue")
            },
            onPlaylist = {
                overlays.hideL()
                showPlaylistPickerOverlay(overlays, viewModel, song, playlists, songs)
            },
            onRemovePlaylist = playlist?.let { target ->
                {
                    viewModel.removeFromPlaylist(target.id, song) {
                        overlays.hideL()
                        overlays.showP("Removed from ${target.name}")
                    }
                }
            },
            onRemoveRecent = if (recent) {
                {
                    viewModel.removeFromRecent(song)
                    overlays.hideL()
                    overlays.showP("Removed from recent")
                }
            } else null,
            onLiked = {
                viewModel.toggleLiked(song)
                overlays.hideL()
                overlays.showP(if (isLiked) "Removed from liked" else "Added to liked")
            },
            onDelete = {
                overlays.hideL()
                showDeleteOverlay(overlays, context, song, playerViewModel, viewModel, deleteLauncher, onPendingDelete)
            },
            onInfo = {
                overlays.hideL()
                viewModel.loadInfo(song) { info ->
                    overlays.showL { SongInfoBox(info) }
                }
            },
            onRingtone = {
                overlays.hideL()
                if (XvoxSongActions.canWriteSettings(context)) {
                    val success = XvoxSongActions.setRingtone(context, song)
                    overlays.showP(if (success) "Ringtone set" else "Couldn't set ringtone")
                } else {
                    XvoxSongActions.openWriteSettings(context)
                    overlays.showP("Allow modify system settings")
                }
            },
            onShare = {
                overlays.hideL()
                XvoxSongActions.share(context, song)
            }
        )
    }
}
