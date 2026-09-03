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
    ): XvoxLyrics? {
        return withContext(
            Dispatchers.IO
        ) {
            loadInternal(
                song
            )
        }
    }

    private suspend fun loadInternal(
        song: Song
    ): XvoxLyrics? {
        /*
         * ====================================================
         * 1. USER-SELECTED LRC / TXT
         * ====================================================
         *
         * User-selected lyrics always have priority over
         * embedded lyrics.
         */
        val selectedUriString =
            preferences
                .lyricsUri(
                    song.id
                )
                .first()

        if (
            !selectedUriString
                .isNullOrBlank()
        ) {
            val selectedUri =
                runCatching {
                    Uri.parse(
                        selectedUriString
                    )
                }
                    .getOrNull()

            if (
                selectedUri != null
            ) {
                val userLyrics =
                    readUserLyrics(
                        selectedUri
                    )

                if (
                    userLyrics != null
                ) {
                    return userLyrics
                }
            }
        }

        /*
         * ====================================================
         * 2. EMBEDDED METADATA
         * ====================================================
         *
         * XvoxMetadataReader:
         *
         * content://
         *     ↓
         * Android metadata
         *     ↓
         * private temporary copy when deep parsing is needed
         *     ↓
         * Jaudiotagger
         *
         * Never File(uri.path!!).
         */
        val metadata =
            runCatching {
                metadataReader.read(
                    song.contentUri
                )
            }
                .getOrNull()

        val embeddedLyrics =
            metadata
                ?.lyrics
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }

        if (
            embeddedLyrics != null
        ) {
            return XvoxLyricsParser.parse(
                raw =
                    embeddedLyrics,
                source =
                    XvoxLyricsSource
                        .EMBEDDED
            )
        }

        return null
    }

    suspend fun attach(
        songId: Long,
        uri: Uri
    ): XvoxLyrics? {
        return withContext(
            Dispatchers.IO
        ) {
            /*
             * OpenDocument normally allows a persistable grant.
             *
             * Some document providers don't support it, so failure
             * here must not prevent immediate lyrics loading.
             */
            runCatching {
                appContext
                    .contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent
                            .FLAG_GRANT_READ_URI_PERMISSION
                    )
            }

            val lyrics =
                readUserLyrics(
                    uri
                )

            if (
                lyrics == null
            ) {
                null
            } else {
                preferences
                    .setLyricsUri(
                        songId =
                            songId,
                        uri =
                            uri.toString()
                    )

                lyrics
            }
        }
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
                        reader ->

                        reader.readText()
                    }
            }
                .getOrNull()
                ?: return null

        if (
            raw.isBlank()
        ) {
            return null
        }

        val hasTimestamp =
            Regex(
                """\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]"""
            )
                .containsMatchIn(
                    raw
                )

        val source =
            if (hasTimestamp) {
                XvoxLyricsSource
                    .USER_LRC
            } else {
                XvoxLyricsSource
                    .USER_TEXT
            }

        return XvoxLyricsParser.parse(
            raw =
                raw,
            source =
                source
        )
    }
}
