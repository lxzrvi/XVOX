package com.xvox.music.player.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.xvox.music.core.model.Song

/** A queue occurrence may intentionally repeat one library Song, so callers supply a unique media ID. */
fun Song.toMediaItem(mediaId: String = id.toString()): MediaItem {
    return MediaItem.Builder().setMediaId(mediaId).setUri(contentUri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setArtworkUri(artworkUri)
            .setExtras(Bundle().apply {
                putString("xvox_original_uri", contentUri.toString())
                putLong("xvox_song_id", id)
                putLong("xvox_duration", duration)
                putLong("xvox_file_size", sizeBytes)
            }).build()).build()
}

/** Original library ID carried by an occurrence-specific media item, if one is available. */
fun MediaItem.xvoxOriginalSongId(): Long? {
    val extras = mediaMetadata.extras
    return if (extras?.containsKey("xvox_song_id") == true) {
        extras.getLong("xvox_song_id")
    } else {
        mediaId.toLongOrNull()
    }
}
