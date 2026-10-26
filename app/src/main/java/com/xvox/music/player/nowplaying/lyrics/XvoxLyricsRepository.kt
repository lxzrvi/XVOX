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
        UserPreferencesRepository(appContext)

    private val metadataReader =
        XvoxMetadataReader(appContext)

    suspend fun load(
        song: Song
    ): XvoxLyrics? =
        withContext(Dispatchers.IO) {
            loadInternal(song, true)
        }

    private suspend fun loadInternal(
        song: Song,
        includeCustom: Boolean
    ): XvoxLyrics? {
        if (includeCustom) {
            val selected =
                preferences.lyricsUri(song.id)
                    .first()

            if (!selected.isNullOrBlank()) {
                val uri =
                    runCatching {
                        Uri.parse(selected)
                    }.getOrNull()

                if (uri != null) {
                    readUserLyrics(uri)?.let {
                        return it
                    }
                }
            }
        }

        val metadata =
            runCatching {
                metadataReader.read(
                    song.contentUri
                )
            }.getOrNull()

        val raw =
            metadata?.lyrics
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: return null

        return XvoxLyricsParser.parse(
            raw = raw,
            source =
                XvoxLyricsSource.EMBEDDED
        )
    }

    suspend fun attach(
        songId: Long,
        uri: Uri
    ): XvoxLyrics? =
        withContext(Dispatchers.IO) {
            // This repository guard also protects callers that bypass the Compose picker. A ZIP,
            // image, or arbitrary binary file must never become the saved custom-lyrics URI.
            if (!XvoxLyricsFileSupport.isSupported(appContext, uri)) {
                return@withContext null
            }

            runCatching {
                appContext.contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
            }

            val lyrics =
                readUserLyrics(uri)
                    ?: return@withContext null

            preferences.setLyricsUri(
                songId,
                uri.toString()
            )

            lyrics
        }

    suspend fun removeCustom(
        song: Song
    ): XvoxLyrics? =
        withContext(Dispatchers.IO) {
            val old =
                preferences.lyricsUri(
                    song.id
                ).first()

            preferences.setLyricsUri(
                song.id,
                null
            )

            old?.let { value ->
                runCatching {
                    appContext.contentResolver
                        .releasePersistableUriPermission(
                            Uri.parse(value),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }
            }

            loadInternal(
                song,
                includeCustom = false
            )
        }

    private fun readUserLyrics(
        uri: Uri
    ): XvoxLyrics? {
        if (!XvoxLyricsFileSupport.isSupported(appContext, uri)) return null
        val raw = readBoundedLyricsText(uri) ?: return null
        if (raw.isBlank() || !looksLikeReadableText(raw)) return null

        val synced =
            Regex(
                """\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]"""
            ).containsMatchIn(raw) || raw.contains("-->") || raw.contains("Dialogue:", ignoreCase = true)

        return XvoxLyricsParser.parse(
            raw = raw,
            source =
                if (synced) {
                    XvoxLyricsSource.USER_LRC
                } else {
                    XvoxLyricsSource.USER_TEXT
                }
        ).takeIf { it.lines.isNotEmpty() }
    }

    /** Avoid treating a large archive/image stream as a lyrics text document. */
    private fun readBoundedLyricsText(uri: Uri): String? = runCatching {
        appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val buffer = CharArray(8 * 1024)
            val text = StringBuilder()
            while (true) {
                val read = reader.read(buffer)
                if (read <= 0) break
                if (text.length + read > MAX_LYRICS_CHARACTERS) return@use null
                text.append(buffer, 0, read)
            }
            text.toString().removePrefix("\uFEFF")
        }
    }.getOrNull()

    private fun looksLikeReadableText(raw: String): Boolean {
        if ('\u0000' in raw) return false
        val replacementCharacters = raw.count { it == '\uFFFD' }
        return replacementCharacters * 20 < raw.length.coerceAtLeast(1)
    }

    private companion object {
        const val MAX_LYRICS_CHARACTERS = 1_000_000
    }
}
