package com.xvox.music.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.xvox.music.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreSongRepository(
    private val context: Context
) {

    suspend fun loadSongs(): List<Song> =
        withContext(Dispatchers.IO) {
            val songs = mutableListOf<Song>()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            val sortOrder =
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->

                val idIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media._ID
                    )

                val titleIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.TITLE
                    )

                val artistIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ARTIST
                    )

                val albumIdIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ALBUM_ID
                    )

                while (cursor.moveToNext()) {
                    val id =
                        cursor.getLong(idIndex)

                    val title =
                        cursor.getString(titleIndex)
                            ?.takeIf { it.isNotBlank() }
                            ?: "Unknown song"

                    val artist =
                        cursor.getString(artistIndex)
                            ?.takeIf {
                                it.isNotBlank() &&
                                    it != "<unknown>"
                            }
                            ?: "Unknown artist"

                    val albumId =
                        cursor.getLong(albumIdIndex)

                    val contentUri =
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media
                                .EXTERNAL_CONTENT_URI,
                            id
                        )

                    val artworkUri =
                        if (albumId > 0L) {
                            ContentUris.withAppendedId(
                                android.net.Uri.parse(
                                    "content://media/external/audio/albumart"
                                ),
                                albumId
                            )
                        } else {
                            null
                        }

                    songs += Song(
                        id = id,
                        title = title,
                        artist = artist,
                        contentUri = contentUri,
                        artworkUri = artworkUri
                    )
                }
            }

            songs
        }
}
