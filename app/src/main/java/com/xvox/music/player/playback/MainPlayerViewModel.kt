package com.xvox.music.player.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainPlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val playback =
        PlaybackController(application)

    private val _state =
        MutableStateFlow(
            MainPlayerUiState()
        )

    val state:
        StateFlow<MainPlayerUiState> =
        _state.asStateFlow()

    init {
        viewModelScope.launch {
            playback.state.collect {
                playbackState ->

                _state.update {
                    current ->

                    current.copy(
                        currentSongId =
                            playbackState
                                .currentSongId,
                        currentIndex =
                            playbackState
                                .currentIndex,
                        isPlaying =
                            playbackState
                                .isPlaying,
                        position =
                            playbackState
                                .position,
                        duration =
                            playbackState
                                .duration
                    )
                }
            }
        }
    }

    fun setQueue(
        songs: List<Song>
    ) {
        playback.setQueue(songs)

        _state.update {
            it.copy(
                queue = songs
            )
        }
    }

    fun play(
        song: Song
    ) {
        playback.play(song)

        _state.update {
            current ->

            current.copy(
                miniPlayerVisible = true,
                miniPlayerGeneration =
                    if (
                        current.currentSongId ==
                        null
                    ) {
                        current
                            .miniPlayerGeneration +
                            1
                    } else {
                        current
                            .miniPlayerGeneration
                    }
            )
        }
    }

    fun playQueueIndex(
        index: Int
    ) {
        playback.playQueueIndex(
            index = index,
            preservePlayingState = true
        )

        _state.update {
            it.copy(
                miniPlayerVisible = true
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

    fun showMiniPlayer() {
        if (
            _state.value.currentSongId !=
            null
        ) {
            _state.update {
                it.copy(
                    miniPlayerVisible = true
                )
            }
        }
    }

    override fun onCleared() {
        playback.release()
        super.onCleared()
    }
}
