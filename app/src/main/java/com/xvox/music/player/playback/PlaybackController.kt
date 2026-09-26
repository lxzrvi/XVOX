package com.xvox.music.player.playback

import android.content.ComponentName
import android.content.Context
import java.util.ArrayDeque
import java.util.IdentityHashMap
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

    // Song.id identifies a library file, not an occurrence in the play queue. Every occurrence
    // receives a private token used as Media3's mediaId so repeated additions remain independent.
    private var queueEntryIds: List<String> = emptyList()
    private var entryPositions: Map<String, Int> = emptyMap()
    private var nextEntrySerial = 0L
    private var nativeEntryIds = mutableListOf<String>()
    private var installedQueue: List<Song>? = null
    private var installingQueue: List<Song>? = null
    private var installingEntryIds: List<String>? = null
    private var installAnchor = -1
    private var installPrefix = 0
    private var installTail = -1

    private fun indexOfEntry(entryId: String?): Int = entryId?.let { entryPositions[it] } ?: -1
    private fun indexOfSong(songId: Long?): Int = songId?.let { id -> queue.indexOfFirst { it.id == id } } ?: -1
    private fun entryIdAt(index: Int): String? = queueEntryIds.getOrNull(index)

    /**
     * Preserves a token by object identity first, then by same-song occurrence order. New copies
     * receive fresh tokens. The identity pass matters when a queue reorders two equal Song data
     * objects: the audible occurrence must move with its row rather than jump to its twin.
     */
    private fun entryIdsFor(nextQueue: List<Song>): List<String> {
        val oldBySong = queue.indices.groupBy { queue[it].id }
            .mapValues { (_, indices) -> ArrayDeque(indices.mapNotNull { index -> queueEntryIds.getOrNull(index) }) }
            .toMutableMap()
        val oldByReference = IdentityHashMap<Song, ArrayDeque<String>>()
        queue.forEachIndexed { index, song ->
            val entry = queueEntryIds.getOrNull(index) ?: return@forEachIndexed
            val referenceBucket = oldByReference[song] ?: ArrayDeque<String>().also { oldByReference[song] = it }
            referenceBucket.addLast(entry)
        }
        val assigned = MutableList<String?>(nextQueue.size) { null }

        // Allocate rows that are literally the old objects before consuming a same-ID fallback.
        nextQueue.forEachIndexed { index, song ->
            val referenceBucket = oldByReference[song]
            val entry = if (referenceBucket == null || referenceBucket.isEmpty()) null else referenceBucket.removeFirst()
            if (entry != null) {
                assigned[index] = entry
                oldBySong[song.id]?.removeFirstOccurrence(entry)
            }
        }

        return nextQueue.indices.map { index ->
            assigned[index] ?: run {
                val song = nextQueue[index]
                val bucket = oldBySong[song.id]
                val existing = if (bucket == null || bucket.isEmpty()) null else bucket.removeFirst()
                // Include a monotonic clock component so a service-reconnected queue cannot
                // collide with tokens allocated by an earlier controller instance.
                existing ?: "xvox:${song.id}:${++nextEntrySerial}:${System.nanoTime()}"
            }
        }
    }

    private fun indexEntries() {
        entryPositions = queueEntryIds.withIndex().associate { it.value to it.index }
    }


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
        // Structural Song equality cannot distinguish two independently queued copies. Only a
        // position-for-position identity match means this is genuinely the same occurrence list.
        if (songs.size == queue.size && songs.indices.all { index -> songs[index] === queue[index] }) return

        songs.forEach { knownSongs[it.id] = it }
        val nextEntryIds = entryIdsFor(songs)
        val currentEntry = controller?.currentMediaItem?.mediaId
        queue = songs.toList()
        queueEntryIds = nextEntryIds
        indexEntries()
        externalQueue = false
        installedQueue = null
        installingQueue = null
        installingEntryIds = null
        queueJob?.cancel()

        val p = controller
        if (p != null && currentEntry != null && indexOfEntry(currentEntry) >= 0) {
            queueJob = scope.launch {
                yield() // A tap may immediately choose a different song; don't serialize the old queue first.
                installAround(p, currentEntry, preserveCurrent = true)
            }
        }
        publishState()
    }

    private fun adoptNativeQueue() {
        lastQueueInput = null
        val p = controller ?: return
        if (p.mediaItemCount == 0) return
        val nativeIds = List(p.mediaItemCount) { index ->
            p.getMediaItemAt(index).mediaId.ifBlank { "external:$index:${++nextEntrySerial}" }
        }
        queue = List(p.mediaItemCount) { index ->
            val item = p.getMediaItemAt(index)
            // External controllers may still use a bare library ID. Internal XVOX occurrence
            // tokens carry their original ID in metadata so service reconnects retain duplicates.
            val extras = item.mediaMetadata.extras
            val id = if (extras?.containsKey("xvox_song_id") == true) {
                extras.getLong("xvox_song_id")
            } else {
                item.mediaId.toLongOrNull() ?: -index.toLong() - 1
            }
            knownSongs[id] ?: Song(id, item.mediaMetadata.title?.toString() ?: "Unknown song",
                item.mediaMetadata.artist?.toString() ?: "Unknown artist",
                item.mediaMetadata.extras?.getString("xvox_original_uri")?.let(android.net.Uri::parse) ?: item.localConfiguration?.uri ?: android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                item.mediaMetadata.artworkUri, if (index == p.currentMediaItemIndex) p.duration.coerceAtLeast(0) else 0)
        }
        queueEntryIds = nativeIds
        indexEntries()
        nativeEntryIds = nativeIds.toMutableList()
        installedQueue = queue
    }

    /** Keep the audible occurrence, then populate the timeline in small, cancellable batches. */
    private suspend fun installAround(p: MediaController, anchorEntryId: String, preserveCurrent: Boolean) {
        val snapshot = queue
        val entrySnapshot = queueEntryIds
        val anchor = entrySnapshot.indexOf(anchorEntryId)
        if (anchor < 0 || released || controller !== p) return
        installingQueue = snapshot
        installingEntryIds = entrySnapshot
        try {
            if (preserveCurrent) {
                val activeIndex = p.currentMediaItemIndex
                if (p.currentMediaItem?.mediaId != anchorEntryId) return
                if (activeIndex + 1 < p.mediaItemCount) p.removeMediaItems(activeIndex + 1, p.mediaItemCount)
                if (activeIndex > 0) p.removeMediaItems(0, activeIndex)
                nativeEntryIds = mutableListOf(anchorEntryId)
            }
            val alreadyFollowing = nativeEntryIds.size - 1
            installAnchor = anchor; installPrefix = 0; installTail = anchor + alreadyFollowing
            val followingStart = (anchor + 1 + alreadyFollowing).coerceAtMost(snapshot.size)
            for (chunk in (followingStart until snapshot.size).toList().chunked(48)) {
                val items = withContext(Dispatchers.Default) {
                    chunk.map { index -> snapshot[index].toMediaItem(entrySnapshot[index]) }
                }
                if (queue !== snapshot || queueEntryIds != entrySnapshot || released || controller !== p) return
                p.addMediaItems(p.mediaItemCount, items)
                nativeEntryIds.addAll(chunk.map { index -> entrySnapshot[index] })
                installTail += chunk.size
                delay(16)
            }
            var prefixSize = 0
            for (chunk in (0 until anchor).toList().chunked(48)) {
                val items = withContext(Dispatchers.Default) {
                    chunk.map { index -> snapshot[index].toMediaItem(entrySnapshot[index]) }
                }
                if (queue !== snapshot || queueEntryIds != entrySnapshot || released || controller !== p) return
                p.addMediaItems(prefixSize, items)
                nativeEntryIds.addAll(prefixSize, chunk.map { index -> entrySnapshot[index] }); prefixSize += chunk.size
                installPrefix = prefixSize
                delay(16)
            }
            installedQueue = snapshot
        } finally {
            if (installingQueue === snapshot) installingQueue = null
            if (installingEntryIds === entrySnapshot) installingEntryIds = null
        }
    }

    fun cacheLibrary(songs: List<Song>) { songs.forEach { knownSongs[it.id] = it } }
    fun currentQueue(): List<Song> = queue

    fun playNext(song: Song): List<Song> {
        val active = _state.value.currentIndex.takeIf { it in queue.indices }
            ?: indexOfEntry(controller?.currentMediaItem?.mediaId)
        val updated = queue.toMutableList()
        updated.add((active + 1).coerceIn(0, updated.size), song.copy())
        setQueue(updated)
        return queue
    }

    fun addToQueue(song: Song): List<Song> {
        // Appending is deliberately occurrence-preserving: identical songs are independent rows.
        setQueue(queue + song.copy())
        return queue
    }

    fun removeFromQueue(songId: Long): List<Song> {
        setQueue(queue.filterNot { it.id == songId })
        return queue
    }

    fun moveQueueItem(from: Int, to: Int): List<Song> {
        if (from !in queue.indices || to !in queue.indices || from == to) return queue
        lastQueueInput = null
        val oldQueue = queue
        val oldEntries = queueEntryIds
        val updated = oldQueue.toMutableList().apply { add(to, removeAt(from)) }
        val updatedEntries = oldEntries.toMutableList().apply { add(to, removeAt(from)) }
        queue = updated
        queueEntryIds = updatedEntries
        indexEntries()
        val p = controller
        if (installedQueue === oldQueue && p != null && p.mediaItemCount == queue.size) {
            p.moveMediaItem(from, to)
            nativeEntryIds = updatedEntries.toMutableList()
            installedQueue = queue
        } else {
            installedQueue = null
            queueJob?.cancel()
            val currentEntry = p?.currentMediaItem?.mediaId
            if (p != null && currentEntry != null && indexOfEntry(currentEntry) >= 0) {
                queueJob = scope.launch { yield(); installAround(p, currentEntry, preserveCurrent = true) }
            }
        }
        val newIdx = indexOfEntry(p?.currentMediaItem?.mediaId)
        _state.value = _state.value.copy(currentIndex = if (newIdx >= 0) newIdx else _state.value.currentIndex)
        publishState()
        return queue
    }

    fun restoreState(songId: Long?, positionMs: Long) {
        val id = songId ?: return
        restoredSongId = id
        val index = indexOfSong(id)
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
        QueuePopulationEpoch.invalidate()
        externalQueue = false
        var index = indexOfSong(song.id)
        if (index < 0) {
            setQueue(queue + song)
            index = queue.lastIndex
        }
        val p = controller
        if (p == null) {
            pendingPlay = song
            _state.value = _state.value.copy(currentSongId = song.id, currentIndex = index, duration = song.duration)
            return
        }
        startAt(index, shouldPlay = true)
    }

    fun playQueueIndex(index: Int, keepPlayingState: Boolean = true) {
        val song = queue.getOrNull(index) ?: return
        val p = controller
        if (p == null) { pendingPlay = song; return }
        val isCurrentlyPlaying = _state.value.isPlaying && p.playWhenReady
        val shouldPlay = if (keepPlayingState) isCurrentlyPlaying else (restoredSongId == null)
        startAt(index, shouldPlay)
    }

    private fun startAt(index: Int, shouldPlay: Boolean) {
        val p = controller ?: return
        val song = queue.getOrNull(index) ?: return
        val entryId = entryIdAt(index) ?: return
        restoredSongId = null
        val sameOccurrence = p.currentMediaItem?.mediaId == entryId && _state.value.currentIndex == index && p.playbackState != Player.STATE_ENDED
        var nativeIndex: Int? = when {
            installedQueue === queue && p.mediaItemCount == queue.size -> index
            installingQueue === queue && installingEntryIds == queueEntryIds && index < installAnchor && index < installPrefix -> index
            installingQueue === queue && installingEntryIds == queueEntryIds && index >= installAnchor && index <= installTail -> installPrefix + index - installAnchor
            else -> null
        }
        if (nativeIndex != null && (nativeIndex >= p.mediaItemCount || p.getMediaItemAt(nativeIndex).mediaId != entryId)) nativeIndex = null
        if (sameOccurrence) {
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
            // Start with the chosen occurrence plus four successors, not thousands of bundles.
            val seedIndices = (index until minOf(queue.size, index + 5)).toList()
            p.setMediaItems(seedIndices.map { itemIndex -> queue[itemIndex].toMediaItem(queueEntryIds[itemIndex]) }, 0, 0)
            nativeEntryIds = seedIndices.map { itemIndex -> queueEntryIds[itemIndex] }.toMutableList()
            installedQueue = null
            installingQueue = null
            installingEntryIds = null
            queueJob = scope.launch { yield(); installAround(p, entryId, preserveCurrent = false) }
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
        queueJob?.cancel(); installedQueue = null; installingQueue = null; installingEntryIds = null
        nativeEntryIds.clear()

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

        val currentEntry = mediaController.currentMediaItem?.mediaId
        if (currentEntry.isNullOrBlank()) {
            val restored = restoredSongId
            if (restored != null) {
                val index = indexOfSong(restored)
                val song = queue.getOrNull(index)
                if (song != null) {
                    _state.value = PlaybackState(
                        connected = true,
                        currentSongId = song.id,
                        currentIndex = index,
                        duration = song.duration,
                        queue = queue,
                        externalQueue = externalQueue
                    )
                    return
                }
            }
            _state.value = PlaybackState(connected = true)
            return
        }

        restoredSongId = null
        val index = indexOfEntry(currentEntry)
        val currentSong = queue.getOrNull(index)
        val fallbackDuration = currentSong?.duration ?: 0L

        val effectiveIsPlaying = mediaController.isPlaying ||
            (mediaController.playWhenReady && mediaController.playbackState == Player.STATE_BUFFERING && mediaController.playerError == null)

        val currentPos = mediaController.currentPosition.coerceAtLeast(0L)
        val currentDur = mediaController.duration.takeIf { it > 0L } ?: fallbackDuration

        _state.value = PlaybackState(
            connected = true,
            currentSongId = currentSong?.id,
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
