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

data class PlaybackState(
    val connected: Boolean = false,
    val currentSongId: Long? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L
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
    private var progressJob: Job? = null

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
                    setRepeatMode(repeatMode)
                    val pending = pendingPlay
                    pendingPlay = null
                    if (pending != null) play(pending) else publishState()

                    progressJob = scope.launch {
                        while (isActive) {
                            publishState()
                            delay(250L)
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
        val unique = songs.distinctBy { it.id }
        if (queue == unique) return
        queue = unique
        val p = controller
        // A queue update must not prepare/start the first track before a restored song or first tap.
        if (p != null && p.mediaItemCount > 0) {
            queue.forEachIndexed { index, song ->
                val existing = (0 until p.mediaItemCount).firstOrNull { p.getMediaItemAt(it).mediaId == song.id.toString() }
                if (existing == null) p.addMediaItem(index, song.toMediaItem())
                else if (existing != index) p.moveMediaItem(existing, index)
            }
            if (p.mediaItemCount > queue.size) p.removeMediaItems(queue.size, p.mediaItemCount)
        }
        publishState()
    }

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
        val mutable = queue.toMutableList()
        val moved = mutable.removeAt(from)
        mutable.add(to, moved)
        queue = mutable
        controller?.moveMediaItem(from, to)
        publishState()
        return queue
    }

    fun restoreState(songId: Long?, positionMs: Long) {
        val id = songId ?: return
        restoredSongId = id
        val index = queue.indexOfFirst { it.id == id }
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
        if (queue.none { it.id == song.id }) queue = queue + song
        val p = controller
        if (p == null) {
            pendingPlay = song
            _state.value = _state.value.copy(currentSongId = song.id, currentIndex = queue.indexOf(song), duration = song.duration)
            return
        }
        startAt(queue.indexOfFirst { it.id == song.id }, shouldPlay = true)
    }

    fun playQueueIndex(index: Int, keepPlayingState: Boolean = true) {
        val song = queue.getOrNull(index) ?: return
        val p = controller
        if (p == null) { pendingPlay = song; return }
        val shouldPlay = restoredSongId != null || !keepPlayingState || p.playWhenReady || _state.value.isPlaying
        startAt(index, shouldPlay)
    }

    private fun startAt(index: Int, shouldPlay: Boolean) {
        val p = controller ?: return
        val song = queue.getOrNull(index) ?: return
        restoredSongId = null
        expectedPlaying = shouldPlay
        expectedPlayingUntil = System.currentTimeMillis() + 1800
        val sameQueue = p.mediaItemCount == queue.size && queue.indices.all { p.getMediaItemAt(it).mediaId == queue[it].id.toString() }
        if (sameQueue) p.seekTo(index, 0L) else p.setMediaItems(queue.map { it.toMediaItem() }, index, 0L)
        p.prepare()
        if (shouldPlay) p.play() else p.pause()
        _state.value = PlaybackState(true, song.id, index, shouldPlay, 0L, song.duration)
        XvoxAppWidgetProvider.updateAllWidgets(appContext, song, shouldPlay, 0L, song.duration)
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

        if (mediaController.isPlaying) {
            mediaController.pause()
            expectedPlaying = false
            expectedPlayingUntil = System.currentTimeMillis() + 400
        } else {
            if (mediaController.playbackState == Player.STATE_ENDED) mediaController.seekToDefaultPosition()
            mediaController.play()
            expectedPlaying = true
            expectedPlayingUntil = System.currentTimeMillis() + 800
        }
        publishState()
    }

    fun stop() {
        restoredSongId = null
        pendingPlay = null

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
                val index = queue.indexOfFirst { it.id == restored }
                val song = queue.getOrNull(index)
                if (song != null) {
                    _state.value = PlaybackState(
                        connected = true,
                        currentSongId = song.id,
                        currentIndex = index,
                        duration = song.duration
                    )
                    XvoxAppWidgetProvider.updateAllWidgets(appContext, song, false, 0L, song.duration)
                    return
                }
            }
            _state.value = PlaybackState(connected = true)
            return
        }

        restoredSongId = null
        val index = queue.indexOfFirst { it.id == id }
        val currentSong = queue.getOrNull(index)
        val fallbackDuration = currentSong?.duration ?: 0L

        val now = System.currentTimeMillis()
        val isExpectedActive = expectedPlaying != null && now < expectedPlayingUntil
        if (!isExpectedActive && expectedPlaying != null) expectedPlaying = null
        val rawIsPlaying = mediaController.isPlaying
        if (rawIsPlaying) expectedPlaying = null
        val effectiveIsPlaying = when {
            rawIsPlaying -> true
            isExpectedActive -> expectedPlaying!!
            else -> mediaController.playWhenReady && mediaController.playbackState != Player.STATE_ENDED && mediaController.playbackState != Player.STATE_IDLE
        }

        val currentPos = mediaController.currentPosition.coerceAtLeast(0L)
        val currentDur = mediaController.duration.takeIf { it > 0L } ?: fallbackDuration

        _state.value = PlaybackState(
            connected = true,
            currentSongId = id,
            currentIndex = index,
            isPlaying = effectiveIsPlaying,
            position = currentPos,
            duration = currentDur
        )

        XvoxAppWidgetProvider.updateAllWidgets(appContext, currentSong, effectiveIsPlaying, currentPos, currentDur)
    }

    fun release() {
        released = true
        if (activeInstance === this) activeInstance = null
        progressJob?.cancel()
        pendingPlay = null
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        scope.cancel()
    }
}
