package com.xvox.music.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.artwork.ArtworkPreloader
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.library.HomeLibraryMode
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
        MediaStoreSongRepository(
            application
        )

    private val preferencesRepository =
        UserPreferencesRepository(
            application
        )

    private val libraryPreferences =
        XvoxLibraryPreferences(
            application
        )

    private val artworkPreloader =
        ArtworkPreloader(
            application
        )

    private val infoReader =
        SongInfoReader(
            application
        )

    private val _state =
        MutableStateFlow(
            HomeUiState()
        )

    val state:
        StateFlow<HomeUiState> =
        _state.asStateFlow()

    private var allSongs:
        List<Song> =
        emptyList()

    private var recentIds:
        List<Long> =
        emptyList()

    private var prefetchJob:
        Job? = null

    private var lastPrefetchStart =
        -1

    private var transitionId =
        0L

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
                            likedSongIds =
                                ids
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
                            hiddenSongIds =
                                ids
                        )
                    }

                    publishSongs()
                }
        }

        viewModelScope.launch {
            libraryPreferences
                .playlists
                .collect {
                    playlists ->

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
            _state.value
                .hiddenSongIds

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
                songRepository
                    .loadSongs()

            publishSongs()

            _state.update {
                it.copy(
                    loading = false
                )
            }

            prefetchFrom(0)
        }
    }

    fun refresh(
        onDone:
            (LibraryRefreshResult) -> Unit =
            {}
    ) {
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

            val beforeIds =
                allSongs
                    .asSequence()
                    .map {
                        it.id
                    }
                    .toSet()

            val refreshed =
                songRepository
                    .loadSongs()

            val afterIds =
                refreshed
                    .asSequence()
                    .map {
                        it.id
                    }
                    .toSet()

            val result =
                LibraryRefreshResult(
                    totalSongs =
                        refreshed.size,
                    addedSongs =
                        (afterIds - beforeIds)
                            .size,
                    removedSongs =
                        (beforeIds - afterIds)
                            .size
                )

            allSongs =
                refreshed

            prefetchJob?.cancel()
            lastPrefetchStart =
                -1

            publishSongs()

            _state.update {
                it.copy(
                    refreshing = false,
                    loading = false
                )
            }

            prefetchFrom(0)

            onDone(result)
        }
    }

    fun saveProfile(
        username: String,
        selectedPfp: String,
        customPfpUri: String?,
        onDone: () -> Unit = {}
    ) {
        if (
            username.isBlank()
        ) {
            return
        }

        viewModelScope.launch {
            preferencesRepository
                .saveProfile(
                    username =
                        username,
                    selectedPfp =
                        selectedPfp,
                    customPfpUri =
                        customPfpUri
                )

            onDone()
        }
    }

    fun removeFromRecent(
        song: Song
    ) {
        _state.update {
            current ->

            current.copy(
                recentlyPlayed =
                    current
                        .recentlyPlayed
                        .filterNot {
                            it.id ==
                                song.id
                        }
            )
        }

        viewModelScope.launch {
            preferencesRepository
                .removeRecentSong(
                    song.id
                )
        }
    }

    fun toggleLikedMode() {
        _state.update {
            current ->

            current.copy(
                libraryMode =
                    if (
                        current.libraryMode ==
                        HomeLibraryMode.LIKED
                    ) {
                        HomeLibraryMode
                            .ALL_SONGS
                    } else {
                        HomeLibraryMode
                            .LIKED
                    }
            )
        }
    }

    fun togglePlaylistMode() {
        _state.update {
            current ->

            current.copy(
                libraryMode =
                    if (
                        current.libraryMode ==
                        HomeLibraryMode
                            .PLAYLISTS
                    ) {
                        HomeLibraryMode
                            .ALL_SONGS
                    } else {
                        HomeLibraryMode
                            .PLAYLISTS
                    }
            )
        }
    }

    fun setLibraryMode(
        mode: HomeLibraryMode
    ) {
        _state.update {
            it.copy(
                libraryMode = mode
            )
        }
    }

    fun toggleLiked(
        song: Song
    ) {
        val liked =
            song.id in
                _state.value
                    .likedSongIds

        viewModelScope.launch {
            libraryPreferences
                .setLiked(
                    song.id,
                    !liked
                )
        }
    }

    fun hideSong(
        song: Song
    ) {
        viewModelScope.launch {
            libraryPreferences
                .hideSong(
                    song.id
                )
        }
    }

    fun createPlaylist(
        name: String,
        songIds: Set<Long>,
        onDone:
            (XvoxPlaylist?) -> Unit
    ) {
        viewModelScope.launch {
            onDone(
                libraryPreferences
                    .createPlaylist(
                        name,
                        songIds
                    )
            )
        }
    }

    fun addToPlaylist(
        playlistId: String,
        song: Song,
        onDone:
            (XvoxPlaylist?) -> Unit
    ) {
        viewModelScope.launch {
            onDone(
                libraryPreferences
                    .addSongToPlaylist(
                        playlistId,
                        song.id
                    )
            )
        }
    }

    fun removeFromPlaylist(
        playlistId: String,
        song: Song,
        onDone:
            (XvoxPlaylist?) -> Unit
    ) {
        viewModelScope.launch {
            onDone(
                libraryPreferences
                    .removeSongFromPlaylist(
                        playlistId,
                        song.id
                    )
            )
        }
    }

    fun renamePlaylist(
        playlistId: String,
        name: String,
        onDone:
            (XvoxPlaylist?) -> Unit
    ) {
        viewModelScope.launch {
            onDone(
                libraryPreferences
                    .renamePlaylist(
                        playlistId,
                        name
                    )
            )
        }
    }

    fun deletePlaylist(
        playlistId: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            libraryPreferences
                .deletePlaylist(
                    playlistId
                )

            onDone()
        }
    }

    fun playlistSongs(
        playlist: XvoxPlaylist
    ): List<Song> {
        val byId =
            _state.value
                .songs
                .associateBy {
                    it.id
                }

        return playlist
            .songIds
            .mapNotNull {
                byId[it]
            }
    }

    fun likedSongs():
        List<Song> {
        val liked =
            _state.value
                .likedSongIds

        return _state.value
            .songs
            .filter {
                it.id in liked
            }
    }

    fun loadInfo(
        song: Song,
        onLoaded:
            (SongInfo) -> Unit
    ) {
        viewModelScope.launch {
            onLoaded(
                infoReader.read(
                    song
                )
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
        ) {
            return
        }

        transitionId++

        promote(
            song,
            RecentTransitionRequest(
                id =
                    transitionId,
                songId =
                    song.id,
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
        ) {
            return
        }

        promote(
            song,
            RecentTransitionRequest(
                id =
                    _state.value
                        .recentTransition
                        .id,
                songId = null,
                mode =
                    RecentTransitionMode
                        .NONE
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

        if (
            songs.isEmpty()
        ) {
            return
        }

        val start =
            sourceIndex
                .coerceIn(
                    0,
                    songs.lastIndex
                )

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
                artworkPreloader
                    .warm(
                        songs = songs,
                        fromIndex =
                            start,
                        count = 24
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
