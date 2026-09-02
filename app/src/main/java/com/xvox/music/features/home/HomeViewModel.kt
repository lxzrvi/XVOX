package com.xvox.music.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.artwork.ArtworkPreloader
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.media.MediaStoreSongRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val songRepository =
        MediaStoreSongRepository(application)

    private val preferencesRepository =
        UserPreferencesRepository(application)

    private val artworkPreloader =
        ArtworkPreloader(application)

    private val _state =
        MutableStateFlow(
            HomeUiState()
        )

    val state: StateFlow<HomeUiState> =
        _state.asStateFlow()

    private var recentIds:
        List<Long> = emptyList()

    private var prefetchJob:
        Job? = null

    private var lastPrefetchStart =
        -1

    private var transitionId =
        0L

    init {
        observeProfile()
        observeRecent()
        loadLibrary()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            preferencesRepository
                .preferences
                .collect { profile ->
                    _state.update {
                        it.copy(
                            profile = profile
                        )
                    }
                }
        }
    }

    private fun observeRecent() {
        viewModelScope.launch {
            preferencesRepository
                .recentSongIds
                .collect { ids ->
                    recentIds = ids

                    _state.update {
                        current ->

                        current.copy(
                            recentlyPlayed =
                                resolveRecent(
                                    songs =
                                        current.songs,
                                    ids = ids
                                )
                        )
                    }
                }
        }
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            val songs =
                songRepository
                    .loadSongs()

            _state.update {
                it.copy(
                    songs = songs,
                    recentlyPlayed =
                        resolveRecent(
                            songs,
                            recentIds
                        ),
                    loading = false
                )
            }

            prefetchFrom(0)
        }
    }

    fun refresh() {
        if (
            _state.value.refreshing
        ) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    refreshing = true
                )
            }

            val songs =
                songRepository
                    .loadSongs()

            prefetchJob?.cancel()
            lastPrefetchStart = -1

            _state.update {
                it.copy(
                    songs = songs,
                    recentlyPlayed =
                        resolveRecent(
                            songs,
                            recentIds
                        ),
                    refreshing = false,
                    loading = false
                )
            }

            prefetchFrom(0)
        }
    }

    fun recordPlayedFromLibrary(
        song: Song,
        currentSongId: Long?
    ) {
        /*
         * Current song:
         * HomeScreen still sends playback click,
         * but Recent remains completely untouched.
         */
        if (
            song.id ==
            currentSongId
        ) {
            return
        }

        promoteRecent(
            song = song,
            transitionMode =
                RecentTransitionMode
                    .FRONT_REPLACE
        )
    }

    fun recordPlayedFromRecent(
        song: Song,
        currentSongId: Long?,
        transitionMode:
            RecentTransitionMode
    ) {
        /*
         * Current playback song:
         * toggle only.
         *
         * No reorder.
         * No Recent transition.
         */
        if (
            song.id ==
            currentSongId
        ) {
            return
        }

        promoteRecent(
            song = song,
            transitionMode =
                transitionMode
        )
    }

    private fun promoteRecent(
        song: Song,
        transitionMode:
            RecentTransitionMode
    ) {
        transitionId++

        _state.update {
            current ->

            val reordered =
                buildList {
                    add(song)

                    addAll(
                        current
                            .recentlyPlayed
                            .filterNot {
                                it.id ==
                                    song.id
                            }
                    )
                }
                    .take(20)

            current.copy(
                recentlyPlayed =
                    reordered,
                recentTransition =
                    RecentTransitionRequest(
                        id =
                            transitionId,
                        songId =
                            song.id,
                        mode =
                            transitionMode
                    )
            )
        }

        viewModelScope.launch {
            preferencesRepository
                .recordRecentSong(
                    song.id
                )
        }
    }

    fun prefetchFrom(
        sourceIndex: Int
    ) {
        val songs =
            _state.value.songs

        if (
            songs.isEmpty()
        ) {
            return
        }

        val start =
            (sourceIndex / 12) *
                12

        if (
            start ==
            lastPrefetchStart
        ) {
            return
        }

        lastPrefetchStart =
            start

        prefetchJob?.cancel()

        prefetchJob =
            viewModelScope.launch {
                artworkPreloader.warm(
                    songs = songs,
                    fromIndex = start,
                    count = 36
                )
            }
    }

    fun toggleLibraryMode() {
        _state.update {
            it.copy(
                showPlaylists =
                    !it.showPlaylists
            )
        }
    }

    private fun resolveRecent(
        songs: List<Song>,
        ids: List<Long>
    ): List<Song> {
        if (
            songs.isEmpty() ||
            ids.isEmpty()
        ) {
            return emptyList()
        }

        val byId =
            songs.associateBy {
                it.id
            }

        return ids.mapNotNull {
            byId[it]
        }
    }

    override fun onCleared() {
        prefetchJob?.cancel()
        super.onCleared()
    }
}
