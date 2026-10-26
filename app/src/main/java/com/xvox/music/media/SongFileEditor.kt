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
    val destination: SongFileEditDestination,
    /** Keeps a cover-only save from changing a title that MediaStore has independently refreshed. */
    val renameRequested: Boolean
)

sealed interface SongFileEditOutcome {
    data class Saved(
        /** Existing URI for an original mutation, or the newly inserted URI for a copy. */
        val mediaUri: Uri,
        /** App-owned JPEG used until MediaStore finishes regenerating album artwork. */
        val artworkPreviewUri: Uri?,
        val createdCopy: Boolean,
        /** Truthful user-facing result; it distinguishes a file rename from a tag/artwork edit. */
        val message: String
    ) : SongFileEditOutcome

    data class Failed(val message: String) : SongFileEditOutcome
}

/**
 * Writes edits to real MediaStore files rather than cached UI state.
 *
 * Every MediaStore audio item can be renamed through its real display name and TITLE column.
 * Embedded cover art is inherently container-dependent, so XVOX only attempts a byte-level
 * artwork write for formats with a portable tag representation. Unsupported containers fail
 * clearly instead of pretending that artwork was changed.
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
            val changingCover = request.coverUri != null
            try {
                val cleanTitle = request.title.trim()
                if (cleanTitle.isEmpty()) {
                    return@withContext SongFileEditOutcome.Failed("Song name cannot be empty")
                }

                val info = readSourceInfo(song)
                val changingTitle = request.renameRequested
                val titleToWrite = if (changingTitle) cleanTitle else info.title
                if (!changingTitle && !changingCover) {
                    return@withContext SongFileEditOutcome.Failed(
                        "Choose a different song name or select a cover image before saving"
                    )
                }

                if (!changingCover) {
                    // A provider-level rename is a real filename / MediaStore metadata update and
                    // works for every indexed audio container, even ones without writable tags.
                    return@withContext renameEveryAudioFile(song, info, titleToWrite, request.destination)
                }

                val extension = extensionFor(info.displayName, info.mimeType)
                if (!supportsEmbeddedArtwork(extension)) {
                    return@withContext SongFileEditOutcome.Failed(
                        unsupportedArtworkMessage(extension)
                    )
                }

                rewriteTaggedAudio(
                    song = song,
                    info = info,
                    title = titleToWrite,
                    coverUri = request.coverUri,
                    destination = request.destination
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                SongFileEditOutcome.Failed(editErrorMessage(error, changingCover))
            }
        }

    /**
     * First try a portable embedded title update for containers with writable tags. If the source
     * file has malformed/unsupported tags, fall back to the actual MediaStore display name + TITLE
     * update, so a normal rename still works for that audio file rather than falsely succeeding.
     */
    private fun renameEveryAudioFile(
        song: Song,
        info: SourceInfo,
        title: String,
        destination: SongFileEditDestination
    ): SongFileEditOutcome.Saved {
        val extension = extensionFor(info.displayName, info.mimeType)
        if (supportsWritableTags(extension)) {
            try {
                return rewriteTaggedAudio(
                    song = song,
                    info = info,
                    title = title,
                    coverUri = null,
                    destination = destination
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The direct MediaStore rename below is still a genuine file/title update. It is
                // specifically what lets unknown, damaged, and tagless audio containers rename.
            }
        }

        return renameViaMediaStore(song, info, title, destination)
    }

    private fun renameViaMediaStore(
        song: Song,
        info: SourceInfo,
        title: String,
        destination: SongFileEditDestination
    ): SongFileEditOutcome.Saved = when (destination) {
        SongFileEditDestination.ORIGINAL -> {
            updateMediaColumns(
                mediaUri = song.contentUri,
                displayName = displayNameFor(info, title, copy = false),
                title = title,
                pending = null
            )
            SongFileEditOutcome.Saved(
                mediaUri = song.contentUri,
                artworkPreviewUri = null,
                createdCopy = false,
                message = "Renamed the original audio file"
            )
        }

        SongFileEditDestination.COPY -> {
            val editDirectory = File(appContext.cacheDir, "song_file_edits").apply { mkdirs() }
            val extension = extensionFor(info.displayName, info.mimeType)
            val rawCopy = File.createTempFile("xvox_rename_", ".${extension}", editDirectory)
            try {
                copyMediaToFile(song.contentUri, rawCopy)
                val target = createCopy(info, rawCopy, title)
                SongFileEditOutcome.Saved(
                    mediaUri = target,
                    artworkPreviewUri = null,
                    createdCopy = true,
                    message = "Created a renamed copy in your library"
                )
            } finally {
                rawCopy.delete()
            }
        }
    }

    /**
     * Uses seekable cache files because ContentResolver streams do not support tag rewriting. The
     * edited bytes are complete before an original is opened for output, so a failure can restore
     * the untouched byte-for-byte backup instead of leaving a partial track behind.
     */
    private fun rewriteTaggedAudio(
        song: Song,
        info: SourceInfo,
        title: String,
        coverUri: Uri?,
        destination: SongFileEditDestination
    ): SongFileEditOutcome.Saved {
        val editDirectory = File(appContext.cacheDir, "song_file_edits").apply { mkdirs() }
        val extension = extensionFor(info.displayName, info.mimeType)
        val originalBytes = File.createTempFile("xvox_original_", ".${extension}", editDirectory)
        val editableBytes = File.createTempFile("xvox_editable_", ".${extension}", editDirectory)
        var previewCover: File? = null

        return try {
            copyMediaToFile(song.contentUri, originalBytes)
            originalBytes.copyTo(editableBytes, overwrite = true)
            previewCover = coverUri?.let(::persistCoverPreview)
            rewriteAudioTags(editableBytes, title, previewCover)

            val targetUri = when (destination) {
                SongFileEditDestination.ORIGINAL -> {
                    replaceOriginal(
                        mediaUri = song.contentUri,
                        backup = originalBytes,
                        edited = editableBytes,
                        displayName = displayNameFor(info, title, copy = false),
                        title = title
                    )
                    song.contentUri
                }

                SongFileEditDestination.COPY -> createCopy(
                    info = info,
                    edited = editableBytes,
                    title = title
                )
            }

            SongFileEditOutcome.Saved(
                mediaUri = targetUri,
                artworkPreviewUri = previewCover?.let(Uri::fromFile),
                createdCopy = destination == SongFileEditDestination.COPY,
                message = when {
                    coverUri != null && destination == SongFileEditDestination.COPY ->
                        "Created an edited copy in your library"
                    coverUri != null -> "Updated the original audio file"
                    destination == SongFileEditDestination.COPY -> "Created a renamed copy in your library"
                    else -> "Renamed the original audio file"
                }
            )
        } catch (cancelled: CancellationException) {
            previewCover?.delete()
            throw cancelled
        } catch (error: Exception) {
            previewCover?.delete()
            throw error
        } finally {
            originalBytes.delete()
            editableBytes.delete()
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
     * Transcodes the picked image to a bounded JPEG. This keeps embedded tags portable and avoids
     * writing a multi-megabyte camera original into every selected song.
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

    /** Writes only portable TITLE/artwork tags for the containers preflighted above. */
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
        if (!copy && title == info.title) return info.displayName
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

    private fun supportsWritableTags(extension: String): Boolean =
        extension.lowercase() in WritableTagExtensions

    private fun supportsEmbeddedArtwork(extension: String): Boolean =
        extension.lowercase() in ArtworkTagExtensions

    private fun unsupportedArtworkMessage(extension: String): String {
        val label = extension.uppercase().ifBlank { "this" }
        return "$label files can be renamed, but XVOX cannot safely embed cover art in this format. Use a tag-writable format such as MP3, M4A/MP4, FLAC, OGG, WAV, WMA, AIFF, or DSF."
    }

    private fun editErrorMessage(error: Exception, changingCover: Boolean): String {
        val detail = error.message?.trim().orEmpty()
        return when {
            detail.contains("denied", ignoreCase = true) -> "Android did not allow that file to be edited"
            detail.contains("readable", ignoreCase = true) || detail.contains("empty", ignoreCase = true) -> detail
            changingCover -> "This audio file did not accept embedded cover art"
            else -> "Android could not rename this audio file"
        }
    }

    private companion object {
        const val MAX_COVER_EDGE = 1600
        // JavaAudioTagger 3.0.1 provides writers for these containers. Each artwork write is still
        // committed to a temp copy first; a malformed or non-portable individual file returns a
        // truthful failure instead of damaging the source or showing a fake cover update.
        val WritableTagExtensions = setOf(
            "mp3", "m4a", "m4b", "m4p", "mp4", "flac", "ogg", "oga",
            "wav", "wma", "aif", "aifc", "aiff", "dsf"
        )
        val ArtworkTagExtensions = WritableTagExtensions
    }
}
