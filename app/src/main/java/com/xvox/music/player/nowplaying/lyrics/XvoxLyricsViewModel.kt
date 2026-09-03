package com.xvox.music.player.nowplaying.lyrics

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.core.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class XvoxLyricsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        XvoxLyricsRepository(application)

    private val _state =
        MutableStateFlow(
            XvoxLyricsUiState()
        )

    val state:
        StateFlow<XvoxLyricsUiState> =
        _state.asStateFlow()

    private var currentSong: Song? = null
    private var loadJob: Job? = null

    fun load(
        song: Song
    ) {
        if (currentSong?.id == song.id) {
            return
        }

        currentSong = song
        loadJob?.cancel()

        val fullscreen =
            _state.value.fullscreen

        _state.value =
            XvoxLyricsUiState(
                loading = true,
                fullscreen = fullscreen
            )

        loadJob =
            viewModelScope.launch {
                val lyrics =
                    repository.load(song)

                if (
                    currentSong?.id ==
                    song.id
                ) {
                    _state.update {
                        it.copy(
                            loading = false,
                            lyrics = lyrics
                        )
                    }
                }
            }
    }

    fun attach(
        uri: Uri
    ) {
        val song =
            currentSong ?: return

        loadJob?.cancel()

        _state.update {
            it.copy(loading = true)
        }

        loadJob =
            viewModelScope.launch {
                val lyrics =
                    repository.attach(
                        song.id,
                        uri
                    )

                if (
                    currentSong?.id ==
                    song.id
                ) {
                    _state.update {
                        it.copy(
                            loading = false,
                            lyrics = lyrics
                        )
                    }
                }
            }
    }

    fun removeCustom() {
        val song =
            currentSong ?: return

        val source =
            _state.value
                .lyrics
                ?.source

        if (
            source != XvoxLyricsSource.USER_LRC &&
            source != XvoxLyricsSource.USER_TEXT
        ) {
            return
        }

        loadJob?.cancel()

        _state.update {
            it.copy(loading = true)
        }

        loadJob =
            viewModelScope.launch {
                val fallback =
                    repository.removeCustom(song)

                if (
                    currentSong?.id ==
                    song.id
                ) {
                    _state.update {
                        it.copy(
                            loading = false,
                            lyrics = fallback
                        )
                    }
                }
            }
    }

    fun openFullscreen() {
        _state.update {
            it.copy(fullscreen = true)
        }
    }

    fun closeFullscreen() {
        _state.update {
            it.copy(fullscreen = false)
        }
    }
}
