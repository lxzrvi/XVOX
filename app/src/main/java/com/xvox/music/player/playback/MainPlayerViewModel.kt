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

    private val controller =
        PlaybackController(
            application
        )

    private val _state =
        MutableStateFlow(
            MainPlayerUiState()
        )

    val state:
        StateFlow<MainPlayerUiState> =
        _state.asStateFlow()

    init {
        viewModelScope.launch {
            controller.state
                .collect {
                    playback ->

                    _state.update {
                        it.copy(
                            connected =
                                playback.connected,
                            currentSongId =
                                playback.currentSongId,
                            currentIndex =
                                playback.currentIndex,
                            isPlaying =
                                playback.isPlaying,
                            position =
                                playback.position,
                            duration =
                                playback.duration
                        )
                    }
                }
        }
    }

    fun setQueue(
        songs: List<Song>
    ) {
        if (
            _state.value.queue ===
            songs
        ) {
            return
        }

        controller.setQueue(
            songs
        )

        _state.update {
            it.copy(
                queue = songs
            )
        }
    }

    fun play(
        song: Song
    ) {
        val needsEntrance =
            !_state.value
                .miniPlayerVisible

        controller.play(
            song
        )

        _state.update {
            current ->

            current.copy(
                miniPlayerVisible =
                    !current
                        .nowPlayingVisible,

                miniPlayerRiseKey =
                    if (
                        needsEntrance &&
                        !current
                            .nowPlayingVisible
                    ) {
                        current
                            .miniPlayerRiseKey +
                            1
                    } else {
                        current
                            .miniPlayerRiseKey
                    }
            )
        }
    }

    fun playQueueIndex(
        index: Int
    ) {
        controller.playQueueIndex(
            index = index,
            keepPlayingState = true
        )
    }

    fun playPrevious() {
        controller.playPrevious()
    }

    fun playNext() {
        controller.playNext()
    }

    fun seekTo(
        positionMs: Long
    ) {
        controller.seekTo(
            positionMs
        )
    }

    fun togglePlay() {
        controller.togglePlay()
    }

    fun openNowPlaying() {
        if (
            _state.value
                .currentSongId ==
            null
        ) {
            return
        }

        _state.update {
            it.copy(
                nowPlayingVisible = true,
                miniPlayerVisible = false
            )
        }
    }

    fun closeNowPlaying() {
        val hasSong =
            _state.value
                .currentSongId != null

        _state.update {
            current ->

            current.copy(
                nowPlayingVisible = false,
                miniPlayerVisible =
                    hasSong,
                miniPlayerRiseKey =
                    if (hasSong) {
                        current
                            .miniPlayerRiseKey +
                            1
                    } else {
                        current
                            .miniPlayerRiseKey
                    }
            )
        }
    }

    fun hideMiniPlayer() {
        _state.update {
            it.copy(
                miniPlayerVisible = false
            )
        }
    }

    fun stopPlayback() {
        controller.stop()

        _state.update {
            it.copy(
                miniPlayerVisible = false,
                nowPlayingVisible = false
            )
        }
    }

    override fun onCleared() {
        controller.release()
        super.onCleared()
    }
}
