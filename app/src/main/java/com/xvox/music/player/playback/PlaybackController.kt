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
    private val appContext =
        context.applicationContext

    private val prefs =
        UserPreferencesRepository(appContext)

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate
        )

    private var controller:
        MediaController? = null

    private var queue:
        List<Song> = emptyList()

    private var progressJob: Job? = null

    private var restoredSongId:
        Long? = null

    private var repeatMode: RepeatMode = RepeatMode.OFF

    private var expectedPlaying: Boolean? = null
    private var expectedPlayingUntil: Long = 0L

    private val _state =
        MutableStateFlow(
            PlaybackState()
        )

    val state:
        StateFlow<PlaybackState> =
        _state.asStateFlow()

    private val listener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events
            ) {
                publishState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
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
                                val curIdx = _state.value.currentIndex
                                val nextIdx = if (curIdx < 0) 0 else (curIdx + 1) % queue.size
                                playQueueIndex(nextIdx, true)
                            }
                        }
                        RepeatMode.OFF -> {
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
        }

    init {
        connect()
    }

    private fun connect() {
        val token =
            SessionToken(
                appContext,
                ComponentName(
                    appContext,
                    XvoxPlaybackService::class.java
                )
            )

        val future =
            MediaController.Builder(
                appContext,
                token
            ).buildAsync()

        future.addListener(
            {
                runCatching {
                    future.get()
                }.onSuccess {
                    mediaController ->

                    controller =
                        mediaController

                    mediaController.addListener(
                        listener
                    )

                    publishState()

                    progressJob =
                        scope.launch {
                            while (isActive) {
                                publishState()
                                delay(500L)
                            }
                        }
                }
            },
            ContextCompat.getMainExecutor(
                appContext
            )
        )
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        controller?.repeatMode = Player.REPEAT_MODE_OFF
        publishState()
    }

    fun setQueue(
        songs: List<Song>
    ) {
        queue = songs
        publishState()
    }

    fun currentQueue():
        List<Song> =
        queue

    fun playNext(
        song: Song
    ): List<Song> {
        val currentId =
            _state.value.currentSongId

        val currentPosition =
            queue.indexOfFirst {
                it.id == currentId
            }

        val without =
            queue.filterNot {
                it.id == song.id
            }
                .toMutableList()

        val updatedCurrent =
            without.indexOfFirst {
                it.id == currentId
            }

        val insert =
            if (updatedCurrent >= 0) {
                updatedCurrent + 1
            } else if (
                currentPosition >= 0
            ) {
                currentPosition
                    .coerceAtMost(
                        without.size
                    )
            } else {
                0
            }

        without.add(
            insert.coerceIn(
                0,
                without.size
            ),
            song
        )

        queue = without
        publishState()
        return queue
    }

    fun addToQueue(
        song: Song
    ): List<Song> {
        if (
            queue.any {
                it.id == song.id
            }
        ) {
            return queue
        }

        queue = queue + song
        publishState()
        return queue
    }

    fun removeFromQueue(
        songId: Long
    ): List<Song> {
        queue =
            queue.filterNot {
                it.id == songId
            }

        publishState()
        return queue
    }

    fun restoreSong(
        songId: Long
    ) {
        val mediaController =
            controller

        val liveId =
            mediaController
                ?.currentMediaItem
                ?.mediaId
                ?.toLongOrNull()

        if (liveId != null) return

        val index =
            queue.indexOfFirst {
                it.id == songId
            }

        val song =
            queue.getOrNull(index)
                ?: return

        restoredSongId = song.id

        _state.value =
            PlaybackState(
                connected =
                    mediaController != null,
                currentSongId = song.id,
                currentIndex = index,
                isPlaying = false,
                position = 0L,
                duration = song.duration
            )

        XvoxAppWidgetProvider.updateAllWidgets(
            appContext,
            song,
            false,
            0L,
            song.duration
        )
    }

    fun play(
        song: Song
    ) {
        val mediaController =
            controller ?: return

        val liveId =
            mediaController
                .currentMediaItem
                ?.mediaId

        if (
            liveId ==
            song.id.toString() &&
            restoredSongId == null
        ) {
            togglePlay()
            return
        }

        restoredSongId = null

        var index =
            queue.indexOfFirst {
                it.id == song.id
            }

        if (index < 0) {
            queue = queue + song
            index = queue.lastIndex
        }

        mediaController.setMediaItem(
            song.toMediaItem()
        )
        mediaController.prepare()
        mediaController.play()
        expectedPlaying = true
        expectedPlayingUntil = System.currentTimeMillis() + 1800

        _state.value =
            _state.value.copy(
                connected = true,
                currentSongId = song.id,
                currentIndex = index,
                isPlaying = true,
                position = 0L,
                duration = song.duration
            )

        XvoxAppWidgetProvider.updateAllWidgets(
            appContext,
            song,
            true,
            0L,
            song.duration
        )
    }

    fun playQueueIndex(
        index: Int,
        keepPlayingState: Boolean = true
    ) {
        val song =
            queue.getOrNull(index)
                ?: return

        val mediaController =
            controller ?: return

        val shouldPlay =
            if (restoredSongId != null) {
                true
            } else if (keepPlayingState) {
                _state.value.isPlaying || mediaController.isPlaying || mediaController.playWhenReady
            } else {
                true
            }

        restoredSongId = null
        expectedPlaying = shouldPlay
        expectedPlayingUntil = System.currentTimeMillis() + 1800

        mediaController.setMediaItem(
            song.toMediaItem()
        )
        mediaController.prepare()

        if (shouldPlay) {
            mediaController.play()
        } else {
            mediaController.pause()
        }

        _state.value =
            _state.value.copy(
                connected = true,
                currentSongId = song.id,
                currentIndex = index,
                isPlaying = shouldPlay,
                position = 0L,
                duration = song.duration
            )

        XvoxAppWidgetProvider.updateAllWidgets(
            appContext,
            song,
            shouldPlay,
            0L,
            song.duration
        )
    }

    fun playPrevious() {
        val index =
            _state.value.currentIndex

        if (queue.isEmpty() || index < 0) return

        val atFirst = index <= 0
        if (atFirst && repeatMode == RepeatMode.OFF) return
        val target = if (atFirst && repeatMode == RepeatMode.ALL) queue.lastIndex else index - 1

        playQueueIndex(
            target,
            true
        )
    }

    fun playNext() {
        val index =
            _state.value.currentIndex

        if (queue.isEmpty() || index < 0) return

        val atLast = index >= queue.lastIndex
        if (atLast && repeatMode == RepeatMode.OFF) return
        val target = if (atLast && repeatMode == RepeatMode.ALL) 0 else index + 1

        playQueueIndex(
            target,
            true
        )
    }

    fun seekTo(
        positionMs: Long
    ) {
        val mediaController =
            controller ?: return

        if (
            mediaController.currentMediaItem ==
            null
        ) return

        val duration =
            mediaController.duration
                .takeIf {
                    it > 0L
                }

        mediaController.seekTo(
            if (duration != null) {
                positionMs.coerceIn(
                    0L,
                    duration
                )
            } else {
                positionMs.coerceAtLeast(
                    0L
                )
            }
        )

        publishState()
    }

    fun togglePlay() {
        val restoredId =
            restoredSongId

        if (restoredId != null) {
            val index =
                queue.indexOfFirst {
                    it.id == restoredId
                }

            if (index >= 0) {
                playQueueIndex(
                    index,
                    false
                )
            }

            return
        }

        val mediaController =
            controller ?: return

        if (
            mediaController.currentMediaItem ==
            null
        ) return

        if (mediaController.isPlaying) {
            mediaController.pause()
            expectedPlaying = false
            expectedPlayingUntil = System.currentTimeMillis() + 400
        } else {
            mediaController.play()
            expectedPlaying = true
            expectedPlayingUntil = System.currentTimeMillis() + 800
        }
        publishState()
    }

    fun stop() {
        restoredSongId = null

        controller?.let {
            it.stop()
            it.clearMediaItems()
        }

        _state.value =
            PlaybackState(
                connected =
                    controller != null
            )

        XvoxAppWidgetProvider.updateAllWidgets(
            appContext,
            null,
            false,
            0L,
            0L
        )
    }

    private fun publishState() {
        val mediaController =
            controller

        if (mediaController == null) {
            if (restoredSongId == null) {
                _state.value =
                    PlaybackState()
            }
            return
        }

        val id =
            mediaController.currentMediaItem
                ?.mediaId
                ?.toLongOrNull()

        if (id == null) {
            val restored =
                restoredSongId

            if (restored != null) {
                val index =
                    queue.indexOfFirst {
                        it.id == restored
                    }

                val song =
                    queue.getOrNull(index)

                if (song != null) {
                    _state.value =
                        PlaybackState(
                            connected = true,
                            currentSongId =
                                song.id,
                            currentIndex =
                                index,
                            duration =
                                song.duration
                        )
                    XvoxAppWidgetProvider.updateAllWidgets(
                        appContext,
                        song,
                        false,
                        0L,
                        song.duration
                    )
                    return
                }
            }

            _state.value =
                PlaybackState(
                    connected = true
                )
            return
        }

        restoredSongId = null

        val index =
            queue.indexOfFirst {
                it.id == id
            }

        val currentSong = queue.getOrNull(index)

        val fallbackDuration =
            currentSong
                ?.duration
                ?: 0L

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

        _state.value =
            PlaybackState(
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
        controller?.removeListener(
            listener
        )
        controller?.release()
        controller = null
        scope.cancel()
    }

    private fun Song.toMediaItem():
        MediaItem =
        MediaItem.Builder()
            .setMediaId(
                id.toString()
            )
            .setUri(contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(
                        artworkUri
                    )
                    .build()
            )
            .build()
}
