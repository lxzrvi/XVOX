package com.xvox.music.player.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.xvox.music.core.model.Song
import com.xvox.music.split.SplitModel
import com.xvox.music.split.XvoxSplitRepository

fun Song.toMediaItem(): MediaItem {
    val prepared = XvoxSplitRepository.resolve(this)
    return MediaItem.Builder().setMediaId(id.toString()).setUri(prepared ?: contentUri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setArtworkUri(artworkUri)
            .setExtras(Bundle().apply {
                putBoolean(SplitModel.STEM_FLAG, prepared != null)
                putString("xvox_original_uri", contentUri.toString())
                putLong("xvox_duration", duration)
                putLong("xvox_file_size", sizeBytes)
            }).build()).build()
}
