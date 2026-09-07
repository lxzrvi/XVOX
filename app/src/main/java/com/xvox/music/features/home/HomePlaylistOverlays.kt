package com.xvox.music.features.home

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.net.Uri
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.PlaylistInfoBox
import com.xvox.music.features.playlist.PlaylistPickerBox
import com.xvox.music.features.playlist.XvoxAddPlaylistSongsBox
import com.xvox.music.features.playlist.XvoxPlaylistActionsBox
import com.xvox.music.features.playlist.XvoxPlaylistCoverEditor

fun showPlaylistActions(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    playlist: XvoxPlaylist,
    onDeleted: () -> Unit
) {
    overlays.showBox("Playlist options") {
        val liveState by viewModel.state.collectAsState()
        val current = liveState.playlists.firstOrNull { it.id == playlist.id } ?: playlist

        XvoxPlaylistActionsBox(
            playlist = current,
            songs = viewModel.playlistSongs(current),
            onRename = { name ->
                viewModel.renamePlaylist(current.id, name) { updated ->
                    if (updated != null) {
                        overlays.showP("Playlist renamed")
                    }
                }
            },
            onSaveCover = { songIds, customUri, done ->
                viewModel.savePlaylistCover(
                    playlistId = current.id,
                    songIds = songIds,
                    customUri = customUri
                ) { updated ->
                    done()
                    if (updated != null) {
                        overlays.showP("Playlist cover updated")
                    }
                }
            },
            onDelete = {
                viewModel.deletePlaylist(current.id) {
                    overlays.hideBox()
                    onDeleted()
                    overlays.showP("Playlist deleted")
                }
            },
            onInfo = {
                overlays.showBox("Playlist info") {
                    PlaylistInfoBox(
                        playlist = current,
                        songCount = viewModel.playlistSongs(current).size
                    )
                }
            }
        )
    }
}

fun showPlaylistCoverEditor(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    playlist: XvoxPlaylist
) {
    overlays.showBox("Edit playlist cover") {
        val liveState by viewModel.state.collectAsState()
        val current = liveState.playlists.firstOrNull { it.id == playlist.id } ?: playlist

        XvoxPlaylistCoverEditor(
            playlist = current,
            songs = viewModel.playlistSongs(current),
            onCancel = overlays::hideBox,
            onApply = { songIds, customUri ->
                viewModel.savePlaylistCover(
                    playlistId = current.id,
                    songIds = songIds,
                    customUri = customUri
                ) { updated ->
                    overlays.hideBox()
                    if (updated != null) {
                        overlays.showP("Playlist cover updated")
                    }
                }
            }
        )
    }
}

fun showAddPlaylistSongs(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    playlist: XvoxPlaylist
) {
    overlays.showBox("Add songs") {
        val liveState by viewModel.state.collectAsState()
        val current = liveState.playlists.firstOrNull { it.id == playlist.id } ?: playlist

        XvoxAddPlaylistSongsBox(
            songs = liveState.songs,
            existingSongIds = current.songIds.toSet(),
            playlist = current,
            playlistSongs = viewModel.playlistSongs(current),
            onAddMultiple = { selectedSongs ->
                viewModel.addMultipleToPlaylist(current.id, selectedSongs) {
                    overlays.hideBox()
                    overlays.showP("Added ${selectedSongs.size} songs to ${current.name}")
                }
            },
            onCancel = { overlays.hideBox() }
        )
    }
}

fun showMultiAddToPlaylistOverlay(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    songs: List<Song>,
    onDone: () -> Unit
) {
    overlays.showBox("Selected songs · Playlist") {
        val liveState by viewModel.state.collectAsState()
        val playlists = liveState.playlists
        PlaylistPickerBox(
            song = songs.firstOrNull() ?: Song(0L, "", "", Uri.EMPTY, null),
            playlists = playlists,
            onCreate = {
                showCreatePlaylistOverlay(overlays, viewModel, liveState.songs)
            },
            onAdd = { pl ->
                viewModel.addMultipleToPlaylist(pl.id, songs) {
                    overlays.hideBox()
                    overlays.showP("${songs.size} songs added to ${pl.name}")
                    onDone()
                }
            },
            onRemove = { pl ->
                viewModel.removeMultipleFromPlaylist(pl.id, songs) {
                    overlays.hideBox()
                    overlays.showP("${songs.size} songs removed from ${pl.name}")
                    onDone()
                }
            },
            songs = liveState.songs
        )
    }
}
