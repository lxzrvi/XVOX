package com.xvox.music.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File

class XvoxMetadataReader(
    context: Context
) : SongMetadataReader {

    private val appContext =
        context.applicationContext

    private val resolver =
        appContext.contentResolver

    override suspend fun read(
        uri: Uri
    ): SongMetadata =
        withContext(Dispatchers.IO) {
            val android =
                readAndroidMetadata(uri)

            val deep =
                runCatching {
                    readDeepMetadata(uri)
                }.getOrNull()

            merge(android, deep)
        }

    private fun readAndroidMetadata(
        uri: Uri
    ): SongMetadata {
        val retriever =
            MediaMetadataRetriever()

        return try {
            retriever.setDataSource(
                appContext,
                uri
            )

            SongMetadata(
                uri = uri,
                title =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_TITLE
                    ),
                artist =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_ARTIST
                    ),
                album =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_ALBUM
                    ),
                albumArtist =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_ALBUMARTIST
                    ),
                composer =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_COMPOSER
                    ),
                genre =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_GENRE
                    ),
                year =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_YEAR
                    )?.toIntOrNull(),
                duration =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_DURATION
                    )?.toLongOrNull(),
                bitrate =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_BITRATE
                    )?.toIntOrNull()
            )
        } catch (_: Exception) {
            SongMetadata.empty(uri)
        } finally {
            runCatching {
                retriever.release()
            }
        }
    }

    private fun MediaMetadataRetriever.value(
        key: Int
    ): String? =
        extractMetadata(key)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun readDeepMetadata(
        uri: Uri
    ): SongMetadata? {
        val temp =
            createTemporaryAudioFile(uri)
                ?: return null

        return try {
            val audioFile =
                AudioFileIO.read(temp)

            val tag = audioFile.tag
            val header = audioFile.audioHeader

            SongMetadata(
                uri = uri,
                title = tag?.field(FieldKey.TITLE),
                artist = tag?.field(FieldKey.ARTIST),
                album = tag?.field(FieldKey.ALBUM),
                albumArtist =
                    tag?.field(FieldKey.ALBUM_ARTIST),
                composer =
                    tag?.field(FieldKey.COMPOSER),
                genre = tag?.field(FieldKey.GENRE),
                year =
                    tag?.field(FieldKey.YEAR)
                        ?.take(4)
                        ?.toIntOrNull(),
                trackNumber =
                    tag?.field(FieldKey.TRACK)
                        ?.substringBefore("/")
                        ?.toIntOrNull(),
                discNumber =
                    tag?.field(FieldKey.DISC_NO)
                        ?.substringBefore("/")
                        ?.toIntOrNull(),
                duration =
                    header?.trackLength
                        ?.toLong()
                        ?.times(1000L),
                bitrate =
                    header?.bitRateAsNumber
                        ?.toInt(),
                sampleRate =
                    header?.sampleRateAsNumber
                        ?.toInt(),
                lyrics = tag?.readLyrics(),
                comment =
                    tag?.field(FieldKey.COMMENT)
            )
        } finally {
            temp.delete()
        }
    }

    private fun Tag.readLyrics(): String? {
        field(FieldKey.LYRICS)?.let {
            return it
        }

        val matches =
            listOf(
                "USLT",
                "SYLT",
                "LYRICS",
                "UNSYNCEDLYRICS",
                "UNSYNCED LYRICS",
                "©LYR",
                "\u00A9LYR",
                "LYRIC",
                "UNSYNCED_LYRICS"
            )

        val fields =
            runCatching {
                fields.asSequence().toList()
            }.getOrDefault(emptyList())

        for (field in fields) {
            val id =
                runCatching {
                    field.id
                }
                    .getOrNull()
                    ?.uppercase()
                    .orEmpty()

            val relevant =
                matches.any {
                    candidate ->
                    id == candidate ||
                        id.contains(candidate)
                } ||
                    id.contains("LYRIC") ||
                    id.contains("USLT") ||
                    id.contains("SYLT")

            if (!relevant) continue

            val text =
                extractNativeLyrics(
                    field.toString()
                )

            if (!text.isNullOrBlank()) {
                return text
            }
        }

        return null
    }

    private fun extractNativeLyrics(
        raw: String
    ): String? {
        var text = raw.trim()

        if (text.isBlank()) return null

        val markers =
            listOf(
                "Text=",
                "Text:",
                "Lyrics=",
                "Lyrics:",
                "Content=",
                "Content:"
            )

        for (marker in markers) {
            val index =
                text.indexOf(
                    marker,
                    ignoreCase = true
                )

            if (index >= 0) {
                val candidate =
                    text.substring(
                        index + marker.length
                    ).trim()

                if (candidate.isNotBlank()) {
                    text = candidate
                    break
                }
            }
        }

        text =
            text.replace(
                "\\n",
                "\n"
            ).replace(
                "\\r",
                "\n"
            ).trim()

        return text.takeIf {
            it.isNotBlank()
        }
    }

    private fun createTemporaryAudioFile(
        uri: Uri
    ): File? {
        val extension =
            when (resolver.getType(uri)) {
                "audio/mpeg" -> ".mp3"
                "audio/flac",
                "audio/x-flac" -> ".flac"

                "audio/mp4",
                "audio/m4a",
                "audio/x-m4a" -> ".m4a"

                "audio/ogg",
                "application/ogg" -> ".ogg"

                "audio/opus",
                "audio/ogg; codecs=opus" -> ".opus"

                "audio/wav",
                "audio/x-wav" -> ".wav"

                else -> extensionFromUri(uri)
            }

        val temp =
            File.createTempFile(
                "xvox_metadata_",
                extension,
                appContext.cacheDir
            )

        return try {
            resolver.openInputStream(uri)
                ?.use { input ->
                    temp.outputStream()
                        .buffered()
                        .use { output ->
                            input.copyTo(
                                output,
                                64 * 1024
                            )
                        }
                }
                ?: run {
                    temp.delete()
                    return null
                }

            temp
        } catch (_: Exception) {
            temp.delete()
            null
        }
    }

    private fun extensionFromUri(
        uri: Uri
    ): String {
        val path =
            uri.lastPathSegment
                .orEmpty()

        val extension =
            path.substringAfterLast(
                '.',
                ""
            )
                .lowercase()

        return when (extension) {
            "mp3",
            "flac",
            "m4a",
            "mp4",
            "ogg",
            "opus",
            "wav" -> ".$extension"

            else -> ".mp3"
        }
    }

    private fun Tag.field(
        key: FieldKey
    ): String? =
        runCatching {
            getFirst(key)
        }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun merge(
        primary: SongMetadata,
        deep: SongMetadata?
    ): SongMetadata {
        if (deep == null) return primary

        return SongMetadata(
            uri = primary.uri,
            title = deep.title ?: primary.title,
            artist =
                deep.artist ?: primary.artist,
            album = deep.album ?: primary.album,
            albumArtist =
                deep.albumArtist
                    ?: primary.albumArtist,
            composer =
                deep.composer ?: primary.composer,
            genre = deep.genre ?: primary.genre,
            year = deep.year ?: primary.year,
            trackNumber = deep.trackNumber,
            discNumber = deep.discNumber,
            duration =
                primary.duration ?: deep.duration,
            bitrate =
                primary.bitrate ?: deep.bitrate,
            sampleRate = deep.sampleRate,
            lyrics = deep.lyrics,
            comment = deep.comment,
            artworkCacheKey = null
        )
    }
}
