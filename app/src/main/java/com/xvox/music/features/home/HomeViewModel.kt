package com.xvox.music.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
) : AndroidViewModel(application) {

    private val repository =
        MediaStoreSongRepository(application)

    private val preferences =
        UserPreferencesRepository(application)

    private val playbackController =
        PlaybackController(application)

    private val _state =
        MutableStateFlow(HomeUiState())

    val state: StateFlow<HomeUiState> =
        _state.asStateFlow()

    init {
        observeProfile()
        refresh()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            preferences.preferences.collect { profile ->
                _state.update {
                    it.copy(profile = profile)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true)
            }

            val songs =
                repository.loadSongs()

            _state.update {
                it.copy(
                    loading = false,
                    songs = songs
                )
            }
        }
    }

    fun play(song: Song) {
        playbackController.play(song)

        _state.update { current ->
            val recent =
                buildList {
                    add(song)

                    addAll(
                        current.recentlyPlayed
                            .filterNot {
                                it.id == song.id
                            }
                    )
                }.take(20)

            current.copy(
                recentlyPlayed = recent
            )
        }
    }

    override fun onCleared() {
        playbackController.release()
        super.onCleared()
    }
}
