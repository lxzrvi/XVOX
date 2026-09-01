package com.xvox.music.player.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.xvox.music.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val currentSongId: Long? = null,
    val isPlaying: Boolean = false
)

class PlaybackController(
    context: Context
) {

    private val player =
        ExoPlayer.Builder(
            context.applicationContext
        ).build()

    private val _state =
        MutableStateFlow(
            PlaybackState()
        )

    val state:
        StateFlow<PlaybackState> =
        _state.asStateFlow()

    private val listener =
        object : Player.Listener {

            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {
                _state.value =
                    _state.value.copy(
                        isPlaying =
                            isPlaying
                    )
            }
        }

    init {
        player.addListener(
            listener
        )
    }

    fun play(song: Song) {
        if (
            _state.value.currentSongId ==
            song.id
        ) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }

            return
        }

        _state.value =
            PlaybackState(
                currentSongId =
                    song.id,
                isPlaying = false
            )

        player.setMediaItem(
            MediaItem.Builder()
                .setMediaId(
                    song.id.toString()
                )
                .setUri(
                    song.contentUri
                )
                .build()
        )

        player.prepare()
        player.play()
    }

    fun release() {
        player.removeListener(
            listener
        )
        player.release()
    }
}
