package com.xvox.music.player.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainPlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val controller =
        PlaybackController(application)

    private val preferences =
        UserPreferencesRepository(application)

    private val _state =
        MutableStateFlow(
            MainPlayerUiState()
        )

    val state:
        StateFlow<MainPlayerUiState> =
        _state.asStateFlow()

    private var restoreResolved = false
    private var savedSongId: Long? = null

    init {
        viewModelScope.launch {
            savedSongId =
                preferences
                    .lastPlayedSongId
                    .first()

            restoreFromQueueIfPossible()
        }

        viewModelScope.launch {
            controller.state.collect {
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
        val current =
            _state.value

        if (
            current.currentSongId == null ||
            current.queue.isEmpty()
        ) {
            controller.setQueue(songs)

            _state.update {
                it.copy(queue = songs)
            }
        } else {
            val available =
                songs.map {
                    it.id
                }.toSet()

            val retained =
                current.queue.filter {
                    it.id in available
                }

            val existing =
                retained.map {
                    it.id
                }.toSet()

            val merged =
                retained +
                    songs.filterNot {
                        it.id in existing
                    }

            controller.setQueue(merged)

            _state.update {
                it.copy(queue = merged)
            }
        }

        restoreFromQueueIfPossible()
    }

    fun playNextInQueue(
        song: Song
    ) {
        val queue =
            controller.playNext(song)

        _state.update {
            it.copy(queue = queue)
        }
    }

    fun addToQueue(
        song: Song
    ) {
        val queue =
            controller.addToQueue(song)

        _state.update {
            it.copy(queue = queue)
        }
    }

    fun removeFromQueue(
        songId: Long
    ) {
        val wasCurrent =
            _state.value.currentSongId ==
                songId

        val queue =
            controller.removeFromQueue(
                songId
            )

        if (wasCurrent) {
            controller.stop()
        }

        _state.update {
            it.copy(
                queue = queue,
                miniPlayerVisible =
                    if (wasCurrent) {
                        false
                    } else {
                        it.miniPlayerVisible
                    },
                nowPlayingVisible =
                    if (wasCurrent) {
                        false
                    } else {
                        it.nowPlayingVisible
                    }
            )
        }
    }

    private fun restoreFromQueueIfPossible() {
        if (restoreResolved) return

        val songs =
            _state.value.queue

        val id =
            savedSongId ?: return

        val song =
            songs.firstOrNull {
                it.id == id
            } ?: return

        restoreResolved = true
        controller.restoreSong(song.id)

        _state.update {
            current ->

            current.copy(
                currentSongId = song.id,
                currentIndex =
                    songs.indexOf(song),
                duration = song.duration,
                position = 0L,
                isPlaying = false,
                miniPlayerVisible = true,
                miniPlayerRiseKey =
                    current.miniPlayerRiseKey +
                        1
            )
        }
    }

    private fun persistSong(
        songId: Long
    ) {
        savedSongId = songId
        restoreResolved = true

        viewModelScope.launch {
            preferences
                .setLastPlayedSongId(
                    songId
                )
        }
    }

    fun play(
        song: Song
    ) {
        val needsEntrance =
            !_state.value
                .miniPlayerVisible &&
                !_state.value
                    .nowPlayingVisible

        controller.play(song)
        persistSong(song.id)

        _state.update {
            current ->

            current.copy(
                queue =
                    controller.currentQueue(),
                miniPlayerVisible = true,
                miniPlayerRiseKey =
                    if (needsEntrance) {
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
        val song =
            _state.value.queue
                .getOrNull(index)
                ?: return

        persistSong(song.id)

        controller.playQueueIndex(
            index = index,
            keepPlayingState = true
        )
    }

    fun playPrevious() {
        val target =
            _state.value
                .currentIndex - 1

        if (
            target !in
            _state.value.queue.indices
        ) return

        playQueueIndex(target)
    }

    fun playNext() {
        val target =
            _state.value
                .currentIndex + 1

        if (
            target !in
            _state.value.queue.indices
        ) return

        playQueueIndex(target)
    }

    fun seekTo(
        positionMs: Long
    ) {
        controller.seekTo(positionMs)
    }

    fun togglePlay() {
        controller.togglePlay()
    }

    fun openNowPlaying() {
        if (
            _state.value.currentSongId ==
            null
        ) return

        _state.update {
            it.copy(
                nowPlayingVisible = true,
                miniPlayerVisible = false
            )
        }
    }

    fun closeNowPlaying() {
        _state.update {
            current ->

            if (
                current.currentSongId ==
                null
            ) {
                current.copy(
                    nowPlayingVisible =
                        false,
                    miniPlayerVisible =
                        false
                )
            } else {
                current.copy(
                    nowPlayingVisible =
                        false,
                    miniPlayerVisible =
                        true,
                    miniPlayerRiseKey =
                        current
                            .miniPlayerRiseKey +
                            1
                )
            }
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
