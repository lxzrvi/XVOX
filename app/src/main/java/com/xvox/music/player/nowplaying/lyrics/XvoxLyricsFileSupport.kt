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

    /**
     * Include the generic binary MIME only because several Android document providers label .lrc
     * files that way. [isSupported] still rejects ZIPs, images, and every unknown extension after
     * selection, before app state or lyric preferences can be changed.
     */
    fun pickerMimeTypes(): Array<String> = arrayOf(
        "text/plain",
        "text/*",
        "application/x-subrip",
        "text/vtt",
        "application/octet-stream"
    )

    fun isSupported(context: Context, uri: Uri): Boolean {
        val name = displayName(context, uri)
        val extension = name.substringAfterLast('.', "").trim().lowercase()
        if (extension.isNotBlank()) {
            return extension in supportedExtensions
        }

        val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase()
        return mimeType in supportedMimeTypes || mimeType.startsWith("text/")
    }

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
