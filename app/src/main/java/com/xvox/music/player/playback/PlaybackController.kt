package com.xvox.music.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
    private var fadeJob: Job? = null
    private var crossfadeTriggeredForSongId: Long? = null

    private var restoredSongId: Long? = null
    private var repeatMode: RepeatMode = RepeatMode.OFF

    private var expectedPlaying: Boolean? = null
    private var expectedPlayingUntil: Long = 0L

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                handleTrackEnded()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            crossfadeTriggeredForSongId = null
            publishState()
        }
    }

    init {
        activeInstance = this
        connect()
    }

    private fun handleTrackEnded() {
        val curIdx = _state.value.currentIndex
        when (repeatMode) {
            RepeatMode.ONE -> {
                controller?.let {
                    it.seekTo(0)
                    it.play()
                    expectedPlaying = true
                    expectedPlayingUntil = System.currentTimeMillis() + 1500
                    publishState()
                }
            }
            RepeatMode.ALL -> {
                if (queue.isNotEmpty()) {
                    val nextIdx = if (curIdx < 0) 0 else (curIdx + 1) % queue.size
                    playQueueIndex(nextIdx, true)
                }
            }
            RepeatMode.OFF -> {
                if (queue.isNotEmpty() && curIdx >= 0 && curIdx < queue.lastIndex) {
                    playQueueIndex(curIdx + 1, true)
                } else {
                    scope.launch {
                        val shouldClear = prefs.clearQueueAfterPlayback.first()
                        if (shouldClear) {
                            stop()
                        } else {
                            expectedPlaying = false
                            expectedPlayingUntil = System.currentTimeMillis() + 800
                            publishState()
                        }
                    }
                }
            }
        }
    }

    private fun connect() {
        val token = SessionToken(
            appContext,
            ComponentName(appContext, XvoxPlaybackService::class.java)
        )

        val future = MediaController.Builder(appContext, token).buildAsync()

        future.addListener(
            {
                runCatching {
                    future.get()
                }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    publishState()

                    progressJob = scope.launch {
                        while (isActive) {
                            publishState()
                            checkCrossfadeAndAdvance()
                            delay(350L)
                        }
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun checkCrossfadeAndAdvance() {
        val mediaController = controller ?: return
        if (!mediaController.isPlaying) return

        val duration = mediaController.duration
        val position = mediaController.currentPosition
        if (duration <= 0L || position <= 0L) return

        val remainingMs = duration - position
        val currentId = _state.value.currentSongId ?: return

        scope.launch {
            val isCrossfade = prefs.crossfade.first()
            val crossfadeDurationSec = prefs.crossfadeDuration.first()
            val crossfadeThreshold = (crossfadeDurationSec * 1000L).coerceIn(1500L, 12000L)

            if (isCrossfade && remainingMs <= crossfadeThreshold && crossfadeTriggeredForSongId != currentId) {
                crossfadeTriggeredForSongId = currentId
                val curIdx = _state.value.currentIndex
                val hasNext = when (repeatMode) {
                    RepeatMode.ALL, RepeatMode.ONE -> queue.isNotEmpty()
                    RepeatMode.OFF -> curIdx >= 0 && curIdx < queue.lastIndex
                }
                if (hasNext) {
                    val nextIdx = when (repeatMode) {
                        RepeatMode.ALL -> (curIdx + 1) % queue.size
                        RepeatMode.ONE -> curIdx
                        RepeatMode.OFF -> curIdx + 1
                    }
                    performCrossfadeTransition(nextIdx, crossfadeThreshold)
                }
            }
        }
    }

    private fun performCrossfadeTransition(targetIndex: Int, durationMs: Long) {
        val mediaController = controller ?: return
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 12
            val stepDelay = durationMs / (steps * 2)
            val masterVol = (prefs.appVolume.first() * prefs.volumeLimit.first()).coerceIn(0.1f, 1f)

            for (i in (steps - 1) downTo 2) {
                val fraction = i.toFloat() / steps.toFloat()
                mediaController.volume = (masterVol * fraction).coerceIn(0f, 1f)
                delay(stepDelay)
            }

            // Advance to target track
            playQueueIndex(targetIndex, true)

            // Fade in new track
            for (i in 2..steps) {
                val fraction = i.toFloat() / steps.toFloat()
                mediaController.volume = (masterVol * fraction).coerceIn(0f, 1f)
                delay(stepDelay)
            }
            mediaController.volume = masterVol
        }
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

        val insert = if (updatedCurrent >= 0) {
            updatedCurrent + 1
        } else if (currentPosition >= 0) {
            currentPosition.coerceAtMost(without.size)
        } else {
            0
        }

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

    fun restoreSong(songId: Long) {
        val mediaController = controller
        val liveId = mediaController?.currentMediaItem?.mediaId?.toLongOrNull()
        if (liveId != null) return

        val index = queue.indexOfFirst { it.id == songId }
        val song = queue.getOrNull(index) ?: return

        restoredSongId = song.id
        _state.value = PlaybackState(
            connected = mediaController != null,
            currentSongId = song.id,
            currentIndex = index,
            isPlaying = false,
            position = 0L,
            duration = song.duration
        )

        XvoxAppWidgetProvider.updateAllWidgets(appContext, song, false, 0L, song.duration)
    }

    fun play(song: Song) {
        val mediaController = controller ?: return
        val liveId = mediaController.currentMediaItem?.mediaId

        if (liveId == song.id.toString() && restoredSongId == null) {
            togglePlay()
            return
        }

        restoredSongId = null
        crossfadeTriggeredForSongId = null

        var index = queue.indexOfFirst { it.id == song.id }
        if (index < 0) {
            queue = queue + song
            index = queue.lastIndex
        }

        scope.launch {
            val isGapless = prefs.gaplessPlayback.first()
            val isFadeIn = prefs.fadeIn.first()
            val masterVol = (prefs.appVolume.first() * prefs.volumeLimit.first()).coerceIn(0.1f, 1f)

            if (isGapless && queue.size > 1) {
                val mediaItems = queue.map { it.toMediaItem() }
                mediaController.setMediaItems(mediaItems, index, 0L)
            } else {
                mediaController.setMediaItem(song.toMediaItem())
            }

            mediaController.prepare()

            if (isFadeIn) {
                mediaController.volume = 0f
                mediaController.play()
                val steps = 10
                for (i in 1..steps) {
                    delay(80L)
                    mediaController.volume = (masterVol * (i.toFloat() / steps)).coerceIn(0f, 1f)
                }
                mediaController.volume = masterVol
            } else {
                mediaController.volume = masterVol
                mediaController.play()
            }
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

        val shouldPlay = if (restoredSongId != null) {
            true
        } else if (keepPlayingState) {
            _state.value.isPlaying || mediaController.isPlaying || mediaController.playWhenReady
        } else {
            true
        }

        restoredSongId = null
        crossfadeTriggeredForSongId = null
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
            if (shouldPlay) {
                mediaController.play()
            } else {
                mediaController.pause()
            }
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
        mediaController.seekTo(
            if (duration != null) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        )
        publishState()
    }

    fun togglePlay() {
        val restoredId = restoredSongId
        if (restoredId != null) {
            val index = queue.indexOfFirst { it.id == restoredId }
            if (index >= 0) {
                playQueueIndex(index, true)
            }
            return
        }

        val mediaController = controller ?: return
        if (mediaController.currentMediaItem == null) return

        if (mediaController.isPlaying) {
            scope.launch {
                val isFadeOut = prefs.fadeOut.first()
                val masterVol = (prefs.appVolume.first() * prefs.volumeLimit.first()).coerceIn(0.1f, 1f)
                if (isFadeOut) {
                    val steps = 8
                    for (i in (steps - 1) downTo 0) {
                        delay(40L)
                        mediaController.volume = (masterVol * (i.toFloat() / steps)).coerceIn(0f, 1f)
                    }
                    mediaController.pause()
                    mediaController.volume = masterVol
                } else {
                    mediaController.pause()
                }
            }
            expectedPlaying = false
            expectedPlayingUntil = System.currentTimeMillis() + 400
        } else {
            scope.launch {
                val isFadeIn = prefs.fadeIn.first()
                val masterVol = (prefs.appVolume.first() * prefs.volumeLimit.first()).coerceIn(0.1f, 1f)
                if (isFadeIn) {
                    mediaController.volume = 0f
                    mediaController.play()
                    val steps = 8
                    for (i in 1..steps) {
                        delay(60L)
                        mediaController.volume = (masterVol * (i.toFloat() / steps)).coerceIn(0f, 1f)
                    }
                    mediaController.volume = masterVol
                } else {
                    mediaController.volume = masterVol
                    mediaController.play()
                }
            }
            expectedPlaying = true
            expectedPlayingUntil = System.currentTimeMillis() + 800
        }
        publishState()
    }

    fun stop() {
        restoredSongId = null
        crossfadeTriggeredForSongId = null
        fadeJob?.cancel()

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
            if (restoredSongId == null) {
                _state.value = PlaybackState()
            }
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
        if (!isExpectedActive && expectedPlaying != null) {
            expectedPlaying = null
        }
        val rawIsPlaying = mediaController.isPlaying
        if (rawIsPlaying) {
            expectedPlaying = null
        }
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

        XvoxAppWidgetProvider.updateAllWidgets(
            appContext,
            currentSong,
            effectiveIsPlaying,
            currentPos,
            currentDur
        )
    }

    fun release() {
        progressJob?.cancel()
        fadeJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        scope.cancel()
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()
}
