package com.xvox.music.features.home

data class SongInfo(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: String,
    val duration: String,
    val format: String,
    val bitrate: String,
    val sampleRate: String,
    val location: String,
    val trackNumber: String
)
