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
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class XvoxMetadataReader(
    context: Context
) : SongMetadataReader {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    override suspend fun read(uri: Uri): SongMetadata = withContext(Dispatchers.IO) {
        val androidMetadata = readAndroidMetadata(uri)
        val deepMetadata = runCatching { readDeepMetadata(uri) }.getOrNull()
        merge(androidMetadata, deepMetadata)
    }

    private fun readAndroidMetadata(uri: Uri): SongMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            SongMetadata(
                uri = uri,
                title = retriever.value(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.value(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.value(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                albumArtist = retriever.value(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                composer = retriever.value(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
                genre = retriever.value(MediaMetadataRetriever.METADATA_KEY_GENRE),
                year = retriever.value(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull(),
                duration = retriever.value(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                bitrate = retriever.value(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            )
        } catch (_: Exception) {
            SongMetadata.empty(uri)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun MediaMetadataRetriever.value(key: Int): String? =
        extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }

    private fun readDeepMetadata(uri: Uri): SongMetadata? {
        val temp = createTemporaryAudioFile(uri) ?: return null
        return try {
            val audioFile = runCatching { AudioFileIO.read(temp) }.getOrNull()
            val tag = audioFile?.tag
            val header = audioFile?.audioHeader
            val lyrics = tag?.readLyrics() ?: readContainerLyrics(temp)

            SongMetadata(
                uri = uri,
                title = tag?.field(FieldKey.TITLE),
                artist = tag?.field(FieldKey.ARTIST),
                album = tag?.field(FieldKey.ALBUM),
                albumArtist = tag?.field(FieldKey.ALBUM_ARTIST),
                composer = tag?.field(FieldKey.COMPOSER),
                genre = tag?.field(FieldKey.GENRE),
                year = tag?.field(FieldKey.YEAR)?.take(4)?.toIntOrNull(),
                trackNumber = tag?.field(FieldKey.TRACK)?.substringBefore("/")?.toIntOrNull(),
                discNumber = tag?.field(FieldKey.DISC_NO)?.substringBefore("/")?.toIntOrNull(),
                duration = header?.trackLength?.toLong()?.times(1000L),
                bitrate = header?.bitRateAsNumber?.toInt(),
                sampleRate = header?.sampleRateAsNumber?.toInt(),
                lyrics = lyrics,
                comment = tag?.field(FieldKey.COMMENT)
            )
        } finally {
            temp.delete()
        }
    }

    private fun Tag.readLyrics(): String? {
        field(FieldKey.LYRICS)?.let { return normalizeLyrics(it) }

        val ids = listOf(
            "LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS",
            "UNSYNCED_LYRICS", "USLT", "SYLT", "©lyr"
        )
        for (id in ids) {
            val value = runCatching { getFirst(id) }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            if (value != null) return normalizeLyrics(value)
        }

        return runCatching {
            fields.asSequence().firstNotNullOfOrNull { field ->
                val id = field.id.uppercase()
                if (id.contains("LYRIC") || id.contains("USLT") || id.contains("SYLT")) {
                    nativeFieldText(field.toString())
                } else null
            }
        }.getOrNull()?.let(::normalizeLyrics)
    }

    private fun readContainerLyrics(file: File): String? = when (file.extension.lowercase()) {
        "m4a", "mp4" -> readMp4Lyrics(file)
        "flac", "ogg", "opus" -> readVorbisLikeLyrics(file)
        "mp3" -> readId3Lyrics(file)
        else -> null
    }

    private fun readMp4Lyrics(file: File): String? {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        val markers = listOf(
            byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte()),
            "LYRICS".toByteArray(StandardCharsets.UTF_8)
        )

        for (marker in markers) {
            var index = findBytes(bytes, marker, 0)
            while (index >= 0) {
                val text = findMp4DataAfter(bytes, index + marker.size)
                if (text != null && looksLikeLyrics(text)) {
                    return normalizeLyrics(text)
                }
                index = findBytes(bytes, marker, index + marker.size)
            }
        }
        return null
    }

    private fun findMp4DataAfter(bytes: ByteArray, from: Int): String? {
        val dataMarker = byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte())
        val dataId = findBytes(bytes, dataMarker, from)
        if (dataId < 4 || dataId - from > 128) return null

        val boxStart = dataId - 4
        val boxSize = readUInt32(bytes, boxStart)
        if (boxSize < 16) return null

        val payloadStart = dataId + 12
        val payloadEnd = (boxStart + boxSize).coerceAtMost(bytes.size)
        if (payloadStart >= payloadEnd) return null

        val payload = bytes.copyOfRange(payloadStart, payloadEnd)
        return runCatching { String(payload, StandardCharsets.UTF_8) }.getOrNull()
    }

    private fun readVorbisLikeLyrics(file: File): String? {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        val text = String(bytes, StandardCharsets.ISO_8859_1)
        val fieldNames = listOf("LYRICS=", "UNSYNCEDLYRICS=", "UNSYNCED_LYRICS=", "DESCRIPTION=")

        for (name in fieldNames) {
            val start = text.indexOf(name, ignoreCase = true)
            if (start < 0) continue

            val valueStart = start + name.length
            val nextField = Regex("""[\u0000-\u001F][A-Za-z0-9_ -]{2,32}=""")
                .find(text, valueStart)?.range?.first ?: text.length

            val candidate = text.substring(valueStart, nextField).trim('\u0000', '\r', '\n')
            if (looksLikeLyrics(candidate)) {
                return normalizeLyrics(candidate)
            }
        }
        return null
    }

    private fun readId3Lyrics(file: File): String? {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        val marker = "USLT".toByteArray(StandardCharsets.ISO_8859_1)
        val frame = findBytes(bytes, marker, 0)
        if (frame < 0 || frame + 10 >= bytes.size) return null

        val size = readUInt32(bytes, frame + 4)
        if (size <= 0 || frame + 10 + size > bytes.size) return null

        val payload = bytes.copyOfRange(frame + 10, frame + 10 + size)
        if (payload.size < 5) return null

        val encoding = payload[0].toInt() and 0xFF
        val charset = when (encoding) {
            1 -> Charset.forName("UTF-16")
            2 -> Charset.forName("UTF-16BE")
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }

        val text = runCatching { String(payload, charset) }.getOrNull() ?: return null
        val timestampStart = Regex("""\[\d{1,3}:\d{2}""").find(text)?.range?.first
        if (timestampStart != null) {
            return normalizeLyrics(text.substring(timestampStart))
        }

        val parts = text.split('\u0000').map { it.trim() }.filter { it.isNotEmpty() }
        return parts.lastOrNull { it.length > 8 }?.let(::normalizeLyrics)
    }

    private fun looksLikeLyrics(text: String): Boolean {
        if (text.isBlank()) return false
        return Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]""").containsMatchIn(text) || text.count { it == '\n' } >= 2
    }

    private fun nativeFieldText(raw: String): String? {
        var text = raw.trim()
        if (text.isBlank()) return null
        val markers = listOf("Text=", "Text:", "Lyrics=", "Lyrics:", "Content=", "Content:")
        for (marker in markers) {
            val index = text.indexOf(marker, ignoreCase = true)
            if (index >= 0) {
                val candidate = text.substring(index + marker.length).trim()
                if (candidate.isNotEmpty()) {
                    text = candidate
                    break
                }
            }
        }
        return text.takeIf { it.isNotBlank() }
    }

    private fun normalizeLyrics(raw: String): String = raw
        .replace("\\r\\n", "\n")
        .replace("\\n", "\n")
        .replace("\\r", "\n")
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim('\u0000', ' ', '\n', '\t')

    private fun findBytes(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || source.size < target.size) return -1
        var index = fromIndex.coerceAtLeast(0)
        val last = source.size - target.size

        while (index <= last) {
            var matches = true
            for (offset in target.indices) {
                if (source[index + offset] != target[offset]) {
                    matches = false
                    break
                }
            }
            if (matches) return index
            index++
        }
        return -1
    }

    private fun readUInt32(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) return -1
        val value = ((bytes[offset].toLong() and 0xFF) shl 24) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 8) or
            (bytes[offset + 3].toLong() and 0xFF)

        return if (value in 1..Int.MAX_VALUE) value.toInt() else -1
    }

    private fun createTemporaryAudioFile(uri: Uri): File? {
        val mime = resolver.getType(uri).orEmpty().lowercase()
        val extension = when {
            mime == "audio/mpeg" -> ".mp3"
            mime.contains("flac") -> ".flac"
            mime.contains("m4a") || mime == "audio/mp4" -> ".m4a"
            mime.contains("opus") -> ".opus"
            mime.contains("ogg") -> ".ogg"
            mime.contains("wav") -> ".wav"
            else -> extensionFromUri(uri)
        }

        val temp = File.createTempFile("xvox_metadata_", extension, appContext.cacheDir)
        return try {
            resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().buffered().use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            } ?: run {
                temp.delete()
                return null
            }
            temp
        } catch (_: Exception) {
            temp.delete()
            null
        }
    }

    private fun extensionFromUri(uri: Uri): String {
        val extension = uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp3", "flac", "m4a", "mp4", "ogg", "opus", "wav" -> ".$extension"
            else -> ".audio"
        }
    }

    private fun Tag.field(key: FieldKey): String? =
        runCatching { getFirst(key) }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    private fun merge(primary: SongMetadata, deep: SongMetadata?): SongMetadata {
        if (deep == null) return primary
        return SongMetadata(
            uri = primary.uri,
            title = deep.title ?: primary.title,
            artist = deep.artist ?: primary.artist,
            album = deep.album ?: primary.album,
            albumArtist = deep.albumArtist ?: primary.albumArtist,
            composer = deep.composer ?: primary.composer,
            genre = deep.genre ?: primary.genre,
            year = deep.year ?: primary.year,
            trackNumber = deep.trackNumber,
            discNumber = deep.discNumber,
            duration = primary.duration ?: deep.duration,
            bitrate = primary.bitrate ?: deep.bitrate,
            sampleRate = deep.sampleRate,
            lyrics = deep.lyrics,
            comment = deep.comment,
            artworkCacheKey = null
        )
    }
}
