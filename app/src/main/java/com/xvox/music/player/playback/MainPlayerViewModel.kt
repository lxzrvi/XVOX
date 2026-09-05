package com.xvox.music.player.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainPlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val controller = PlaybackController(application)
    private val preferences = UserPreferencesRepository(application)

    private val _state = MutableStateFlow(MainPlayerUiState())
    val state: StateFlow<MainPlayerUiState> = _state.asStateFlow()

    private var restoreResolved = false
    private var savedSongId: Long? = null
    private var libraryQueueSignature = 0L
    private var libraryQueueSize = -1
    private var originalQueueBeforeShuffle: List<Song>? = null

    private val sleepTimerManager = PlayerSleepTimerManager(
        scope = viewModelScope,
        stateFlow = _state,
        onStopPlayback = { controller.stop() }
    )

    init {
        viewModelScope.launch {
            savedSongId = preferences.lastPlayedSongId.first()
            restoreFromQueueIfPossible()
        }

        viewModelScope.launch {
            controller.state.collect { playback ->
                _state.update {
                    it.copy(
                        connected = playback.connected,
                        currentSongId = playback.currentSongId,
                        currentIndex = playback.currentIndex,
                        isPlaying = playback.isPlaying,
                        position = playback.position,
                        duration = playback.duration
                    )
                }
            }
        }
    }

    fun setQueue(songs: List<Song>) {
        val signature = queueSignature(songs)
        if (songs.size == libraryQueueSize && signature == libraryQueueSignature) return

        libraryQueueSize = songs.size
        libraryQueueSignature = signature

        val current = _state.value
        if (current.playingSource != "All Songs" && current.queue.isNotEmpty() && current.currentSongId != null) {
            return
        }

        if (current.currentSongId == null || current.queue.isEmpty()) {
            controller.setQueue(songs)
            _state.update { it.copy(queue = songs) }
            restoreFromQueueIfPossible()
            return
        }

        val available = HashSet<Long>(songs.size * 4 / 3 + 1)
        songs.forEach { available.add(it.id) }

        val retained = ArrayList<Song>(current.queue.size)
        val existing = HashSet<Long>(songs.size * 4 / 3 + 1)
        current.queue.forEach { song ->
            if (song.id in available) {
                retained.add(song)
                existing.add(song.id)
            }
        }

        val merged = ArrayList<Song>(songs.size)
        merged.addAll(retained)
        songs.forEach { song ->
            if (existing.add(song.id)) merged.add(song)
        }

        controller.setQueue(merged)
        _state.update { it.copy(queue = merged) }
        restoreFromQueueIfPossible()
    }

    fun setQueueExact(songs: List<Song>) {
        controller.setQueue(songs)
        libraryQueueSize = songs.size
        libraryQueueSignature = queueSignature(songs)
        _state.update { it.copy(queue = songs) }
        restoreFromQueueIfPossible()
    }

    fun reorderQueue(reordered: List<Song>) {
        PlayerQueueReorderHelper.reorderQueue(
            currentQueue = _state.value.queue,
            reordered = reordered,
            currentSongId = _state.value.currentSongId
        ) { validQueue, newIdx ->
            controller.setQueue(validQueue)
            libraryQueueSize = validQueue.size
            libraryQueueSignature = queueSignature(validQueue)
            _state.update { it.copy(queue = validQueue, currentIndex = newIdx) }
        }
    }

    fun playFromSource(song: Song, sourceQueue: List<Song>, source: String) {
        val queue = if (sourceQueue.isEmpty()) listOf(song) else sourceQueue
        controller.setQueue(queue)
        libraryQueueSize = queue.size
        libraryQueueSignature = queueSignature(queue)

        _state.update { it.copy(queue = queue, playingSource = source) }
        play(song, source)
    }

    fun setPlayingSource(source: String) {
        _state.update { it.copy(playingSource = source) }
    }

    fun playNextInQueue(song: Song) {
        val queue = controller.playNext(song)
        _state.update { it.copy(queue = queue) }
    }

    fun addToQueue(song: Song) {
        val queue = controller.addToQueue(song)
        _state.update { it.copy(queue = queue) }
    }

    fun removeFromQueue(songId: Long) {
        val wasCurrent = _state.value.currentSongId == songId
        val queue = controller.removeFromQueue(songId)
        if (wasCurrent) controller.stop()

        _state.update {
            it.copy(
                queue = queue,
                miniPlayerVisible = if (wasCurrent) false else it.miniPlayerVisible,
                nowPlayingVisible = if (wasCurrent) false else it.nowPlayingVisible
            )
        }
    }

    private fun restoreFromQueueIfPossible() {
        if (restoreResolved) return
        val songs = _state.value.queue
        val id = savedSongId ?: return
        val index = songs.indexOfFirst { it.id == id }
        if (index < 0) return
        val song = songs[index]

        restoreResolved = true
        controller.restoreState(song.id, 0L)

        _state.update { current ->
            current.copy(
                currentSongId = song.id,
                currentIndex = index,
                duration = song.duration,
                position = 0L,
                isPlaying = false,
                miniPlayerVisible = true,
                miniPlayerRiseKey = current.miniPlayerRiseKey + 1
            )
        }
    }

    private fun persistSong(songId: Long) {
        savedSongId = songId
        restoreResolved = true
        viewModelScope.launch { preferences.setLastPlayedSongId(songId) }
    }

    fun play(song: Song, source: String? = null) {
        val needsEntrance = !_state.value.miniPlayerVisible && !_state.value.nowPlayingVisible
        controller.play(song)
        persistSong(song.id)

        _state.update { current ->
            current.copy(
                queue = controller.currentQueue(),
                miniPlayerVisible = true,
                miniPlayerRiseKey = if (needsEntrance) current.miniPlayerRiseKey + 1 else current.miniPlayerRiseKey,
                playingSource = source ?: current.playingSource
            )
        }
    }

    fun playQueueIndex(index: Int) {
        val song = _state.value.queue.getOrNull(index) ?: return
        persistSong(song.id)
        controller.playQueueIndex(index = index, keepPlayingState = true)
    }

    fun playPrevious() {
        val queue = _state.value.queue
        val index = _state.value.currentIndex
        if (queue.isEmpty() || index < 0) return
        val atFirst = index <= 0
        if (atFirst && _state.value.repeatMode == RepeatMode.OFF) return
        val target = if (atFirst && _state.value.repeatMode == RepeatMode.ALL) queue.lastIndex else index - 1
        playQueueIndex(target)
    }

    fun playNext() {
        val queue = _state.value.queue
        val index = _state.value.currentIndex
        if (queue.isEmpty() || index < 0) return
        val atLast = index >= queue.lastIndex
        if (atLast && _state.value.repeatMode == RepeatMode.OFF) return
        val target = if (atLast && _state.value.repeatMode == RepeatMode.ALL) 0 else index + 1
        playQueueIndex(target)
    }

    fun seekTo(positionMs: Long) {
        controller.seekTo(positionMs)
    }

    fun togglePlay() {
        controller.togglePlay()
    }

    fun openNowPlaying() {
        if (_state.value.currentSongId == null) return
        _state.update { it.copy(nowPlayingVisible = true, miniPlayerVisible = false) }
    }

    fun closeNowPlaying() {
        _state.update { current ->
            if (current.currentSongId == null) {
                current.copy(nowPlayingVisible = false, miniPlayerVisible = false)
            } else {
                current.copy(
                    nowPlayingVisible = false,
                    miniPlayerVisible = true,
                    miniPlayerRiseKey = current.miniPlayerRiseKey + 1
                )
            }
        }
    }

    fun hideMiniPlayer() {
        _state.update { it.copy(miniPlayerVisible = false) }
    }

    fun stopPlayback() {
        controller.stop()
        _state.update { it.copy(miniPlayerVisible = false, nowPlayingVisible = false) }
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffleEnabled
        _state.update { it.copy(isShuffleEnabled = newShuffle) }

        if (newShuffle) {
            val currentQueue = _state.value.queue
            originalQueueBeforeShuffle = currentQueue.toList()
            val shuffled = PlayerQueueReorderHelper.shuffleQueue(_state.value.currentSongId, currentQueue)
            if (shuffled != null) {
                controller.setQueue(shuffled)
                _state.update { it.copy(queue = shuffled, currentIndex = 0) }
            }
        } else {
            val unshuffleResult = PlayerQueueReorderHelper.unshuffleQueue(_state.value.queue, originalQueueBeforeShuffle)
            if (unshuffleResult != null) {
                val orig = unshuffleResult.first
                val newIndex = orig.indexOfFirst { it.id == _state.value.currentSongId }.coerceAtLeast(0)
                controller.setQueue(orig)
                _state.update { it.copy(queue = orig, currentIndex = newIndex) }
            }
            originalQueueBeforeShuffle = null
        }
    }

    fun toggleRepeat() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = next) }
        controller.setRepeatMode(next)
    }

    fun moveQueueItem(from: Int, to: Int) {
        val queue = controller.moveQueueItem(from, to)
        val currentId = _state.value.currentSongId
        val currentIndex = queue.indexOfFirst { it.id == currentId }
        libraryQueueSize = queue.size
        libraryQueueSignature = queueSignature(queue)
        _state.update { it.copy(queue = queue, currentIndex = currentIndex) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerManager.setSleepTimer(minutes)
    }

    fun setCustomSleepTimer(minutes: Int, seconds: Int, pauseMusic: Boolean, closeApp: Boolean) {
        sleepTimerManager.setCustomSleepTimer(minutes, seconds, pauseMusic, closeApp)
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelSleepTimer()
    }

    fun consumeCloseApp() {
        _state.update { it.copy(sleepTimerShouldCloseApp = false) }
    }

    fun setPlayerStyle(style: XvoxPlayerStyle) {
        _state.update { it.copy(playerStyle = style) }
    }

    private fun queueSignature(songs: List<Song>): Long {
        var result = 1125899906842597L
        songs.forEach { result = result * 31L + it.id }
        return result
    }

    override fun onCleared() {
        sleepTimerManager.release()
        controller.release()
        super.onCleared()
    }
}
