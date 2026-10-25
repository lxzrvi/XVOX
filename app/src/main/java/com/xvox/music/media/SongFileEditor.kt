package com.xvox.music.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.xvox.music.core.model.Song
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory

/** Whether a requested song edit replaces the selected file or creates a separately indexed copy. */
enum class SongFileEditDestination { ORIGINAL, COPY }

data class SongFileEditRequest(
    val title: String,
    val coverUri: Uri?,
    val destination: SongFileEditDestination
)

sealed interface SongFileEditOutcome {
    data class Saved(
        /** Existing URI for an original mutation, or the newly inserted URI for a copy. */
        val mediaUri: Uri,
        /** App-owned JPEG used until MediaStore finishes regenerating album artwork. */
        val artworkPreviewUri: Uri?,
        val createdCopy: Boolean
    ) : SongFileEditOutcome

    data class Failed(val message: String) : SongFileEditOutcome
}

/**
 * Writes a song's title and optional artwork into the actual audio bytes, never into a UI-only
 * placeholder. MediaStore's write-consent sheet is launched by the composable before [apply] is
 * called for an existing file. Copy mode inserts a new pending MediaStore item so the original
 * stays untouched.
 */
class SongFileEditor(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    private data class SourceInfo(
        val displayName: String,
        val title: String,
        val mimeType: String,
        val relativePath: String
    )

    /** Android 13+ requires a one-time system consent for editing a shared MediaStore item. */
    fun originalWriteRequest(song: Song) = runCatching {
        MediaStore.createWriteRequest(resolver, listOf(song.contentUri))
    }.getOrNull()

    suspend fun apply(song: Song, request: SongFileEditRequest): SongFileEditOutcome =
        withContext(Dispatchers.IO) {
            try {
                val cleanTitle = request.title.trim()
                if (cleanTitle.isEmpty()) return@withContext SongFileEditOutcome.Failed("Song name cannot be empty")

                val info = readSourceInfo(song)
                val editDirectory = File(appContext.cacheDir, "song_file_edits").apply { mkdirs() }
                val extension = extensionFor(info.displayName, info.mimeType)
                val originalBytes = File.createTempFile("xvox_original_", ".${extension}", editDirectory)
                val editableBytes = File.createTempFile("xvox_editable_", ".${extension}", editDirectory)
                var previewCover: File? = null
                try {
                    copyMediaToFile(song.contentUri, originalBytes)
                    originalBytes.copyTo(editableBytes, overwrite = true)
                    previewCover = request.coverUri?.let { persistCoverPreview(it) }
                    rewriteAudioTags(editableBytes, cleanTitle, previewCover)

                    val targetUri = when (request.destination) {
                        SongFileEditDestination.ORIGINAL -> {
                            replaceOriginal(
                                mediaUri = song.contentUri,
                                backup = originalBytes,
                                edited = editableBytes,
                                displayName = displayNameFor(info, cleanTitle, copy = false),
                                title = cleanTitle
                            )
                            song.contentUri
                        }

                        SongFileEditDestination.COPY -> createCopy(
                            info = info,
                            edited = editableBytes,
                            title = cleanTitle
                        )
                    }
                    SongFileEditOutcome.Saved(
                        mediaUri = targetUri,
                        artworkPreviewUri = previewCover?.let(Uri::fromFile),
                        createdCopy = request.destination == SongFileEditDestination.COPY
                    )
                } catch (cancelled: CancellationException) {
                    previewCover?.delete()
                    throw cancelled
                } catch (error: Exception) {
                    previewCover?.delete()
                    SongFileEditOutcome.Failed(editErrorMessage(error))
                } finally {
                    originalBytes.delete()
                    editableBytes.delete()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                SongFileEditOutcome.Failed(editErrorMessage(error))
            }
        }

    private fun readSourceInfo(song: Song): SourceInfo {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.Audio.Media.TITLE
        )
        resolver.query(song.contentUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                fun value(column: String): String = cursor.getColumnIndex(column)
                    .takeIf { it >= 0 }
                    ?.let(cursor::getString)
                    .orEmpty()
                return SourceInfo(
                    displayName = value(MediaStore.MediaColumns.DISPLAY_NAME).ifBlank { "${song.title}.mp3" },
                    title = value(MediaStore.Audio.Media.TITLE).ifBlank { song.title },
                    mimeType = value(MediaStore.MediaColumns.MIME_TYPE).ifBlank { "audio/mpeg" },
                    relativePath = value(MediaStore.MediaColumns.RELATIVE_PATH)
                )
            }
        }
        return SourceInfo(
            displayName = "${song.title}.mp3",
            title = song.title,
            mimeType = "audio/mpeg",
            relativePath = "Music/XVOX"
        )
    }

    private fun copyMediaToFile(source: Uri, target: File) {
        resolver.openInputStream(source)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("The original audio file is no longer readable")
        if (target.length() <= 0L) throw IllegalStateException("The original audio file is empty")
    }

    /**
     * Transcodes the picked image to a bounded JPEG. This keeps the embedded tag portable across
     * common MP3, M4A, FLAC and OGG readers and avoids writing a multi-megabyte camera original.
     */
    private fun persistCoverPreview(source: Uri): File {
        val directory = File(appContext.filesDir, "song_artwork").apply { mkdirs() }
        val sourceHash = Integer.toHexString(source.toString().hashCode())
        val target = File(directory, "song_cover_${System.currentTimeMillis()}_${sourceHash}.jpg")
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, source)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longestEdge = maxOf(info.size.width, info.size.height).coerceAtLeast(1)
            if (longestEdge > MAX_COVER_EDGE) {
                val scale = MAX_COVER_EDGE.toFloat() / longestEdge.toFloat()
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
        try {
            FileOutputStream(target).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                    throw IllegalStateException("The selected image could not be encoded")
                }
            }
        } catch (error: Exception) {
            target.delete()
            throw error
        } finally {
            bitmap.recycle()
        }
        if (target.length() <= 0L) {
            target.delete()
            throw IllegalStateException("The selected image is empty")
        }
        return target
    }

    /** Uses a seekable cache file because ContentResolver streams do not support tag rewriting. */
    private fun rewriteAudioTags(audio: File, title: String, cover: File?) {
        // The dependency is also used on desktop; select its Android-safe filesystem behaviour.
        org.jaudiotagger.tag.TagOptionSingleton.getInstance().setAndroid(true)
        val audioFile = AudioFileIO.read(audio)
        val tag = audioFile.tagOrCreateAndSetDefault
        tag.setField(FieldKey.TITLE, title)
        if (cover != null) {
            val artwork = ArtworkFactory.createArtworkFromFile(cover)
            tag.deleteArtworkField()
            tag.setField(artwork)
        }
        audioFile.setTag(tag)
        audioFile.commit()
    }

    /**
     * The edited temp file is complete before the original is opened for output. If an output error
     * occurs, restore the untouched byte-for-byte cache copy as a best-effort safety net.
     */
    private fun replaceOriginal(
        mediaUri: Uri,
        backup: File,
        edited: File,
        displayName: String,
        title: String
    ) {
        try {
            writeFileToMedia(edited, mediaUri)
            updateMediaColumns(mediaUri, displayName, title, pending = null)
        } catch (error: Exception) {
            runCatching { writeFileToMedia(backup, mediaUri) }
            throw error
        }
    }

    private fun createCopy(info: SourceInfo, edited: File, title: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayNameFor(info, title, copy = true))
            put(MediaStore.MediaColumns.MIME_TYPE, info.mimeType)
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, info.relativePath.ifBlank { "Music/XVOX" })
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val target = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Android could not create the new audio file")
        try {
            writeFileToMedia(edited, target)
            updateMediaColumns(target, displayNameFor(info, title, copy = true), title, pending = false)
            return target
        } catch (error: Exception) {
            runCatching { resolver.delete(target, null, null) }
            throw error
        }
    }

    private fun writeFileToMedia(source: File, target: Uri) {
        resolver.openOutputStream(target, "wt")?.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Android denied writing this audio file")
    }

    private fun updateMediaColumns(mediaUri: Uri, displayName: String, title: String, pending: Boolean?) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.TITLE, title)
            if (pending != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, if (pending) 1 else 0)
            }
        }
        if (resolver.update(mediaUri, values, null, null) <= 0) {
            throw IllegalStateException("Android did not confirm the file metadata update")
        }
    }

    private fun displayNameFor(info: SourceInfo, title: String, copy: Boolean): String {
        // Cover-only edits must not unexpectedly rename a file whose display filename differs
        // from its embedded title. A deliberate title change, or a new copy, gets a new filename.
        if (!copy && title.equals(info.title, ignoreCase = true)) return info.displayName
        val extension = extensionFor(info.displayName, info.mimeType)
        val existingBase = info.displayName.substringBeforeLast('.', info.displayName).trim()
        val safeTitle = title
            .replace(Regex("[\\\\/:*?\"<>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { existingBase.ifBlank { "XVOX song" } }
        val base = if (copy && safeTitle.equals(existingBase, ignoreCase = true)) "$safeTitle (Copy)" else safeTitle
        return "$base.$extension"
    }

    private fun extensionFor(displayName: String, mimeType: String): String {
        val existing = displayName.substringAfterLast('.', "").trim().lowercase()
        if (existing.isNotBlank() && existing.length <= 8) return existing
        return when (mimeType.lowercase()) {
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/ogg" -> "ogg"
            "audio/opus" -> "opus"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/aac", "audio/aac-adts" -> "aac"
            else -> "mp3"
        }
    }

    private fun editErrorMessage(error: Exception): String {
        val detail = error.message?.trim().orEmpty()
        return when {
            detail.contains("denied", ignoreCase = true) -> "Android did not allow that file to be edited"
            detail.contains("readable", ignoreCase = true) || detail.contains("empty", ignoreCase = true) -> detail
            else -> "This audio format or file could not be updated"
        }
    }

    private companion object {
        const val MAX_COVER_EDGE = 1600
    }
}
