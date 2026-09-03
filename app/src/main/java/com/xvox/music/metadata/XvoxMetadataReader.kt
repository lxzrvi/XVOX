package com.xvox.music.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
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
        withContext(
            Dispatchers.IO
        ) {
            val androidMetadata =
                readAndroidMetadata(
                    uri
                )

            /*
             * Deep tags are attempted separately.
             *
             * Failure never invalidates the useful Android
             * metadata result.
             */
            val deepMetadata =
                runCatching {
                    readDeepMetadata(
                        uri
                    )
                }
                    .getOrNull()

            merge(
                androidMetadata,
                deepMetadata
            )
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
                    )
                        ?.toIntOrNull(),

                duration =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_DURATION
                    )
                        ?.toLongOrNull(),

                bitrate =
                    retriever.value(
                        MediaMetadataRetriever
                            .METADATA_KEY_BITRATE
                    )
                        ?.toIntOrNull()
            )
        } catch (_: Exception) {
            SongMetadata.empty(
                uri
            )
        } finally {
            runCatching {
                retriever.release()
            }
        }
    }

    private fun MediaMetadataRetriever.value(
        key: Int
    ): String? {
        return extractMetadata(key)
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
    }

    private fun readDeepMetadata(
        uri: Uri
    ): SongMetadata? {
        val temp =
            createTemporaryAudioFile(
                uri
            ) ?: return null

        return try {
            val audioFile =
                AudioFileIO.read(
                    temp
                )

            val tag =
                audioFile.tag

            val header =
                audioFile.audioHeader

            SongMetadata(
                uri = uri,

                title =
                    tag
                        ?.field(
                            FieldKey.TITLE
                        ),

                artist =
                    tag
                        ?.field(
                            FieldKey.ARTIST
                        ),

                album =
                    tag
                        ?.field(
                            FieldKey.ALBUM
                        ),

                albumArtist =
                    tag
                        ?.field(
                            FieldKey.ALBUM_ARTIST
                        ),

                composer =
                    tag
                        ?.field(
                            FieldKey.COMPOSER
                        ),

                genre =
                    tag
                        ?.field(
                            FieldKey.GENRE
                        ),

                year =
                    tag
                        ?.field(
                            FieldKey.YEAR
                        )
                        ?.take(4)
                        ?.toIntOrNull(),

                trackNumber =
                    tag
                        ?.field(
                            FieldKey.TRACK
                        )
                        ?.substringBefore(
                            "/"
                        )
                        ?.toIntOrNull(),

                discNumber =
                    tag
                        ?.field(
                            FieldKey.DISC_NO
                        )
                        ?.substringBefore(
                            "/"
                        )
                        ?.toIntOrNull(),

                duration =
                    header
                        ?.trackLength
                        ?.toLong()
                        ?.times(
                            1000L
                        ),

                bitrate =
                    header
                        ?.bitRateAsNumber
                        ?.toInt(),

                sampleRate =
                    header
                        ?.sampleRateAsNumber
                        ?.toInt(),

                lyrics =
                    tag
                        ?.field(
                            FieldKey.LYRICS
                        ),

                comment =
                    tag
                        ?.field(
                            FieldKey.COMMENT
                        )
            )
        } finally {
            temp.delete()
        }
    }

    private fun createTemporaryAudioFile(
        uri: Uri
    ): File? {
        /*
         * Preserve a usable extension where ContentResolver
         * can expose MIME type.
         *
         * Some tag parsers use extension as a format hint.
         */
        val extension =
            when (
                resolver.getType(
                    uri
                )
            ) {
                "audio/mpeg" ->
                    ".mp3"

                "audio/flac" ->
                    ".flac"

                "audio/mp4",
                "audio/m4a",
                "audio/x-m4a" ->
                    ".m4a"

                "audio/ogg",
                "application/ogg" ->
                    ".ogg"

                "audio/opus" ->
                    ".opus"

                "audio/wav",
                "audio/x-wav" ->
                    ".wav"

                else ->
                    ".audio"
            }

        val temp =
            File.createTempFile(
                "xvox_metadata_",
                extension,
                appContext.cacheDir
            )

        return try {
            resolver
                .openInputStream(
                    uri
                )
                ?.use {
                    input ->

                    temp
                        .outputStream()
                        .buffered()
                        .use {
                            output ->

                            input.copyTo(
                                output,
                                bufferSize =
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

    private fun org.jaudiotagger.tag.Tag.field(
        key: FieldKey
    ): String? {
        return runCatching {
            getFirst(key)
        }
            .getOrNull()
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
    }

    private fun merge(
        primary: SongMetadata,
        deep: SongMetadata?
    ): SongMetadata {
        if (deep == null) {
            return primary
        }

        return SongMetadata(
            uri =
                primary.uri,

            title =
                deep.title
                    ?: primary.title,

            artist =
                deep.artist
                    ?: primary.artist,

            album =
                deep.album
                    ?: primary.album,

            albumArtist =
                deep.albumArtist
                    ?: primary.albumArtist,

            composer =
                deep.composer
                    ?: primary.composer,

            genre =
                deep.genre
                    ?: primary.genre,

            year =
                deep.year
                    ?: primary.year,

            trackNumber =
                deep.trackNumber,

            discNumber =
                deep.discNumber,

            duration =
                primary.duration
                    ?: deep.duration,

            bitrate =
                primary.bitrate
                    ?: deep.bitrate,

            sampleRate =
                deep.sampleRate,

            lyrics =
                deep.lyrics,

            comment =
                deep.comment,

            artworkCacheKey =
                null
        )
    }
}
