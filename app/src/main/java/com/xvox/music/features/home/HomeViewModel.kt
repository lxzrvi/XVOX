package com.xvox.music.features.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.artwork.XvoxArtworkPreloader
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.data.preferences.XvoxLibraryPreferences
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.recent.RecentTransitionMode
import com.xvox.music.features.home.recent.RecentTransitionRequest
import com.xvox.music.features.playlist.XvoxHomeLibraryMode
import com.xvox.music.features.playlist.XvoxPlaylistCoverStorage
import com.xvox.music.media.MediaStoreSongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FolderInfo(
    val name: String,
    val songCount: Int,
    val songs: List<Song>,
    val path: String = name
)

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val songRepository = MediaStoreSongRepository(application)
    private val preferencesRepository = UserPreferencesRepository(application)
    private val libraryPreferences = XvoxLibraryPreferences(application)
    private val artworkPreloader = XvoxArtworkPreloader(application)
    private val coverStorage = XvoxPlaylistCoverStorage(application)
    private val infoReader = SongInfoReader(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderInfo>>(emptyList())
    val folders: StateFlow<List<FolderInfo>> = _folders.asStateFlow()

    private var allRawSongs: List<Song> = emptyList()
    private var recentIds: List<Long> = emptyList()
    private var prefetchJob: Job? = null
    private var lastPrefetchStart = -1
    private var transitionId = 0L

    private var filterConfig = LibraryFilterConfig("A-Z", 0, 0, emptySet())
    private val randomSeed = System.currentTimeMillis()

    init {
        observeProfile()
        observeRecent()
        observeLibraryPreferences()
        observeFilterPreferences()
        loadLibrary()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { profile ->
                _state.update { it.copy(profile = profile) }
            }
        }
    }

    private fun observeRecent() {
        viewModelScope.launch {
            preferencesRepository.recentSongIds.collect { ids ->
                recentIds = ids
                _state.update { current -> current.copy(recentlyPlayed = resolveRecent(current.songs, ids)) }
            }
        }
    }

    private fun observeLibraryPreferences() {
        viewModelScope.launch {
            libraryPreferences.likedSongIds.collect { ids ->
                _state.update { it.copy(likedSongIds = ids) }
            }
        }

        viewModelScope.launch {
            libraryPreferences.hiddenSongIds.collect { ids ->
                _state.update { it.copy(hiddenSongIds = ids) }
                publishFilteredSongs()
            }
        }

        viewModelScope.launch {
            libraryPreferences.playlists.collect { playlists ->
                _state.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun observeFilterPreferences() {
        viewModelScope.launch {
            combine(
                preferencesRepository.sortOrder,
                preferencesRepository.ignoreBelowSec,
                preferencesRepository.ignoreBelowKb,
                preferencesRepository.ignoredFolders
            ) { sort, sec, kb, ignored ->
                LibraryFilterConfig(sort, sec, kb, ignored)
            }.collect { config ->
                filterConfig = config
                publishFilteredSongs()
            }
        }
    }

    private fun publishFilteredSongs() {
        val sorted = HomeLibraryFilterHelper.filterAndSort(
            songs = allRawSongs,
            hiddenSongIds = _state.value.hiddenSongIds,
            config = filterConfig,
            randomSeed = randomSeed
        )
        _state.update {
            it.copy(
                songs = sorted,
                recentlyPlayed = resolveRecent(sorted, recentIds)
            )
        }
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { songRepository.loadSongs() }
            allRawSongs = loaded
            _folders.value = HomeLibraryFilterHelper.groupFolders(loaded)
            publishFilteredSongs()
            _state.update { it.copy(loading = false) }
            prefetchFrom(0)
        }
    }

    fun refresh(onDone: (LibraryRefreshResult) -> Unit = {}) {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            val before = allRawSongs.asSequence().map { it.id }.toSet()
            val refreshed = withContext(Dispatchers.IO) { songRepository.loadSongs() }
            val after = refreshed.asSequence().map { it.id }.toSet()
            val result = LibraryRefreshResult(
                totalSongs = refreshed.size,
                addedSongs = (after - before).size,
                removedSongs = (before - after).size
            )

            allRawSongs = refreshed
            _folders.value = HomeLibraryFilterHelper.groupFolders(refreshed)
            prefetchJob?.cancel()
            lastPrefetchStart = -1
            publishFilteredSongs()

            _state.update { it.copy(refreshing = false, loading = false) }
            prefetchFrom(0)
            onDone(result)
        }
    }

    fun saveProfile(username: String, selectedPfp: String, customPfpUri: String?, onDone: () -> Unit = {}) {
        if (username.isBlank()) return
        viewModelScope.launch {
            preferencesRepository.saveProfile(username, selectedPfp, customPfpUri)
            onDone()
        }
    }

    fun removeFromRecent(song: Song) {
        _state.update { it.copy(recentlyPlayed = it.recentlyPlayed.filterNot { s -> s.id == song.id }) }
        viewModelScope.launch { preferencesRepository.removeRecentSong(song.id) }
    }

    fun toggleLikedMode() {
        _state.update {
            it.copy(libraryMode = if (it.libraryMode == XvoxHomeLibraryMode.LIKED) XvoxHomeLibraryMode.ALL_SONGS else XvoxHomeLibraryMode.LIKED)
        }
    }

    fun togglePlaylistMode() {
        _state.update {
            it.copy(libraryMode = if (it.libraryMode == XvoxHomeLibraryMode.PLAYLISTS) XvoxHomeLibraryMode.ALL_SONGS else XvoxHomeLibraryMode.PLAYLISTS)
        }
    }

    fun setLibraryMode(mode: XvoxHomeLibraryMode) {
        _state.update { it.copy(libraryMode = mode) }
    }

    fun toggleLiked(song: Song) {
        val liked = song.id in _state.value.likedSongIds
        viewModelScope.launch { libraryPreferences.setLiked(song.id, !liked) }
    }

    fun addMultipleToLiked(songs: List<Song>) {
        viewModelScope.launch {
            val current = _state.value.likedSongIds.toMutableSet()
            current.addAll(songs.map { it.id })
            libraryPreferences.setAllLiked(current)
        }
    }

    fun removeMultipleFromLiked(songs: List<Song>) {
        viewModelScope.launch {
            val current = _state.value.likedSongIds.toMutableSet()
            current.removeAll(songs.map { it.id }.toSet())
            libraryPreferences.setAllLiked(current)
        }
    }

    fun addMultipleToPlaylist(playlistId: String, songs: List<Song>, onDone: (XvoxPlaylist?) -> Unit) {
        viewModelScope.launch {
            val pl = _state.value.playlists.firstOrNull { it.id == playlistId } ?: return@launch
            val updated = pl.copy(songIds = (pl.songIds + songs.map { it.id }).distinct())
            val saved = libraryPreferences.savePlaylist(updated)
            onDone(saved)
        }
    }

    fun removeMultipleFromPlaylist(playlistId: String, songs: List<Song>, onDone: (XvoxPlaylist?) -> Unit) {
        viewModelScope.launch {
            val pl = _state.value.playlists.firstOrNull { it.id == playlistId } ?: return@launch
            val removeSet = songs.map { it.id }.toSet()
            val updated = pl.copy(songIds = pl.songIds.filterNot { it in removeSet })
            val saved = libraryPreferences.savePlaylist(updated)
            onDone(saved)
        }
    }

    fun hideSong(song: Song) = viewModelScope.launch { libraryPreferences.hideSong(song.id) }

    fun createPlaylist(name: String, songIds: Set<Long>, onDone: (XvoxPlaylist?) -> Unit) =
        viewModelScope.launch { onDone(libraryPreferences.createPlaylist(name, songIds)) }

    fun addToPlaylist(playlistId: String, song: Song, onDone: (XvoxPlaylist?) -> Unit) =
        viewModelScope.launch { onDone(libraryPreferences.addSongToPlaylist(playlistId, song.id)) }

    fun removeFromPlaylist(playlistId: String, song: Song, onDone: (XvoxPlaylist?) -> Unit) =
        viewModelScope.launch { onDone(libraryPreferences.removeSongFromPlaylist(playlistId, song.id)) }

    fun renamePlaylist(playlistId: String, name: String, onDone: (XvoxPlaylist?) -> Unit) =
        viewModelScope.launch { onDone(libraryPreferences.renamePlaylist(playlistId, name)) }

    fun savePlaylistCover(playlistId: String, songIds: List<Long>, customUri: Uri?, onDone: (XvoxPlaylist?) -> Unit) {
        viewModelScope.launch {
            val persistedUri = if (customUri != null) coverStorage.persist(playlistId, customUri)
            else { coverStorage.delete(playlistId); null }
            val updated = libraryPreferences.setPlaylistCover(playlistId, songIds, persistedUri)
            onDone(updated)
        }
    }

    fun deletePlaylist(playlistId: String, onDone: () -> Unit) = viewModelScope.launch {
        libraryPreferences.deletePlaylist(playlistId)
        coverStorage.delete(playlistId)
        onDone()
    }

    fun playlistSongs(playlist: XvoxPlaylist): List<Song> {
        val byId = _state.value.songs.associateBy { it.id }
        return playlist.songIds.mapNotNull { byId[it] }
    }

    fun likedSongs(): List<Song> {
        val liked = _state.value.likedSongIds
        return _state.value.songs.filter { it.id in liked }
    }

    fun loadInfo(song: Song, onLoaded: (SongInfo) -> Unit) = viewModelScope.launch {
        val info = withContext(Dispatchers.IO) { infoReader.read(song) }
        onLoaded(info)
    }

    fun recordPlayedFromLibrary(song: Song, currentSongId: Long?) {
        if (song.id == currentSongId) return
        transitionId++
        promote(song, RecentTransitionRequest(id = transitionId, songId = song.id, mode = RecentTransitionMode.LIBRARY))
    }

    fun recordPlayedFromRecent(song: Song, currentSongId: Long?) {
        if (song.id == currentSongId) return
        promote(song, RecentTransitionRequest(id = _state.value.recentTransition.id, songId = null, mode = RecentTransitionMode.NONE))
    }

    private fun promote(song: Song, transition: RecentTransitionRequest) {
        _state.update { current ->
            current.copy(
                recentlyPlayed = buildList {
                    add(song)
                    addAll(current.recentlyPlayed.filterNot { it.id == song.id })
                }.take(20),
                recentTransition = transition
            )
        }
        viewModelScope.launch { preferencesRepository.recordRecentSong(song.id) }
    }

    fun prefetchFrom(sourceIndex: Int) {
        val songs = _state.value.songs
        if (songs.isEmpty()) return
        val start = sourceIndex.coerceIn(0, songs.lastIndex)
        if (start == lastPrefetchStart) return
        lastPrefetchStart = start
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            artworkPreloader.warm(songs = songs, fromIndex = start, count = 24)
        }
    }

    private fun resolveRecent(songs: List<Song>, ids: List<Long>): List<Song> {
        if (songs.isEmpty() || ids.isEmpty()) return emptyList()
        val byId = songs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    override fun onCleared() {
        prefetchJob?.cancel()
        super.onCleared()
    }
}
