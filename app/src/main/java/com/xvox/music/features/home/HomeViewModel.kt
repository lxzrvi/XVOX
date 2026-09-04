package com.xvox.music.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.artwork.ArtworkPreloader
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import com.xvox.music.data.preferences.XvoxPlaylist
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

    private val libraryPreferences =
        XvoxLibraryPreferences(application)

    private val artworkPreloader =
        ArtworkPreloader(application)

    private val infoReader =
        SongInfoReader(application)

    private val _state =
        MutableStateFlow(
            HomeUiState()
        )

    val state:
        StateFlow<HomeUiState> =
        _state.asStateFlow()

    private var allSongs:
        List<Song> = emptyList()

    private var recentIds:
        List<Long> = emptyList()

    private var prefetchJob:
        Job? = null

    private var lastPrefetchStart = -1
    private var transitionId = 0L

    init {
        observeProfile()
        observeRecent()
        observeLibraryPreferences()
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
                    publishSongs()
                }
        }
    }

    private fun observeLibraryPreferences() {
        viewModelScope.launch {
            libraryPreferences
                .likedSongIds
                .collect { ids ->
                    _state.update {
                        it.copy(
                            likedSongIds = ids
                        )
                    }
                }
        }

        viewModelScope.launch {
            libraryPreferences
                .hiddenSongIds
                .collect { ids ->
                    _state.update {
                        it.copy(
                            hiddenSongIds = ids
                        )
                    }
                    publishSongs()
                }
        }

        viewModelScope.launch {
            libraryPreferences
                .playlists
                .collect { playlists ->
                    _state.update {
                        it.copy(
                            playlists =
                                playlists
                        )
                    }
                }
        }
    }

    private fun publishSongs() {
        val hidden =
            _state.value.hiddenSongIds

        val visible =
            allSongs.filterNot {
                it.id in hidden
            }

        _state.update {
            it.copy(
                songs = visible,
                recentlyPlayed =
                    resolveRecent(
                        visible,
                        recentIds
                    )
            )
        }
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            allSongs =
                songRepository.loadSongs()

            publishSongs()

            _state.update {
                it.copy(
                    loading = false
                )
            }

            prefetchFrom(0)
        }
    }

    fun refresh() {
        if (_state.value.refreshing) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    refreshing = true
                )
            }

            allSongs =
                songRepository.loadSongs()

            prefetchJob?.cancel()
            lastPrefetchStart = -1

            publishSongs()

            _state.update {
                it.copy(
                    refreshing = false,
                    loading = false
                )
            }

            prefetchFrom(0)
        }
    }

    fun toggleLiked(
        song: Song
    ) {
        val liked =
            song.id in
                _state.value.likedSongIds

        viewModelScope.launch {
            libraryPreferences.setLiked(
                song.id,
                !liked
            )
        }
    }

    fun hideSong(
        song: Song
    ) {
        viewModelScope.launch {
            libraryPreferences.hideSong(
                song.id
            )
        }
    }

    fun createPlaylist(
        name: String,
        songIds: Set<Long>,
        onDone: (XvoxPlaylist?) -> Unit
    ) {
        viewModelScope.launch {
            val result =
                libraryPreferences
                    .createPlaylist(
                        name,
                        songIds
                    )

            onDone(result)
        }
    }

    fun addToPlaylist(
        playlistId: String,
        song: Song,
        onDone: (XvoxPlaylist?) -> Unit
    ) {
        viewModelScope.launch {
            val result =
                libraryPreferences
                    .addSongToPlaylist(
                        playlistId,
                        song.id
                    )

            onDone(result)
        }
    }

    fun loadInfo(
        song: Song,
        onLoaded: (SongInfo) -> Unit
    ) {
        viewModelScope.launch {
            onLoaded(
                infoReader.read(song)
            )
        }
    }

    fun recordPlayedFromLibrary(
        song: Song,
        currentSongId: Long?
    ) {
        if (
            song.id ==
            currentSongId
        ) return

        transitionId++

        promote(
            song,
            RecentTransitionRequest(
                id = transitionId,
                songId = song.id,
                mode =
                    RecentTransitionMode
                        .LIBRARY
            )
        )
    }

    fun recordPlayedFromRecent(
        song: Song,
        currentSongId: Long?
    ) {
        if (
            song.id ==
            currentSongId
        ) return

        promote(
            song,
            RecentTransitionRequest(
                id =
                    _state.value
                        .recentTransition.id,
                songId = null,
                mode =
                    RecentTransitionMode.NONE
            )
        )
    }

    private fun promote(
        song: Song,
        transition:
            RecentTransitionRequest
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
                    }.take(20),
                recentTransition =
                    transition
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

        if (songs.isEmpty()) return

        val start =
            (sourceIndex / 12) * 12

        if (
            start ==
            lastPrefetchStart
        ) return

        lastPrefetchStart = start
        prefetchJob?.cancel()

        prefetchJob =
            viewModelScope.launch {
                artworkPreloader.warm(
                    songs = songs,
                    fromIndex = start,
                    count = 48
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
        ) return emptyList()

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
