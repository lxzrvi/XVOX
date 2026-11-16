package com.xvox.music.features.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.net.Uri
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.overlay.XvoxOverlayController
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.settings.components.XvoxTransactionalFooterActions
import com.xvox.music.features.playlist.PlaylistInfoBox
import com.xvox.music.features.playlist.PlaylistPickerBox
import com.xvox.music.features.playlist.XvoxAddPlaylistSongsBox
import com.xvox.music.features.playlist.XvoxPlaylistActionsBox
import com.xvox.music.features.playlist.XvoxPlaylistCoverEditor

fun showPlaylistActions(
    overlays: XvoxOverlayController,
    viewModel: HomeViewModel,
    playlist: XvoxPlaylist,
    onSelect: (() -> Unit)? = null,
    onDeleted: () -> Unit,
    selectTitle: String = "Select songs"
) {
    fun showSettings() {
        overlays.showBox("Playlist layout") {
            com.xvox.music.features.playlist.PlaylistLayoutEditorBox(
                onDone = {
                    showPlaylistActions(overlays, viewModel, playlist, onSelect, onDeleted, selectTitle)
                }
            )
        }
    }

    // Held-item menu: the playlist lifts off the blurred page and its actions hang beneath it.
    overlays.showBox(
        title = "Playlist options",
        presentation = com.xvox.music.core.ui.overlay.XvoxBoxPresentation.HELD
    ) {
        val liveState by viewModel.state.collectAsState()
        val likedPlaylistIds by viewModel.likedPlaylistIds.collectAsState()
        val current = liveState.playlists.firstOrNull { it.id == playlist.id } ?: playlist
        val songs = viewModel.playlistSongs(current)
        val liked = current.id in likedPlaylistIds

        val actions = mutableListOf<XvoxHeldAction>()
        if (onSelect != null) {
            actions += XvoxHeldAction("$selectTitle (${songs.size})", com.xvox.music.R.drawable.ic_xvox_check) {
                overlays.hideBox()
                onSelect()
            }
        }
        actions += XvoxHeldAction("Rename", com.xvox.music.R.drawable.ic_xvox_edit) {
            overlays.showBox("Rename playlist") {
                var name by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(
                        androidx.compose.ui.text.input.TextFieldValue(
                            current.name,
                            androidx.compose.ui.text.TextRange(current.name.length)
                        )
                    )
                }
                val colors = com.xvox.music.core.design.theme.XvoxTheme.colors
                val focus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
                androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
                androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = colors.primaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primaryText),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
                    XvoxTransactionalFooterActions(
                        onCancel = overlays::hideBox,
                        onOkay = {
                            val clean = name.text.trim()
                            if (clean.isNotEmpty()) {
                                viewModel.renamePlaylist(current.id, clean) { updated ->
                                    overlays.hideBox()
                                    if (updated != null) overlays.showP("Playlist renamed")
                                }
                            }
                        }
                    )
                }
            }
        }
        actions += XvoxHeldAction("Edit cover", com.xvox.music.R.drawable.ic_xvox_playlist) {
            showPlaylistCoverEditor(overlays, viewModel, current)
        }
        actions += XvoxHeldAction(
            if (liked) "Remove from liked" else "Like playlist",
            if (liked) com.xvox.music.R.drawable.ic_xvox_heart else com.xvox.music.R.drawable.ic_xvox_heart_outline
        ) { viewModel.toggleLikedPlaylist(current.id) }
        actions += XvoxHeldAction("Playlist info", com.xvox.music.R.drawable.ic_xvox_info) {
            overlays.showBox(
                title = "Playlist info",
                bottomAction = {
                    XvoxTransactionalFooterActions(
                        onCancel = overlays::hideBox,
                        onOkay = overlays::hideBox
                    )
                }
            ) {
                PlaylistInfoBox(playlist = current, songCount = songs.size)
            }
        }
        actions += XvoxHeldAction("Layout settings", com.xvox.music.R.drawable.ic_xvox_settings) { showSettings() }
        actions += XvoxHeldAction("Delete playlist", com.xvox.music.R.drawable.ic_xvox_delete, destructive = true) {
            overlays.showBox("Delete playlist?") {
                com.xvox.music.shell.XvoxConfirmBox(
                    question = "Delete \"${current.name}\"?",
                    detail = "The songs themselves stay in your library.",
                    confirmLabel = "Delete",
                    danger = true,
                    onCancel = overlays::hideBox,
                    onConfirm = {
                        viewModel.deletePlaylist(current.id) {
                            overlays.hideBox()
                            onDeleted()
                            overlays.showP("Playlist deleted")
                        }
                    }
                )
            }
        }

        XvoxHeldActionsBox(
            title = current.name,
            subtitle = "${songs.size} songs",
            cover = { m ->
                com.xvox.music.features.playlist.XvoxPlaylistCover(
                    songs = songs,
                    coverSongIds = current.coverSongIds,
                    customCoverUri = current.customCoverUri,
                    requestSize = 384,
                    coverShape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
                    modifier = m
                )
            },
            actions = actions
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
            song = null,
            selectedSongs = songs,
            playlists = playlists,
            onCreate = {
                showCreatePlaylistOverlay(
                    overlays = overlays,
                    viewModel = viewModel,
                    songs = liveState.songs,
                    initialSongIds = songs.map { it.id }.toSet(),
                    onCreated = onDone
                )
            },
            onAdd = { pl ->
                viewModel.addMultipleToPlaylist(pl.id, songs) {
                    overlays.hideBox()
                    overlays.showP("${songs.size} songs added to ${pl.name}")
                    onDone()
                }
            },
            onAddCustomList = { pl, listToAdd ->
                viewModel.addMultipleToPlaylist(pl.id, listToAdd) {
                    overlays.hideBox()
                    overlays.showP("${listToAdd.size} songs added to ${pl.name}")
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
