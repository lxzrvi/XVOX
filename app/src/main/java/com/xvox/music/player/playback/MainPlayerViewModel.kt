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
            val rawQueues = preferences.savedQueuesJson.first()
            val decoded = decodeSavedQueues(rawQueues)
            if (decoded.isNotEmpty()) {
                _state.update { it.copy(savedQueues = decoded) }
            }
            restoreFromQueueIfPossible()
        }

        viewModelScope.launch {
            controller.state.collect { playback ->
                if (playback.isPlaying && playback.currentSongId != null && playback.currentSongId != savedSongId) {
                    persistSong(playback.currentSongId)
                }
                _state.update {
                    it.copy(
                        queue = if (playback.queue.isNotEmpty()) playback.queue else it.queue,
                        playingSource = if (playback.externalQueue) "Queue" else it.playingSource,
                        miniPlayerVisible = if (playback.currentSongId == null) false else
                            if (playback.isPlaying && !it.nowPlayingVisible) true else it.miniPlayerVisible,
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
        controller.cacheLibrary(songs)
        val signature = queueSignature(songs)
        if (songs.size == libraryQueueSize && signature == libraryQueueSignature) return

        libraryQueueSize = songs.size
        libraryQueueSignature = signature

        val current = _state.value
        if (current.playingSource != "All Songs" && current.queue.isNotEmpty()) {
            val available = songs.mapTo(HashSet()) { it.id }
            val updatedSongs = songs.associateBy { it.id }
            val retained = current.queue.mapNotNull { updatedSongs[it.id] }
            if (retained != current.queue) {
                if (current.currentSongId != null && current.currentSongId !in available) stopPlayback()
                controller.setQueue(retained)
                _state.update { it.copy(queue = retained) }
            }
            return
        }
        if (current.currentSongId != null && songs.none { it.id == current.currentSongId }) stopPlayback()

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

    private val queueUndoStack = ArrayDeque<List<Song>>()

    fun canUndoQueue(): Boolean = queueUndoStack.isNotEmpty()

    fun undoLastQueueAction(): Boolean {
        if (queueUndoStack.isEmpty()) return false
        val previous = queueUndoStack.removeLast()
        controller.setQueue(previous)
        libraryQueueSize = previous.size
        libraryQueueSignature = queueSignature(previous)
        val currentSongId = _state.value.currentSongId
        val newIdx = previous.indexOfFirst { it.id == currentSongId }
        _state.update {
            it.copy(
                queue = previous,
                currentIndex = if (newIdx >= 0) newIdx else it.currentIndex
            )
        }
        return true
    }

    fun reorderQueue(reordered: List<Song>) {
        val currentQueue = _state.value.queue
        if (reordered.size != currentQueue.size || reordered == currentQueue) return

        queueUndoStack.addLast(currentQueue.toList())
        if (queueUndoStack.size > 20) queueUndoStack.removeFirst()

        PlayerQueueReorderHelper.reorderQueue(
            currentQueue = currentQueue,
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
        val activeQueueId = _state.value.activeQueueId
        if (activeQueueId != "queue_1" && activeQueueId.isNotBlank()) {
            val targetSong = song.copy(source = source)
            val currentQueue = _state.value.queue.toMutableList()
            val existingIdx = currentQueue.indexOfFirst { it.id == song.id }
            if (existingIdx < 0) {
                currentQueue.add(targetSong)
            }
            controller.setQueue(currentQueue)
            libraryQueueSize = currentQueue.size
            libraryQueueSignature = queueSignature(currentQueue)
            val newIdx = currentQueue.indexOfFirst { it.id == song.id }
            _state.update { it.copy(queue = currentQueue, currentIndex = newIdx) }
            play(targetSong, source)
            return
        }

        val sourcedQueue = (if (sourceQueue.isEmpty()) listOf(song) else sourceQueue).map {
            it.copy(source = source)
        }
        val targetSong = song.copy(source = source)
        controller.setQueue(sourcedQueue)
        libraryQueueSize = sourcedQueue.size
        libraryQueueSignature = queueSignature(sourcedQueue)

        _state.update { it.copy(queue = sourcedQueue, playingSource = source) }
        play(targetSong, source)
    }

    fun setPlayingSource(source: String) {
        _state.update { it.copy(playingSource = source) }
    }

    fun playNextInQueue(song: Song): String {
        val currentId = _state.value.currentSongId
        val currentIndex = _state.value.currentIndex
        val currentQueue = _state.value.queue

        if (currentId == song.id) {
            return "Can't add next song — song is already playing."
        }

        queueUndoStack.addLast(currentQueue.toList())
        if (queueUndoStack.size > 20) queueUndoStack.removeFirst()

        val mutable = currentQueue.toMutableList()
        val existingIdx = mutable.indexOfFirst { it.id == song.id }

        val itemToInsert = if (existingIdx >= 0) {
            mutable.removeAt(existingIdx)
        } else song

        val activeIdx = mutable.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: currentIndex
        val insertAt = (activeIdx + 1).coerceIn(0, mutable.size)
        mutable.add(insertAt, itemToInsert)

        controller.setQueue(mutable)
        libraryQueueSize = mutable.size
        libraryQueueSignature = queueSignature(mutable)
        val newIdx = mutable.indexOfFirst { it.id == currentId }
        _state.update { it.copy(queue = mutable, currentIndex = if (newIdx >= 0) newIdx else it.currentIndex) }
        return "Playing next"
    }

    fun playNextInQueue(songs: List<Song>): String {
        if (songs.isEmpty()) return ""
        if (songs.size == 1) return playNextInQueue(songs[0])

        val currentId = _state.value.currentSongId
        val currentIndex = _state.value.currentIndex
        val currentQueue = _state.value.queue

        queueUndoStack.addLast(currentQueue.toList())
        if (queueUndoStack.size > 20) queueUndoStack.removeFirst()

        val mutable = currentQueue.toMutableList()
        val activeIdx = mutable.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: currentIndex
        var insertAt = (activeIdx + 1).coerceIn(0, mutable.size)

        for (song in songs) {
            val existingIdx = mutable.indexOfFirst { it.id == song.id }
            val itemToInsert = if (existingIdx >= 0 && existingIdx != activeIdx) {
                val removed = mutable.removeAt(existingIdx)
                if (existingIdx < insertAt) insertAt--
                removed
            } else song

            mutable.add(insertAt.coerceIn(0, mutable.size), itemToInsert)
            insertAt++
        }

        controller.setQueue(mutable)
        libraryQueueSize = mutable.size
        libraryQueueSignature = queueSignature(mutable)
        val newIdx = mutable.indexOfFirst { it.id == currentId }
        _state.update { it.copy(queue = mutable, currentIndex = if (newIdx >= 0) newIdx else it.currentIndex) }
        return "${songs.size} songs playing next"
    }

    fun addToQueue(song: Song): String {
        val currentId = _state.value.currentSongId
        val currentIndex = _state.value.currentIndex
        val currentQueue = _state.value.queue

        queueUndoStack.addLast(currentQueue.toList())
        if (queueUndoStack.size > 20) queueUndoStack.removeFirst()

        val mutable = currentQueue.toMutableList()
        val existingIdx = mutable.indexOfFirst { it.id == song.id }

        val itemToAdd = if (existingIdx >= 0 && existingIdx != currentIndex) {
            mutable.removeAt(existingIdx)
        } else song

        mutable.add(itemToAdd)

        controller.setQueue(mutable)
        libraryQueueSize = mutable.size
        libraryQueueSignature = queueSignature(mutable)
        val newIdx = mutable.indexOfFirst { it.id == currentId }
        _state.update { it.copy(queue = mutable, currentIndex = if (newIdx >= 0) newIdx else it.currentIndex) }
        val targetName = _state.value.activeQueueName
        return "Added to $targetName"
    }

    fun addToQueue(songs: List<Song>): String {
        if (songs.isEmpty()) return ""
        if (songs.size == 1) return addToQueue(songs[0])

        val currentId = _state.value.currentSongId
        val currentIndex = _state.value.currentIndex
        val currentQueue = _state.value.queue

        queueUndoStack.addLast(currentQueue.toList())
        if (queueUndoStack.size > 20) queueUndoStack.removeFirst()

        val mutable = currentQueue.toMutableList()
        for (song in songs) {
            val existingIdx = mutable.indexOfFirst { it.id == song.id }
            val itemToAdd = if (existingIdx >= 0 && existingIdx != currentIndex) {
                mutable.removeAt(existingIdx)
            } else song

            mutable.add(itemToAdd)
        }

        controller.setQueue(mutable)
        libraryQueueSize = mutable.size
        libraryQueueSignature = queueSignature(mutable)
        val newIdx = mutable.indexOfFirst { it.id == currentId }
        _state.update { it.copy(queue = mutable, currentIndex = if (newIdx >= 0) newIdx else it.currentIndex) }
        val targetName = _state.value.activeQueueName
        return "${songs.size} songs added to $targetName"
    }

    fun addToNewQueue(song: Song): String {
        val nextQueueNumber = _state.value.savedQueues.size + 2
        val newQueueName = "Queue $nextQueueNumber"
        val newQueueId = "queue_$nextQueueNumber"

        val newQueue = XvoxSavedQueue(
            id = newQueueId,
            name = newQueueName,
            songs = listOf(song),
            currentIndex = 0,
            source = song.source.ifBlank { "All Songs" }
        )

        val updatedSaved = _state.value.savedQueues + newQueue
        _state.update { it.copy(savedQueues = updatedSaved) }
        persistSavedQueues(updatedSaved)
        return "Added to $newQueueName"
    }

    fun addToNewQueue(songs: List<Song>): String {
        if (songs.isEmpty()) return ""
        val nextQueueNumber = _state.value.savedQueues.size + 2
        val newQueueName = "Queue $nextQueueNumber"
        val newQueueId = "queue_$nextQueueNumber"

        val newQueue = XvoxSavedQueue(
            id = newQueueId,
            name = newQueueName,
            songs = songs,
            currentIndex = 0,
            source = songs.firstOrNull()?.source?.ifBlank { "All Songs" } ?: "All Songs"
        )

        val updatedSaved = _state.value.savedQueues + newQueue
        _state.update { it.copy(savedQueues = updatedSaved) }
        persistSavedQueues(updatedSaved)
        return "${songs.size} songs added to $newQueueName"
    }

    fun addToSavedQueue(queueId: String, song: Song): String {
        val target = _state.value.savedQueues.firstOrNull { it.id == queueId } ?: return addToQueue(song)
        val updatedSongs = target.songs.filterNot { it.id == song.id } + song
        val updatedQueue = target.copy(songs = updatedSongs)
        val updatedSaved = _state.value.savedQueues.map { q -> if (q.id == queueId) updatedQueue else q }
        _state.update {
            it.copy(savedQueues = updatedSaved)
        }
        persistSavedQueues(updatedSaved)
        return "Added to ${target.name}"
    }

    fun addToSavedQueue(queueId: String, songs: List<Song>): String {
        val target = _state.value.savedQueues.firstOrNull { it.id == queueId } ?: return addToQueue(songs)
        val songIds = songs.map { it.id }.toSet()
        val updatedSongs = target.songs.filterNot { it.id in songIds } + songs
        val updatedQueue = target.copy(songs = updatedSongs)
        val updatedSaved = _state.value.savedQueues.map { q -> if (q.id == queueId) updatedQueue else q }
        _state.update {
            it.copy(savedQueues = updatedSaved)
        }
        persistSavedQueues(updatedSaved)
        return "${songs.size} songs added to ${target.name}"
    }

    fun updateSavedQueue(queueId: String, songs: List<Song>) {
        val updatedSaved = _state.value.savedQueues.map { q ->
            if (q.id == queueId) q.copy(songs = songs) else q
        }
        _state.update {
            it.copy(savedQueues = updatedSaved)
        }
        persistSavedQueues(updatedSaved)
    }

    fun switchToQueue(targetQueueId: String) {
        val currentQueue = _state.value.queue
        val currentActiveId = _state.value.activeQueueId
        val currentActiveName = _state.value.activeQueueName
        val currentIdx = _state.value.currentIndex
        val currentSource = _state.value.playingSource

        if (targetQueueId == currentActiveId) return

        val savedList = _state.value.savedQueues.toMutableList()
        val existingIndex = savedList.indexOfFirst { it.id == currentActiveId }
        val activeQueueEntry = XvoxSavedQueue(
            id = currentActiveId,
            name = currentActiveName,
            songs = currentQueue,
            currentIndex = currentIdx.coerceAtLeast(0),
            source = currentSource
        )
        if (existingIndex >= 0) {
            savedList[existingIndex] = activeQueueEntry
        } else {
            savedList.add(activeQueueEntry)
        }

        val target = if (targetQueueId == "queue_1" || targetQueueId.isBlank()) {
            savedList.firstOrNull { it.id == "queue_1" } ?: XvoxSavedQueue(
                id = "queue_1",
                name = "Queue 1",
                songs = emptyList()
            )
        } else {
            savedList.firstOrNull { it.id == targetQueueId } ?: run {
                val qNumber = targetQueueId.removePrefix("queue_")
                val created = XvoxSavedQueue(
                    id = targetQueueId,
                    name = "Queue $qNumber",
                    songs = emptyList()
                )
                savedList.add(created)
                created
            }
        }

        controller.setQueue(target.songs)
        libraryQueueSize = target.songs.size
        libraryQueueSignature = queueSignature(target.songs)

        _state.update {
            it.copy(
                queue = target.songs,
                activeQueueId = target.id,
                activeQueueName = target.name,
                playingSource = target.source,
                savedQueues = savedList,
                currentIndex = target.currentIndex.coerceIn(0, (target.songs.size - 1).coerceAtLeast(0))
            )
        }
        persistSavedQueues(savedList)
    }

    private fun persistSavedQueues(queues: List<XvoxSavedQueue>) {
        viewModelScope.launch {
            preferences.setSavedQueuesJson(encodeSavedQueues(queues))
        }
    }

    private fun encodeSavedQueues(queues: List<XvoxSavedQueue>): String {
        val arr = org.json.JSONArray()
        for (q in queues) {
            val obj = org.json.JSONObject()
            obj.put("id", q.id)
            obj.put("name", q.name)
            obj.put("currentIndex", q.currentIndex)
            obj.put("source", q.source)
            val songsArr = org.json.JSONArray()
            for (s in q.songs) {
                val sObj = org.json.JSONObject()
                sObj.put("id", s.id)
                sObj.put("title", s.title)
                sObj.put("artist", s.artist)
                sObj.put("contentUri", s.contentUri.toString())
                sObj.put("artworkUri", s.artworkUri?.toString() ?: "")
                sObj.put("duration", s.duration)
                sObj.put("sizeBytes", s.sizeBytes)
                sObj.put("folderName", s.folderName)
                sObj.put("folderPath", s.folderPath)
                sObj.put("source", s.source)
                songsArr.put(sObj)
            }
            obj.put("songs", songsArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeSavedQueues(raw: String): List<XvoxSavedQueue> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = org.json.JSONArray(raw)
            val result = mutableListOf<XvoxSavedQueue>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "queue_$i")
                val name = obj.optString("name", "Queue ${i + 1}")
                val currentIndex = obj.optInt("currentIndex", 0)
                val source = obj.optString("source", "Queue")
                val songsArr = obj.optJSONArray("songs") ?: org.json.JSONArray()
                val songs = mutableListOf<Song>()
                for (j in 0 until songsArr.length()) {
                    val sObj = songsArr.getJSONObject(j)
                    val sId = sObj.getLong("id")
                    val title = sObj.optString("title", "")
                    val artist = sObj.optString("artist", "")
                    val contentUriStr = sObj.optString("contentUri", "")
                    val artworkUriStr = sObj.optString("artworkUri", "")
                    val duration = sObj.optLong("duration", 0L)
                    val sizeBytes = sObj.optLong("sizeBytes", 0L)
                    val folderName = sObj.optString("folderName", "")
                    val folderPath = sObj.optString("folderPath", folderName)
                    val sSource = sObj.optString("source", "")
                    songs.add(
                        Song(
                            id = sId,
                            title = title,
                            artist = artist,
                            contentUri = android.net.Uri.parse(contentUriStr),
                            artworkUri = if (artworkUriStr.isNotBlank()) android.net.Uri.parse(artworkUriStr) else null,
                            duration = duration,
                            sizeBytes = sizeBytes,
                            folderName = folderName,
                            folderPath = folderPath,
                            source = sSource
                        )
                    )
                }
                result.add(XvoxSavedQueue(id, name, songs, currentIndex, source))
            }
            result
        }.getOrDefault(emptyList())
    }

    fun removeFromQueue(songId: Long) {
        val wasCurrent = _state.value.currentSongId == songId
        val currentQueue = _state.value.queue

        queueUndoStack.addLast(currentQueue.toList())
        if (queueUndoStack.size > 20) queueUndoStack.removeFirst()

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

    fun removeFromQueueAt(index: Int) {
        val currentQueue = _state.value.queue
        if (index !in currentQueue.indices) return
        val wasCurrent = _state.value.currentIndex == index

        queueUndoStack.addLast(currentQueue.toList())
        if (queueUndoStack.size > 20) queueUndoStack.removeFirst()

        val mutable = currentQueue.toMutableList().apply { removeAt(index) }
        if (wasCurrent) controller.stop()
        controller.setQueue(mutable)
        libraryQueueSize = mutable.size
        libraryQueueSignature = queueSignature(mutable)

        val currentSongId = _state.value.currentSongId
        val newIdx = mutable.indexOfFirst { it.id == currentSongId }

        _state.update {
            it.copy(
                queue = mutable,
                currentIndex = if (wasCurrent) -1 else (if (newIdx >= 0) newIdx else it.currentIndex),
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

    private fun persistSong(songId: Long, source: String? = null) {
        savedSongId = songId
        restoreResolved = true
        val src = source ?: _state.value.playingSource
        viewModelScope.launch {
            preferences.setLastPlayedSongId(songId)
            preferences.recordRecentSong(songId, src)
        }
    }

    fun play(song: Song, source: String? = null) {
        val needsEntrance = !_state.value.miniPlayerVisible && !_state.value.nowPlayingVisible
        controller.play(song)
        persistSong(song.id, source)

        _state.update { current ->
            current.copy(
                queue = controller.currentQueue(),
                miniPlayerVisible = true,
                miniPlayerRiseKey = if (needsEntrance) current.miniPlayerRiseKey + 1 else current.miniPlayerRiseKey,
                playingSource = source ?: current.playingSource
            )
        }
    }

    fun playQueueIndex(index: Int, keepPlayingState: Boolean = true) {
        val song = _state.value.queue.getOrNull(index) ?: return
        persistSong(song.id, _state.value.playingSource)
        controller.playQueueIndex(index = index, keepPlayingState = keepPlayingState)
    }

    fun playPrevious() {
        val queue = _state.value.queue
        val index = _state.value.currentIndex
        val position = _state.value.position
        if (queue.isEmpty() || index < 0) return

        if (position > 5000L) {
            seekTo(0L)
            return
        }

        val atFirst = index <= 0
        if (atFirst && _state.value.repeatMode == RepeatMode.OFF) {
            seekTo(0L)
            return
        }
        val target = if (atFirst && _state.value.repeatMode == RepeatMode.ALL) queue.lastIndex else index - 1
        playQueueIndex(target, keepPlayingState = false)
    }

    fun playNext() {
        val queue = _state.value.queue
        val index = _state.value.currentIndex
        if (queue.isEmpty() || index < 0) return
        val atLast = index >= queue.lastIndex
        if (atLast && _state.value.repeatMode == RepeatMode.OFF) return
        val target = if (atLast && _state.value.repeatMode == RepeatMode.ALL) 0 else index + 1
        playQueueIndex(target, keepPlayingState = false)
    }

    fun seekTo(positionMs: Long) {
        controller.seekTo(positionMs)
    }

    fun togglePlay() {
        controller.togglePlay()
    }

    fun openNowPlaying() {
        if (_state.value.currentSongId == null) return
        controller.setFastProgress(true)
        _state.update { it.copy(nowPlayingVisible = true, miniPlayerVisible = false) }
    }

    fun closeNowPlaying() {
        controller.setFastProgress(false)
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
        controller.setFastProgress(false)
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
