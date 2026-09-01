package com.xvox.music.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.xvox.music.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreSongRepository(
    context: Context
) {

    private val resolver =
        context.applicationContext.contentResolver

    suspend fun loadSongs(): List<Song> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<Song>()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID
            )

            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->

                val idColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media._ID
                    )

                val titleColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.TITLE
                    )

                val artistColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ARTIST
                    )

                val albumColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ALBUM_ID
                    )

                while (cursor.moveToNext()) {
                    val id =
                        cursor.getLong(idColumn)

                    val albumId =
                        cursor.getLong(albumColumn)

                    val title =
                        cursor.getString(titleColumn)
                            ?.takeIf { it.isNotBlank() }
                            ?: "Unknown song"

                    val rawArtist =
                        cursor.getString(artistColumn)

                    val artist =
                        rawArtist?.takeIf {
                            it.isNotBlank() &&
                                it != "<unknown>"
                        } ?: "Unknown artist"

                    val contentUri =
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media
                                .EXTERNAL_CONTENT_URI,
                            id
                        )

                    val artworkUri =
                        if (albumId > 0L) {
                            ContentUris.withAppendedId(
                                Uri.parse(
                                    "content://media/external/audio/albumart"
                                ),
                                albumId
                            )
                        } else {
                            null
                        }

                    result += Song(
                        id = id,
                        title = title,
                        artist = artist,
                        contentUri = contentUri,
                        artworkUri = artworkUri
                    )
                }
            }

            result
        }
}
