package com.xvox.music.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.xvox.music.core.model.Song
import com.xvox.music.player.session.XvoxPlaybackService
import com.xvox.music.widget.XvoxAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

data class PlaybackState(
    val connected: Boolean = false,
    val currentSongId: Long? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val queue: List<Song> = emptyList(),
    val externalQueue: Boolean = false
)

class PlaybackController(
    context: Context
) {
    companion object {
        @Volatile var activeInstance: PlaybackController? = null
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controller: MediaController? = null
    private var queue: List<Song> = emptyList()
    private var lastQueueInput: List<Song>? = null
    private val knownSongs = mutableMapOf<Long, Song>()
    private var externalQueue = false
    private var progressJob: Job? = null
    private var fastProgress = false
    fun setFastProgress(enabled: Boolean) { fastProgress = enabled }
    private var queueJob: Job? = null
    private var queuePositions: Map<Long, Int> = emptyMap()
    private var nativeIds = mutableListOf<Long>()
    private var nativePositions: Map<Long, Int> = emptyMap()
    private var installedQueue: List<Song>? = null
    private var installingQueue: List<Song>? = null
    private var installAnchor = -1
    private var installPrefix = 0
    private var installTail = -1

    private fun indexOf(id: Long?): Int = id?.let { queuePositions[it] } ?: -1
    private fun indexNative(): Unit { nativePositions = nativeIds.withIndex().associate { it.value to it.index } }


    private var restoredSongId: Long? = null
    private var repeatMode: RepeatMode = RepeatMode.OFF

    private var expectedPlaying: Boolean? = null
    private var expectedPlayingUntil: Long = 0L

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var pendingPlay: Song? = null
    private var released = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (externalQueue && events.contains(Player.EVENT_TIMELINE_CHANGED)) adoptNativeQueue()
            publishState()
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            publishState()
        }
    }

    init {
        activeInstance = this
        connect()
    }

    private fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, XvoxPlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()

        future.addListener(
            {
                runCatching { future.get() }.onSuccess { mediaController ->
                    if (released) {
                        mediaController.release()
                        return@onSuccess
                    }
                    controller = mediaController
                    mediaController.addListener(listener)
                    if (mediaController.mediaItemCount > 0 && pendingPlay == null) {
                        externalQueue = true
                        adoptNativeQueue()
                        repeatMode = when (mediaController.repeatMode) { Player.REPEAT_MODE_ALL -> RepeatMode.ALL; Player.REPEAT_MODE_ONE -> RepeatMode.ONE; else -> RepeatMode.OFF }
                    } else setRepeatMode(repeatMode)
                    val pending = pendingPlay
                    pendingPlay = null
                    if (pending != null) play(pending) else publishState()

                    progressJob = scope.launch {
                        while (isActive) {
                            publishState()
                            delay(if (fastProgress && controller?.playWhenReady == true) 50L else 250L)
                        }
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        controller?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        publishState()
    }

    fun setQueue(songs: List<Song>) {
        if (songs === lastQueueInput || songs === queue) return
        lastQueueInput = songs
        if (songs == queue) return
        songs.forEach { knownSongs[it.id] = it }
        val sameIds = songs.size == queue.size && songs.indices.all { songs[it].id == queue[it].id }
        val wasInstalled = installedQueue === queue
        queue = songs.distinctBy { it.id }
        if (sameIds) {
            if (wasInstalled) installedQueue = queue
            else if (installingQueue != null) {
                queueJob?.cancel()
                val p = controller; val id = p?.currentMediaItem?.mediaId?.toLongOrNull()
                if (p != null && id != null) queueJob = scope.launch { yield(); installAround(p, id, preserveCurrent = true) }
            }
            publishState()
            return
        }
        externalQueue = false
        queuePositions = queue.withIndex().associate { it.value.id to it.index }
        installedQueue = null
        queueJob?.cancel()
        val p = controller
        val currentId = p?.currentMediaItem?.mediaId?.toLongOrNull()
        if (p != null && currentId != null && indexOf(currentId) >= 0) {
            queueJob = scope.launch {
                yield() // A tap may immediately choose a different song; don't serialize the old queue first.
                installAround(p, currentId, preserveCurrent = true)
            }
        }
        publishState()
    }

    private fun adoptNativeQueue() {
        lastQueueInput = null
        val p = controller ?: return
        if (p.mediaItemCount == 0) return
        queue = List(p.mediaItemCount) { index ->
            val item = p.getMediaItemAt(index)
            val id = item.mediaId.toLongOrNull() ?: -index.toLong() - 1
            knownSongs[id] ?: Song(id, item.mediaMetadata.title?.toString() ?: "Unknown song",
                item.mediaMetadata.artist?.toString() ?: "Unknown artist",
                item.mediaMetadata.extras?.getString("xvox_original_uri")?.let(android.net.Uri::parse) ?: item.localConfiguration?.uri ?: android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                item.mediaMetadata.artworkUri, if (index == p.currentMediaItemIndex) p.duration.coerceAtLeast(0) else 0)
        }
        queuePositions = queue.withIndex().associate { it.value.id to it.index }
        nativeIds = queue.map { it.id }.toMutableList(); indexNative()
        installedQueue = queue
    }

    /** Keep the audible item, then populate the timeline in small, cancellable batches. */
    private suspend fun installAround(p: MediaController, anchorId: Long, preserveCurrent: Boolean) {
        val snapshot = queue
        val anchor = snapshot.indexOfFirst { it.id == anchorId }
        if (anchor < 0 || released || controller !== p) return
        installingQueue = snapshot
        try {
            if (preserveCurrent) {
                val activeIndex = p.currentMediaItemIndex
                if (p.currentMediaItem?.mediaId != anchorId.toString()) return
                if (activeIndex + 1 < p.mediaItemCount) p.removeMediaItems(activeIndex + 1, p.mediaItemCount)
                if (activeIndex > 0) p.removeMediaItems(0, activeIndex)
                nativeIds = mutableListOf(anchorId)
            }
            val alreadyFollowing = nativeIds.size - 1
            installAnchor = anchor; installPrefix = 0; installTail = anchor + alreadyFollowing
            val followingStart = (anchor + 1 + alreadyFollowing).coerceAtMost(snapshot.size)
            for (chunk in snapshot.subList(followingStart, snapshot.size).chunked(48)) {
                val items = withContext(Dispatchers.Default) { chunk.map { it.toMediaItem() } }
                if (queue !== snapshot || released || controller !== p) return
                p.addMediaItems(p.mediaItemCount, items)
                nativeIds.addAll(chunk.map { it.id })
                installTail += chunk.size
                delay(16)
            }
            var prefixSize = 0
            for (chunk in snapshot.subList(0, anchor).chunked(48)) {
                val items = withContext(Dispatchers.Default) { chunk.map { it.toMediaItem() } }
                if (queue !== snapshot || released || controller !== p) return
                p.addMediaItems(prefixSize, items)
                nativeIds.addAll(prefixSize, chunk.map { it.id }); prefixSize += chunk.size
                installPrefix = prefixSize
                delay(16)
            }
            installedQueue = snapshot
        } finally {
            if (installingQueue === snapshot) installingQueue = null
        }
    }

    fun cacheLibrary(songs: List<Song>) { songs.forEach { knownSongs[it.id] = it } }
    fun currentQueue(): List<Song> = queue

    fun playNext(song: Song): List<Song> {
        val currentId = _state.value.currentSongId
        val currentPosition = queue.indexOfFirst { it.id == currentId }
        val without = queue.filterNot { it.id == song.id }.toMutableList()
        val updatedCurrent = without.indexOfFirst { it.id == currentId }
        val insert = if (updatedCurrent >= 0) updatedCurrent + 1 else if (currentPosition >= 0) currentPosition.coerceAtMost(without.size) else 0

        without.add(insert.coerceIn(0, without.size), song)
        setQueue(without)
        return queue
    }

    fun addToQueue(song: Song): List<Song> {
        if (queue.any { it.id == song.id }) return queue
        setQueue(queue + song)
        return queue
    }

    fun removeFromQueue(songId: Long): List<Song> {
        setQueue(queue.filterNot { it.id == songId })
        return queue
    }

    fun moveQueueItem(from: Int, to: Int): List<Song> {
        if (from !in queue.indices || to !in queue.indices || from == to) return queue
        lastQueueInput = null
        val updated = queue.toMutableList().apply { add(to, removeAt(from)) }
        val p = controller
        if (installedQueue === queue && p != null && p.mediaItemCount == queue.size) {
            queue = updated
            queuePositions = queue.withIndex().associate { it.value.id to it.index }
            p.moveMediaItem(from, to)
            nativeIds = queue.map { it.id }.toMutableList(); indexNative()
            installedQueue = queue
            publishState()
        } else setQueue(updated)
        return queue
    }

    fun restoreState(songId: Long?, positionMs: Long) {
        val id = songId ?: return
        restoredSongId = id
        val index = indexOf(id)
        val song = queue.getOrNull(index)

        _state.value = _state.value.copy(
            connected = controller != null,
            currentSongId = id,
            currentIndex = index,
            isPlaying = false,
            position = positionMs,
            duration = song?.duration ?: 0L
        )
    }

    fun play(song: Song) {
        com.xvox.music.split.XvoxSplitRepository.onTrackChanged(song.id, manual = true)
        QueuePopulationEpoch.invalidate()
        externalQueue = false
        if (indexOf(song.id) < 0) setQueue(queue + song)
        val p = controller
        if (p == null) {
            pendingPlay = song
            _state.value = _state.value.copy(currentSongId = song.id, currentIndex = queue.indexOf(song), duration = song.duration)
            return
        }
        startAt(indexOf(song.id), shouldPlay = true)
    }

    fun playQueueIndex(index: Int, keepPlayingState: Boolean = true) {
        val song = queue.getOrNull(index) ?: return
        com.xvox.music.split.XvoxSplitRepository.onTrackChanged(song.id, manual = true)
        val p = controller
        if (p == null) { pendingPlay = song; return }
        val shouldPlay = restoredSongId != null || !keepPlayingState || p.playWhenReady || _state.value.isPlaying
        startAt(index, shouldPlay)
    }

    private fun startAt(index: Int, shouldPlay: Boolean) {
        val p = controller ?: return
        val song = queue.getOrNull(index) ?: return
        restoredSongId = null
        val sameSong = p.currentMediaItem?.mediaId == song.id.toString() && p.playbackState != Player.STATE_ENDED
        var nativeIndex: Int? = when {
            installedQueue === queue && p.mediaItemCount == queue.size -> index
            installingQueue === queue && index < installAnchor && index < installPrefix -> index
            installingQueue === queue && index >= installAnchor && index <= installTail -> installPrefix + index - installAnchor
            else -> null
        }
        if (nativeIndex != null && (nativeIndex >= p.mediaItemCount || p.getMediaItemAt(nativeIndex).mediaId != song.id.toString())) nativeIndex = null
        if (sameSong) {
            if (p.playbackState == Player.STATE_IDLE) p.prepare()
            if (shouldPlay) p.play() else p.pause()
            // Repeated taps are feedback/resume, not a seek back to zero.
            publishState()
            return
        }
        if ((installedQueue === queue || installingQueue === queue) && nativeIndex != null) {
            p.seekTo(nativeIndex, 0)
        } else {
            queueJob?.cancel()
            // Start with the chosen song plus four successors, not thousands of media-item bundles.
            val seed = queue.subList(index, minOf(queue.size, index + 5))
            p.setMediaItems(seed.map { it.toMediaItem() }, 0, 0)
            nativeIds = seed.map { it.id }.toMutableList(); indexNative()
            installedQueue = null
            queueJob = scope.launch { yield(); installAround(p, song.id, preserveCurrent = false) }
        }
        if (p.playbackState == Player.STATE_IDLE || p.playbackState == Player.STATE_ENDED) p.prepare()
        if (shouldPlay) p.play() else p.pause()
        _state.value = PlaybackState(true, song.id, index, shouldPlay, 0L, song.duration)
    }

    fun playPrevious() {
        val index = _state.value.currentIndex
        if (queue.isEmpty() || index < 0) return
        val atFirst = index <= 0
        if (atFirst && repeatMode == RepeatMode.OFF) return
        val target = if (atFirst && repeatMode == RepeatMode.ALL) queue.lastIndex else index - 1
        playQueueIndex(target, true)
    }

    fun playNext() {
        val index = _state.value.currentIndex
        if (queue.isEmpty() || index < 0) return
        val atLast = index >= queue.lastIndex
        if (atLast && repeatMode == RepeatMode.OFF) return
        val target = if (atLast && repeatMode == RepeatMode.ALL) 0 else index + 1
        playQueueIndex(target, true)
    }

    fun seekTo(positionMs: Long) {
        val mediaController = controller ?: return
        if (mediaController.currentMediaItem == null) return
        val duration = mediaController.duration.takeIf { it > 0L }
        mediaController.seekTo(if (duration != null) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L))
        publishState()
    }

    fun togglePlay() {
        val restoredId = restoredSongId
        if (restoredId != null) {
            val index = queue.indexOfFirst { it.id == restoredId }
            if (index >= 0) playQueueIndex(index, true)
            return
        }

        val mediaController = controller ?: return
        if (mediaController.currentMediaItem == null) return

        if (mediaController.playWhenReady && mediaController.playbackState != Player.STATE_ENDED && mediaController.playbackState != Player.STATE_IDLE) {
            mediaController.pause()
            expectedPlaying = false
            expectedPlayingUntil = System.currentTimeMillis() + 400
        } else {
            if (mediaController.playbackState == Player.STATE_ENDED) mediaController.seekToDefaultPosition()
            if (mediaController.playbackState == Player.STATE_IDLE) mediaController.prepare()
            mediaController.play()
            expectedPlaying = true
            expectedPlayingUntil = System.currentTimeMillis() + 800
        }
        publishState()
    }

    fun stop() {
        QueuePopulationEpoch.invalidate()
        restoredSongId = null
        pendingPlay = null
        queueJob?.cancel(); installedQueue = null; installingQueue = null
        nativeIds.clear(); indexNative()

        controller?.let {
            it.stop()
            it.clearMediaItems()
        }

        _state.value = PlaybackState(connected = controller != null)
        XvoxAppWidgetProvider.updateAllWidgets(appContext, null, false, 0L, 0L)
    }

    private fun publishState() {
        val mediaController = controller
        if (mediaController == null) {
            if (restoredSongId == null) _state.value = PlaybackState()
            return
        }

        val id = mediaController.currentMediaItem?.mediaId?.toLongOrNull()
        if (id == null) {
            val restored = restoredSongId
            if (restored != null) {
                val index = indexOf(restored)
                val song = queue.getOrNull(index)
                if (song != null) {
                    _state.value = PlaybackState(
                        connected = true,
                        currentSongId = song.id,
                        currentIndex = index,
                        duration = song.duration
                    )
                    return
                }
            }
            _state.value = PlaybackState(connected = true)
            return
        }

        restoredSongId = null
        val index = indexOf(id)
        val currentSong = queue.getOrNull(index)
        val fallbackDuration = currentSong?.duration ?: 0L

        val effectiveIsPlaying = mediaController.isPlaying ||
            (mediaController.playWhenReady && mediaController.playbackState == Player.STATE_BUFFERING && mediaController.playerError == null)

        val currentPos = mediaController.currentPosition.coerceAtLeast(0L)
        val currentDur = mediaController.duration.takeIf { it > 0L } ?: fallbackDuration

        _state.value = PlaybackState(
            connected = true,
            currentSongId = id,
            currentIndex = index,
            isPlaying = effectiveIsPlaying,
            position = currentPos,
            duration = currentDur,
            queue = queue,
            externalQueue = externalQueue
        )

    }

    fun release() {
        released = true
        if (activeInstance === this) activeInstance = null
        progressJob?.cancel()
        queueJob?.cancel()
        pendingPlay = null
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        scope.cancel()
    }
}
