package com.xvox.music.metadata

import android.net.Uri

interface SongMetadataReader {

    suspend fun read(
        uri: Uri
    ): SongMetadata
}
