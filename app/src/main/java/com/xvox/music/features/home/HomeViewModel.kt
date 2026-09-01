package com.xvox.music.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.artwork.ArtworkPreloader
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.media.MediaStoreSongRepository
import com.xvox.music.player.playback.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(
    application
) {

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

    init {
        observeProfile()
        initialLoad()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            preferencesRepository
                .preferences
                .collect { profile ->
                    _state.update {
                        it.copy(
                            profile =
                                profile
                        )
                    }
                }
        }
    }

    private fun initialLoad() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true
                )
            }

            val songs =
                songRepository
                    .loadSongs()

            artworkPreloader
                .warmInitialCache(
                    songs
                )

            _state.update {
                it.copy(
                    songs = songs,
                    loading = false
                )
            }
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

            _state.update {
                it.copy(
                    songs = songs,
                    refreshing = false
                )
            }
        }
    }

    fun play(song: Song) {
        playback.play(song)

        _state.update {
            current ->

            val recent =
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

            current.copy(
                recentlyPlayed =
                    recent
            )
        }
    }

    override fun onCleared() {
        playback.release()
        super.onCleared()
    }
}
