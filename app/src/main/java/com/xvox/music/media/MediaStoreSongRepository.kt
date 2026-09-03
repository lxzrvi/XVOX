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
            val songs = ArrayList<Song>()

            val projection =
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION
                )

            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            val sortOrder =
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
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
                val durationColumn =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.DURATION
                    )

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)

                    val title =
                        cursor.getString(titleColumn)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: "Unknown song"

                    val rawArtist =
                        cursor.getString(artistColumn)
                            ?.trim()

                    val artist =
                        rawArtist
                            ?.takeIf {
                                it.isNotEmpty() &&
                                    !it.equals(
                                        "<unknown>",
                                        ignoreCase = true
                                    )
                            }
                            ?: "Unknown artist"

                    val albumId =
                        cursor.getLong(albumColumn)

                    val duration =
                        cursor.getLong(durationColumn)
                            .coerceAtLeast(0L)

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

                    songs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            contentUri = contentUri,
                            artworkUri = artworkUri,
                            duration = duration
                        )
                    )
                }
            }

            songs
        }
}
