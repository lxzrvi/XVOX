package com.xvox.music.features.home

import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.library.AddPlaylistSongsBox
import com.xvox.music.features.home.library.PlaylistActionsBox
import com.xvox.music.features.home.library.PlaylistInfoBox

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

        PlaylistActionsBox(
            playlist = current,
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

        AddPlaylistSongsBox(
            songs =
                viewModel.state.value
                    .songs,
            existingSongIds =
                current.songIds
                    .toSet(),
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
