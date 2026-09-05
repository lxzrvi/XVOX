package com.xvox.music.features.home

import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.playlist.XvoxAddPlaylistSongsBox
import com.xvox.music.features.playlist.XvoxPlaylistActionsBox
import com.xvox.music.features.playlist.XvoxPlaylistCoverEditor
import com.xvox.music.features.playlist.PlaylistInfoBox

fun showPlaylistActions(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    playlist: XvoxPlaylist,
    onDeleted: () -> Unit
) {
    overlays.showB {
        val current =
            viewModel.state.value
                .playlists
                .firstOrNull {
                    it.id ==
                        playlist.id
                }
                ?: playlist

        XvoxPlaylistActionsBox(
            playlist =
                current,
            songs =
                viewModel
                    .playlistSongs(
                        current
                    ),
            onRename = {
                name ->

                viewModel
                    .renamePlaylist(
                        current.id,
                        name
                    ) {
                        updated ->

                        if (
                            updated != null
                        ) {
                            overlays.showP(
                                "Playlist renamed"
                            )
                        }
                    }
            },
            onSaveCover = {
                    songIds,
                    customUri,
                    done ->

                viewModel
                    .savePlaylistCover(
                        playlistId =
                            current.id,
                        songIds =
                            songIds,
                        customUri =
                            customUri
                    ) {
                        updated ->

                        done()

                        if (
                            updated != null
                        ) {
                            overlays.showP(
                                "Playlist cover updated"
                            )
                        }
                    }
            },
            onDelete = {
                viewModel
                    .deletePlaylist(
                        current.id
                    ) {
                        overlays.hideB()
                        onDeleted()

                        overlays.showP(
                            "Playlist deleted"
                        )
                    }
            },
            onInfo = {
                overlays.showB {
                    PlaylistInfoBox(
                        playlist =
                            current,
                        songCount =
                            viewModel
                                .playlistSongs(
                                    current
                                )
                                .size
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
    overlays.showB {
        val current =
            viewModel.state.value
                .playlists
                .firstOrNull {
                    it.id == playlist.id
                } ?: playlist

        XvoxPlaylistCoverEditor(
            playlist = current,
            songs = viewModel.playlistSongs(current),
            onCancel = overlays::hideB,
            onApply = { songIds, customUri ->
                viewModel.savePlaylistCover(
                    playlistId = current.id,
                    songIds = songIds,
                    customUri = customUri
                ) { updated ->
                    overlays.hideB()
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
    overlays.showB {
        val current =
            viewModel.state.value
                .playlists
                .firstOrNull {
                    it.id ==
                        playlist.id
                }
                ?: playlist

        XvoxAddPlaylistSongsBox(
            songs =
                viewModel
                    .state
                    .value
                    .songs,
            existingSongIds =
                current.songIds
                    .toSet(),
            playlist = current,
            playlistSongs = viewModel.playlistSongs(current),
            onAdd = {
                song: Song ->

                viewModel
                    .addToPlaylist(
                        current.id,
                        song
                    ) {
                        updated ->

                        if (
                            updated != null
                        ) {
                            overlays.hideB()

                            overlays.showP(
                                "Added to ${updated.name}"
                            )
                        }
                    }
            }
        )
    }
}
