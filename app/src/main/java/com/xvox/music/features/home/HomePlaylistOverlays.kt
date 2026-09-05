package com.xvox.music.features.home

import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.PlaylistInfoBox
import com.xvox.music.features.playlist.XvoxAddPlaylistSongsBox
import com.xvox.music.features.playlist.XvoxPlaylistActionsBox
import com.xvox.music.features.playlist.XvoxPlaylistCoverEditor

fun showPlaylistActions(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    playlist: XvoxPlaylist,
    onDeleted: () -> Unit
) {
    overlays.showL {
        val current =
            viewModel.state.value
                .playlists
                .firstOrNull {
                    it.id == playlist.id
                } ?: playlist

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
                    overlays.hideL()
                    onDeleted()
                    overlays.showP("Playlist deleted")
                }
            },
            onInfo = {
                overlays.showL {
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
    overlays.showL {
        val current =
            viewModel.state.value
                .playlists
                .firstOrNull {
                    it.id == playlist.id
                } ?: playlist

        XvoxPlaylistCoverEditor(
            playlist = current,
            songs = viewModel.playlistSongs(current),
            onCancel = overlays::hideL,
            onApply = { songIds, customUri ->
                viewModel.savePlaylistCover(
                    playlistId = current.id,
                    songIds = songIds,
                    customUri = customUri
                ) { updated ->
                    overlays.hideL()
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
    overlays.showL {
        val current =
            viewModel.state.value
                .playlists
                .firstOrNull {
                    it.id == playlist.id
                } ?: playlist

        XvoxAddPlaylistSongsBox(
            songs = viewModel.state.value.songs,
            existingSongIds = current.songIds.toSet(),
            playlist = current,
            playlistSongs = viewModel.playlistSongs(current),
            onAddMultiple = { selectedSongs ->
                selectedSongs.forEach { s ->
                    viewModel.addToPlaylist(current.id, s) {}
                }
                overlays.hideL()
                overlays.showP("Added ${selectedSongs.size} songs to ${current.name}")
            },
            onCancel = { overlays.hideL() }
        )
    }
}
