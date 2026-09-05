package com.xvox.music.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.xvox.music.core.model.Song

fun Song.toMediaItem(): MediaItem =
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
