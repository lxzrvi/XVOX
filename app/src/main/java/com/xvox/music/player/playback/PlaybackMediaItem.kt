package com.xvox.music.player.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.xvox.music.core.model.Song

fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder().setMediaId(id.toString()).setUri(contentUri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setArtworkUri(artworkUri)
            .setExtras(Bundle().apply {
                putString("xvox_original_uri", contentUri.toString())
                putLong("xvox_duration", duration)
                putLong("xvox_file_size", sizeBytes)
            }).build()).build()
}
