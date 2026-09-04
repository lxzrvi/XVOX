package com.xvox.music.features.home

import android.content.Context
import android.provider.MediaStore
import com.xvox.music.core.model.Song
import com.xvox.music.metadata.XvoxMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongInfoReader(
    context: Context
) {
    private val appContext =
        context.applicationContext

    private val resolver =
        appContext.contentResolver

    private val metadataReader =
        XvoxMetadataReader(
            appContext
        )

    suspend fun read(
        song: Song
    ): SongInfo =
        withContext(Dispatchers.IO) {
            val metadata =
                runCatching {
                    metadataReader.read(
                        song.contentUri
                    )
                }.getOrNull()

            var displayName = ""
            var relativePath = ""
            var mime = ""

            val projection =
                arrayOf(
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    MediaStore.Audio.Media.MIME_TYPE
                )

            runCatching {
                resolver.query(
                    song.contentUri,
                    projection,
                    null,
                    null,
                    null
                )?.use {
                    cursor ->

                    if (cursor.moveToFirst()) {
                        displayName =
                            cursor.string(
                                MediaStore.Audio.Media.DISPLAY_NAME
                            )
                        relativePath =
                            cursor.string(
                                MediaStore.Audio.Media.RELATIVE_PATH
                            )
                        mime =
                            cursor.string(
                                MediaStore.Audio.Media.MIME_TYPE
                            )
                    }
                }
            }

            val format =
                displayName
                    .substringAfterLast(
                        '.',
                        ""
                    )
                    .uppercase()
                    .ifBlank {
                        mime.substringAfter(
                            "/",
                            "Unknown"
                        ).uppercase()
                    }

            SongInfo(
                title =
                    metadata?.title
                        ?: song.title,
                artist =
                    metadata?.artist
                        ?: song.artist,
                album =
                    metadata?.album
                        ?: "Unknown",
                albumArtist =
                    metadata?.albumArtist
                        ?: "Unknown",
                genre =
                    metadata?.genre
                        ?: "Unknown",
                year =
                    metadata?.year
                        ?.toString()
                        ?: "Unknown",
                duration =
                    formatDuration(
                        metadata?.duration
                            ?: song.duration
                    ),
                format =
                    format.ifBlank {
                        "Unknown"
                    },
                bitrate =
                    metadata?.bitrate
                        ?.takeIf {
                            it > 0
                        }
                        ?.let {
                            "${it / 1000} kbps"
                        }
                        ?: "Unknown",
                sampleRate =
                    metadata?.sampleRate
                        ?.takeIf {
                            it > 0
                        }
                        ?.let {
                            "${it} Hz"
                        }
                        ?: "Unknown",
                location =
                    when {
                        relativePath.isNotBlank() &&
                            displayName.isNotBlank() ->
                            "$relativePath$displayName"

                        displayName.isNotBlank() ->
                            displayName

                        else ->
                            song.contentUri
                                .toString()
                    },
                trackNumber =
                    metadata?.trackNumber
                        ?.toString()
                        ?: "Unknown"
            )
        }

    private fun android.database.Cursor.string(
        column: String
    ): String {
        val index =
            getColumnIndex(column)

        if (
            index < 0 ||
            isNull(index)
        ) {
            return ""
        }

        return getString(index)
            .orEmpty()
            .trim()
    }

    private fun formatDuration(
        millis: Long
    ): String {
        val total =
            millis
                .coerceAtLeast(0L) /
                1000L

        return buildString {
            append(total / 60L)
            append(':')
            append(
                (total % 60L)
                    .toString()
                    .padStart(
                        2,
                        '0'
                    )
            )
        }
    }
}
