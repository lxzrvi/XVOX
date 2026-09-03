package com.xvox.music.core.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val contentUri: Uri,
    val artworkUri: Uri?,
    val duration: Long = 0L
)
