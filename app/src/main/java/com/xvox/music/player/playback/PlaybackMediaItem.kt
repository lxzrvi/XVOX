package com.xvox.music.player.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.xvox.music.artwork.XvoxFallbackArtworkProvider
import com.xvox.music.artwork.XvoxArtworkCache
import com.xvox.music.core.model.Song

/** A queue occurrence may intentionally repeat one library Song, so callers supply a unique media ID. */
fun Song.toMediaItem(mediaId: String = id.toString()): MediaItem {
    // A stale queue can outlive a library scan. If its former album-art URI has already proved
    // unreadable, hand Media3 the same local fallback URI immediately instead of the system UI
    // falling back to the app's generic XVOX notification icon.
    val notificationArtwork = artworkUri?.takeUnless { XvoxArtworkCache.isKnownFailed(it) }
        ?: XvoxFallbackArtworkProvider.uriForSong(id)
    return MediaItem.Builder().setMediaId(mediaId).setUri(contentUri)
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            // Android's media notification loads this local URI lazily for a coverless item,
            // yielding the same supplied XVOX default cover instead of a generic placeholder.
            .setArtworkUri(notificationArtwork)
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
