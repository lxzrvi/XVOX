package com.xvox.music.player.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.xvox.music.core.model.Song

class PlaybackController(
    context: Context
) {

    private val player =
        ExoPlayer.Builder(
            context.applicationContext
        ).build()

    fun play(song: Song) {
        val mediaItem =
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.contentUri)
                .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun release() {
        player.release()
    }
}
