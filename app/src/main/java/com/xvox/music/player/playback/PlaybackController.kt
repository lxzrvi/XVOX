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
import com.xvox.music.player.session.XvoxPlaybackService
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
    private val appContext =
        context.applicationContext

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

                    mediaController
                        .addListener(
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

    fun setQueue(
        songs: List<Song>
    ) {
        queue = songs
        publishState()
    }

    fun play(
        song: Song
    ) {
        val mediaController =
            controller ?: return

        if (
            mediaController
                .currentMediaItem
                ?.mediaId ==
            song.id.toString()
        ) {
            togglePlay()
            return
        }

        val index =
            queue.indexOfFirst {
                it.id == song.id
            }

        mediaController.setMediaItem(
            song.toMediaItem()
        )

        mediaController.prepare()
        mediaController.play()

        _state.value =
            _state.value.copy(
                connected = true,
                currentSongId =
                    song.id,
                currentIndex =
                    index,
                isPlaying = true,
                position = 0L,
                duration = 0L
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
            if (keepPlayingState) {
                mediaController.isPlaying
            } else {
                true
            }

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
                currentSongId =
                    song.id,
                currentIndex =
                    index,
                isPlaying =
                    shouldPlay,
                position = 0L,
                duration = 0L
            )
    }

    fun playPrevious() {
        val index =
            _state.value
                .currentIndex

        if (index <= 0) {
            return
        }

        playQueueIndex(
            index = index - 1,
            keepPlayingState = true
        )
    }

    fun playNext() {
        val index =
            _state.value
                .currentIndex

        if (
            index < 0 ||
            index >=
            queue.lastIndex
        ) {
            return
        }

        playQueueIndex(
            index = index + 1,
            keepPlayingState = true
        )
    }

    fun seekTo(
        positionMs: Long
    ) {
        val mediaController =
            controller ?: return

        if (
            mediaController
                .currentMediaItem ==
            null
        ) {
            return
        }

        val duration =
            mediaController
                .duration
                .takeIf {
                    it > 0L
                }

        val target =
            if (duration != null) {
                positionMs.coerceIn(
                    0L,
                    duration
                )
            } else {
                positionMs
                    .coerceAtLeast(0L)
            }

        mediaController.seekTo(
            target
        )

        publishState()
    }

    fun togglePlay() {
        val mediaController =
            controller ?: return

        if (
            mediaController
                .currentMediaItem ==
            null
        ) {
            return
        }

        if (
            mediaController.isPlaying
        ) {
            mediaController.pause()
        } else {
            mediaController.play()
        }
    }

    fun stop() {
        val mediaController =
            controller ?: return

        mediaController.stop()
        mediaController.clearMediaItems()

        _state.value =
            PlaybackState(
                connected = true
            )
    }

    private fun publishState() {
        val mediaController =
            controller

        if (
            mediaController == null
        ) {
            _state.value =
                PlaybackState()

            return
        }

        val id =
            mediaController
                .currentMediaItem
                ?.mediaId
                ?.toLongOrNull()

        _state.value =
            PlaybackState(
                connected = true,
                currentSongId = id,
                currentIndex =
                    queue.indexOfFirst {
                        it.id == id
                    },
                isPlaying =
                    mediaController
                        .isPlaying,
                position =
                    mediaController
                        .currentPosition
                        .coerceAtLeast(0L),
                duration =
                    mediaController
                        .duration
                        .takeIf {
                            it > 0L
                        }
                        ?: 0L
            )
    }

    fun release() {
        progressJob?.cancel()

        controller
            ?.removeListener(
                listener
            )

        controller?.release()
        controller = null

        scope.cancel()
    }

    private fun Song.toMediaItem():
        MediaItem {
        return MediaItem.Builder()
            .setMediaId(
                id.toString()
            )
            .setUri(
                contentUri
            )
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
}
