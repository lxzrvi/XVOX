package com.xvox.music.features.artist

import com.xvox.music.core.model.Song

data class XvoxArtist(
    val name: String,
    val songs: List<Song>,
    val coverSong: Song? = null,
    val customImageUri: String? = null
)
