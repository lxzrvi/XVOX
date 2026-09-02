package com.xvox.music.player.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

data class PlaybackState(
    val currentSongId: Long? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L
)

class PlaybackController(
    context: Context
) {

    private val player =
        ExoPlayer.Builder(
            context.applicationContext
        ).build()

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate
        )

    private val _state =
        MutableStateFlow(
            PlaybackState()
        )

    val state:
        StateFlow<PlaybackState> =
        _state.asStateFlow()

    private var queue:
        List<Song> = emptyList()

    private var progressJob: Job? = null

    private val listener =
        object : Player.Listener {

            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {
                updateState()
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                updateState()
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {
                updateState()
            }
        }

    init {
        player.addListener(listener)

        progressJob =
            scope.launch {
                while (isActive) {
                    updateState()
                    delay(500L)
                }
            }
    }

    fun setQueue(
        songs: List<Song>
    ) {
        queue = songs

        updateState()
    }

    fun play(
        song: Song
    ) {
        if (
            _state.value.currentSongId ==
            song.id
        ) {
            togglePlay()
            return
        }

        val index =
            queue.indexOfFirst {
                it.id == song.id
            }

        playSong(
            song = song,
            index = index
        )
    }

    fun playQueueIndex(
        index: Int
    ) {
        val song =
            queue.getOrNull(index)
                ?: return

        playSong(
            song = song,
            index = index
        )
    }

    fun togglePlay() {
        if (
            player.currentMediaItem ==
            null
        ) {
            return
        }

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    private fun playSong(
        song: Song,
        index: Int
    ) {
        val item =
            MediaItem.Builder()
                .setMediaId(
                    song.id.toString()
                )
                .setUri(
                    song.contentUri
                )
                .build()

        player.setMediaItem(item)
        player.prepare()
        player.play()

        _state.value =
            _state.value.copy(
                currentSongId =
                    song.id,
                currentIndex =
                    index,
                position = 0L
            )
    }

    private fun updateState() {
        val id =
            player.currentMediaItem
                ?.mediaId
                ?.toLongOrNull()

        val index =
            queue.indexOfFirst {
                it.id == id
            }

        _state.value =
            PlaybackState(
                currentSongId = id,
                currentIndex = index,
                isPlaying =
                    player.isPlaying,
                position =
                    player.currentPosition
                        .coerceAtLeast(0L),
                duration =
                    player.duration
                        .takeIf {
                            it > 0L
                        }
                        ?: 0L
            )
    }

    fun release() {
        progressJob?.cancel()
        player.removeListener(listener)
        player.release()
        scope.cancel()
    }
}
