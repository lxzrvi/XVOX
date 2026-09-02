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
import com.xvox.music.player.session.XvoxPlaybackService

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

        val item =
            song.toMediaItem()

        mediaController.setMediaItem(
            item
        )

        mediaController.prepare()
        mediaController.play()

        _state.value =
            _state.value.copy(
                currentSongId =
                    song.id,
                currentIndex =
                    index
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
                mediaController
                    .isPlaying
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

        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            mediaController.play()
        }
    }

    private fun publishState() {
        val mediaController =
            controller

        if (mediaController == null) {
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

        controller?.removeListener(
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
