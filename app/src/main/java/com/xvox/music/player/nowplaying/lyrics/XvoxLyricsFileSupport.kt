package com.xvox.music.player.nowplaying.lyrics

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Shared picker and validation policy for manually attached lyrics. File providers do not agree on
 * MIME types for .lrc files, so validation gives a known filename extension precedence over a
 * generic MIME type. Archives, images, and other unrelated files are never attached as lyrics.
 */
object XvoxLyricsFileSupport {
    private val supportedExtensions = setOf("lrc", "txt", "srt", "vtt", "ass", "ssa")
    private val timedExtensions = setOf("srt", "vtt", "ass", "ssa")

    private val supportedMimeTypes = setOf(
        "text/plain",
        "text/x-lrc",
        "application/x-lrc",
        "application/x-subrip",
        "application/x-srt",
        "text/vtt",
        "text/x-vtt",
        "text/x-ass",
        "text/x-ssa"
    )

    fun isSupported(context: Context, uri: Uri): Boolean {
        val extension = extension(context, uri)
        if (extension.isNotBlank()) {
            return extension in supportedExtensions
        }

        val mimeType = context.contentResolver.getType(uri)
            .orEmpty()
            .substringBefore(';')
            .trim()
            .lowercase()
        return mimeType in supportedMimeTypes
    }

    /** File suffix is used only to enforce syntax that the named subtitle formats require. */
    fun extension(context: Context, uri: Uri): String =
        displayName(context, uri).substringAfterLast('.', "").trim().lowercase()

    fun requiresTimedSyntax(context: Context, uri: Uri): Boolean =
        extension(context, uri) in timedExtensions

    fun unsupportedMessage(): String = "Unsupported lyrics file or format"

    private fun displayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column).orEmpty() else ""
            }.orEmpty()
        }.getOrDefault("").ifBlank {
            uri.lastPathSegment.orEmpty().substringAfterLast('/')
        }
    }
}
