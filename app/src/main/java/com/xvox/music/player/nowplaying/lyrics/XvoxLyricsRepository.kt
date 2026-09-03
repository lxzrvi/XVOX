package com.xvox.music.player.nowplaying.lyrics

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.metadata.XvoxMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class XvoxLyricsRepository(
    context: Context
) {
    private val appContext =
        context.applicationContext

    private val preferences =
        UserPreferencesRepository(
            appContext
        )

    private val metadataReader =
        XvoxMetadataReader(
            appContext
        )

    suspend fun load(
        song: Song
    ): XvoxLyrics? =
        withContext(
            Dispatchers.IO
        ) {
            val selectedUri =
                preferences
                    .lyricsUri(
                        song.id
                    )
                    .first()
                    ?.let(
                        Uri::parse
                    )

            if (
                selectedUri != null
            ) {
                readUserLyrics(
                    selectedUri
                )
                    ?.let {
                        return@withContext it
                    }
            }

            val metadata =
                metadataReader.read(
                    song.contentUri
                )

            metadata.lyrics
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    raw ->

                    return@withContext
                        XvoxLyricsParser.parse(
                            raw =
                                raw,
                            source =
                                XvoxLyricsSource
                                    .EMBEDDED
                        )
                }

            null
        }

    suspend fun attach(
        songId: Long,
        uri: Uri
    ): XvoxLyrics? =
        withContext(
            Dispatchers.IO
        ) {
            runCatching {
                appContext
                    .contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
            }

            val lyrics =
                readUserLyrics(
                    uri
                )
                    ?: return@withContext null

            preferences.setLyricsUri(
                songId =
                    songId,
                uri =
                    uri.toString()
            )

            lyrics
        }

    private fun readUserLyrics(
        uri: Uri
    ): XvoxLyrics? {
        val raw =
            runCatching {
                appContext
                    .contentResolver
                    .openInputStream(
                        uri
                    )
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
            }
                .getOrNull()
                ?: return null

        if (
            raw.isBlank()
        ) {
            return null
        }

        val source =
            if (
                raw.contains(
                    Regex(
                        """\[\d{1,3}:\d{2}"""
                    )
                )
            ) {
                XvoxLyricsSource
                    .USER_LRC
            } else {
                XvoxLyricsSource
                    .USER_TEXT
            }

        return XvoxLyricsParser.parse(
            raw = raw,
            source = source
        )
    }
}
