package com.xvox.music.features.home

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.artwork.ArtworkPreloader
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.media.MediaStoreSongRepository
import com.xvox.music.player.playback.PlaybackController
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
        MediaStoreSongRepository(
            application
        )

    private val preferencesRepository =
        UserPreferencesRepository(
            application
        )

    private val artworkPreloader =
        ArtworkPreloader(
            application
        )

    private val playback =
        PlaybackController(
            application
        )

    private val _state =
        MutableStateFlow(
            HomeUiState()
        )

    val state:
        StateFlow<HomeUiState> =
        _state.asStateFlow()

    private var prefetchJob: Job? =
        null

    private var lastPrefetchStart =
        -1

    private var lastTapAt = 0L
    private var lastTappedSong = -1L

    init {
        observeProfile()
        observePlayback()
        initialLoad()
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

    private fun observePlayback() {
        viewModelScope.launch {
            playback.state.collect {
                playbackState ->

                _state.update {
                    it.copy(
                        currentSongId =
                            playbackState
                                .currentSongId,
                        currentIndex =
                            playbackState
                                .currentIndex,
                        isPlaying =
                            playbackState
                                .isPlaying,
                        playbackPosition =
                            playbackState
                                .position,
                        playbackDuration =
                            playbackState
                                .duration
                    )
                }
            }
        }
    }

    private fun initialLoad() {
        viewModelScope.launch {
            val songs =
                songRepository.loadSongs()

            playback.setQueue(songs)

            artworkPreloader.warm(
                songs = songs,
                fromIndex = 0,
                count = 12
            )

            _state.update {
                it.copy(
                    songs = songs,
                    loading = false
                )
            }

            prefetchFrom(12)
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
                songRepository.loadSongs()

            playback.setQueue(songs)

            lastPrefetchStart = -1

            _state.update {
                it.copy(
                    songs = songs,
                    refreshing = false
                )
            }

            prefetchFrom(0)
        }
    }

    fun prefetchFrom(
        sourceIndex: Int
    ) {
        val songs =
            _state.value.songs

        if (songs.isEmpty()) {
            return
        }

        val start =
            (
                sourceIndex /
                    12
                ) * 12

        if (
            start ==
            lastPrefetchStart
        ) {
            return
        }

        lastPrefetchStart = start

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

    fun play(
        song: Song
    ) {
        val now =
            SystemClock
                .elapsedRealtime()

        if (
            song.id ==
            lastTappedSong &&
            now - lastTapAt <
            180L
        ) {
            return
        }

        lastTappedSong = song.id
        lastTapAt = now

        playback.play(song)

        addRecent(song)

        _state.update {
            it.copy(
                miniPlayerVisible = true
            )
        }
    }

    fun playQueueIndex(
        index: Int
    ) {
        val song =
            _state.value.songs
                .getOrNull(index)
                ?: return

        playback.playQueueIndex(index)

        addRecent(song)

        _state.update {
            it.copy(
                miniPlayerVisible = true
            )
        }
    }

    private fun addRecent(
        song: Song
    ) {
        _state.update {
            current ->

            current.copy(
                recentlyPlayed =
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
                    }.take(20)
            )
        }
    }

    fun togglePlay() {
        playback.togglePlay()
    }

    fun hideMiniPlayer() {
        _state.update {
            it.copy(
                miniPlayerVisible = false
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

    override fun onCleared() {
        prefetchJob?.cancel()
        playback.release()
        super.onCleared()
    }
}
