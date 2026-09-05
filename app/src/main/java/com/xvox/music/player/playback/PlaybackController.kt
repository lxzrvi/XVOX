package com.xvox.music.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
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
import kotlinx.coroutines.flow.first
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
        var activeInstance: PlaybackController? = null
    }

    private val appContext = context.applicationContext
    private val prefs = UserPreferencesRepository(appContext)
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

    private val transitionHelper = PlaybackTrackTransitionHelper(
        scope = scope,
        prefs = prefs,
        getController = { controller },
        getQueue = { queue },
        getRepeatMode = { repeatMode },
        playQueueIndex = { idx, keep -> playQueueIndex(idx, keep) },
        onExpectedPlaying = { playing, duration ->
            expectedPlaying = playing
            expectedPlayingUntil = System.currentTimeMillis() + duration
            publishState()
        },
        stop = { stop() }
    )

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                transitionHelper.handleTrackEnded(_state.value.currentIndex)
            }
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            transitionHelper.crossfadeTriggeredForSongId = null
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
                    controller = mediaController
                    mediaController.addListener(listener)
                    publishState()

                    progressJob = scope.launch {
                        while (isActive) {
                            publishState()
                            transitionHelper.checkCrossfadeAndAdvance(
                                _state.value.currentSongId,
                                _state.value.currentIndex
                            )
                            delay(350L)
                        }
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        controller?.repeatMode = Player.REPEAT_MODE_OFF
        publishState()
    }

    fun setQueue(songs: List<Song>) {
        queue = songs
        val mediaController = controller
        if (mediaController != null && songs.isNotEmpty()) {
            val curId = _state.value.currentSongId
            val curIndex = songs.indexOfFirst { it.id == curId }.coerceAtLeast(0)
            val mediaItems = songs.map { it.toMediaItem() }
            mediaController.setMediaItems(mediaItems, curIndex, mediaController.currentPosition)
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
        queue = without
        publishState()
        return queue
    }

    fun addToQueue(song: Song): List<Song> {
        if (queue.any { it.id == song.id }) return queue
        queue = queue + song
        publishState()
        return queue
    }

    fun removeFromQueue(songId: Long): List<Song> {
        queue = queue.filterNot { it.id == songId }
        publishState()
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
        val mediaController = controller ?: return
        restoredSongId = null
        transitionHelper.reset()

        var index = queue.indexOfFirst { it.id == song.id }
        if (index < 0) {
            queue = queue + song
            index = queue.lastIndex
        }

        scope.launch {
            val isGapless = prefs.gaplessPlayback.first()
            if (isGapless && queue.size > 1) {
                val mediaItems = queue.map { it.toMediaItem() }
                mediaController.setMediaItems(mediaItems, index, 0L)
            } else {
                mediaController.setMediaItem(song.toMediaItem())
            }
            mediaController.prepare()
            PlaybackVolumeFadeHelper.applyFadeIn(mediaController, prefs, steps = 10)
        }

        expectedPlaying = true
        expectedPlayingUntil = System.currentTimeMillis() + 1800

        _state.value = _state.value.copy(
            connected = true,
            currentSongId = song.id,
            currentIndex = index,
            isPlaying = true,
            position = 0L,
            duration = song.duration
        )

        XvoxAppWidgetProvider.updateAllWidgets(appContext, song, true, 0L, song.duration)
    }

    fun playQueueIndex(index: Int, keepPlayingState: Boolean = true) {
        val song = queue.getOrNull(index) ?: return
        val mediaController = controller ?: return

        val shouldPlay = if (restoredSongId != null) true else if (keepPlayingState) _state.value.isPlaying || mediaController.isPlaying || mediaController.playWhenReady else true

        restoredSongId = null
        transitionHelper.reset()
        expectedPlaying = shouldPlay
        expectedPlayingUntil = System.currentTimeMillis() + 1800

        scope.launch {
            val isGapless = prefs.gaplessPlayback.first()
            if (isGapless && queue.size > 1) {
                val mediaItems = queue.map { it.toMediaItem() }
                mediaController.setMediaItems(mediaItems, index, 0L)
            } else {
                mediaController.setMediaItem(song.toMediaItem())
            }
            mediaController.prepare()
            if (shouldPlay) mediaController.play() else mediaController.pause()
        }

        _state.value = _state.value.copy(
            connected = true,
            currentSongId = song.id,
            currentIndex = index,
            isPlaying = shouldPlay,
            position = 0L,
            duration = song.duration
        )

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
            scope.launch { PlaybackVolumeFadeHelper.applyFadeOutAndPause(mediaController, prefs) }
            expectedPlaying = false
            expectedPlayingUntil = System.currentTimeMillis() + 400
        } else {
            scope.launch { PlaybackVolumeFadeHelper.applyFadeIn(mediaController, prefs) }
            expectedPlaying = true
            expectedPlayingUntil = System.currentTimeMillis() + 800
        }
        publishState()
    }

    fun stop() {
        restoredSongId = null
        transitionHelper.reset()

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
        progressJob?.cancel()
        transitionHelper.reset()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        scope.cancel()
    }
}
