package com.xvox.music.metadata

import android.net.Uri

data class SongMetadata(
    val uri: Uri,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val duration: Long? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val lyrics: String? = null,
    val comment: String? = null,
    val artworkCacheKey: String? = null
) {
    companion object {
        fun empty(
            uri: Uri
        ): SongMetadata {
            return SongMetadata(
                uri = uri
            )
        }
    }
}
