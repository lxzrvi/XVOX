package com.xvox.music.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.xvox.music.core.model.Song
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The result of one authoritative device-library scan.
 *
 * [songs] only contains rows which Android currently exposes as complete, non-empty, readable
 * audio files. The skipped counters are intentionally row counters: they explain why a provider
 * result did not become an item in the playable library without inventing a placeholder song.
 */
data class MediaStoreSongScan(
    val songs: List<Song>,
    val unavailableRowsSkipped: Int,
    val duplicateRowsSkipped: Int
)

class MediaStoreSongRepository(
    context: Context
) {
    private val resolver = context.applicationContext.contentResolver

    /**
     * MediaStore can assign an album ID even when its album-art endpoint has no readable bitmap.
     * Resolve each album only once during an IO library scan so a coverless song is represented by
     * null from the start. That lets every surface—including Now Playing and Media3 notification—
     * select the bundled XVOX fallback immediately instead of first attempting a dead URI.
     */
    private fun resolveAlbumArtwork(
        albumId: Long,
        availability: MutableMap<Long, Uri?>
    ): Uri? {
        if (albumId <= 0L) return null
        if (availability.containsKey(albumId)) return availability[albumId]
        val candidate = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
        // A positive descriptor length is ideal; UNKNOWN_LENGTH still proves the provider exposed
        // a readable stream. A missing/empty descriptor is a true no-cover item, not an image that
        // should flash blank while Compose/Media3 waits for an eventual error callback.
        val readable = runCatching {
            resolver.openAssetFileDescriptor(candidate, "r")?.use { descriptor ->
                descriptor.length != 0L
            } == true
        }.getOrDefault(false)
        return candidate.takeIf { readable }.also { availability[albumId] = it }
    }

    /**
     * A MediaStore row is not necessarily a usable track. Download managers can expose a row while
     * it is pending, leave a zero-byte/zero-duration record behind, or retain an entry after the
     * underlying file becomes inaccessible. Opening a descriptor is a cheap final capability check
     * which uses exactly the URI Media3 will later play.
     */
    private fun isReadableAudio(contentUri: Uri, sizeBytes: Long, durationMs: Long): Boolean {
        if (sizeBytes <= 0L || durationMs <= 0L) return false
        return runCatching {
            resolver.openAssetFileDescriptor(contentUri, "r")?.use { descriptor ->
                // -1 is AssetFileDescriptor.UNKNOWN_LENGTH and is valid for a provider stream.
                descriptor.length != 0L
            } == true
        }.getOrDefault(false)
    }

    /**
     * Deduplicate a *physical* file only. Title, artist, duration, and artwork are deliberately
     * not part of the key: two different files with matching metadata must remain two songs.
     *
     * DATA is preferred because it identifies the filesystem object that MediaStore indexed. When
     * scoped storage withholds DATA, volume + relative path + display name is the same physical
     * MediaStore location. The content URI is the final fallback, so uncertainty never collapses
     * distinct tracks.
     */
    private fun physicalIdentity(
        contentUri: Uri,
        dataPath: String?,
        volume: String,
        relativePath: String,
        displayName: String?
    ): String {
        val path = dataPath?.trim().orEmpty()
        if (path.isNotEmpty()) {
            val canonical = runCatching { File(path).canonicalPath }.getOrDefault(path)
            return "path:$canonical"
        }
        val name = displayName?.trim().orEmpty()
        if (relativePath.isNotBlank() && name.isNotBlank()) {
            return "location:${volume.trim()}/${relativePath.trim('/')}/$name"
        }
        return "uri:$contentUri"
    }

    /**
     * Performs one current MediaStore scan. The provider is queried afresh for every invocation;
     * this method never returns a cached list. It is shared by Home, service resume, headset, and
     * widget paths so they cannot disagree about whether a row is safe to play.
     */
    suspend fun scanSongs(): MediaStoreSongScan = withContext(Dispatchers.IO) {
        val songs = ArrayList<Song>()
        val seenPhysicalFiles = HashSet<String>()
        val albumArtworkAvailability = HashMap<Long, Uri?>()
        var unavailableRowsSkipped = 0
        var duplicateRowsSkipped = 0

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME
        ).apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.IS_PENDING)
                add(MediaStore.Audio.Media.RELATIVE_PATH)
                add(MediaStore.Audio.Media.VOLUME_NAME)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        // A null cursor is a provider/query failure, not evidence that a user has no music. Throw
        // so callers can keep their last verified library and report a truthful scan failure.
        val cursor = resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        ) ?: throw IllegalStateException("MediaStore audio query returned no cursor")

        cursor.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val pendingColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
            } else {
                -1
            }
            val relativeColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                -1
            }
            val volumeColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val duration = cursor.getLong(durationColumn).coerceAtLeast(0L)
                val sizeBytes = if (sizeColumn >= 0) cursor.getLong(sizeColumn).coerceAtLeast(0L) else 0L
                val isPending = pendingColumn >= 0 && cursor.getInt(pendingColumn) != 0
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                // Pending/empty/stale records are never surfaced as songs, even briefly.
                if (isPending || !isReadableAudio(contentUri, sizeBytes, duration)) {
                    unavailableRowsSkipped++
                    continue
                }

                val dataPath = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                val relativePath = if (relativeColumn >= 0) {
                    cursor.getString(relativeColumn).orEmpty().trim('/')
                } else {
                    ""
                }
                val volume = if (volumeColumn >= 0) {
                    cursor.getString(volumeColumn).orEmpty().ifBlank { "external" }
                } else {
                    "external"
                }
                val displayName = if (displayNameColumn >= 0) cursor.getString(displayNameColumn) else null
                val physicalIdentity = physicalIdentity(contentUri, dataPath, volume, relativePath, displayName)
                if (!seenPhysicalFiles.add(physicalIdentity)) {
                    duplicateRowsSkipped++
                    continue
                }

                val title = cursor.getString(titleColumn)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: displayName
                        ?.let { fileName -> fileName.substringBeforeLast('.', fileName) }
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    ?: "Unknown song"
                val rawArtist = cursor.getString(artistColumn)?.trim()
                val artist = rawArtist?.takeIf {
                    it.isNotEmpty() && !it.equals("<unknown>", ignoreCase = true)
                } ?: "Unknown artist"
                val albumId = cursor.getLong(albumColumn)
                val storageRoot = if (volume == "external_primary" || volume == "external") {
                    "/storage/emulated/0"
                } else {
                    "/storage/$volume"
                }
                val folderPath = dataPath?.let { File(it).parent } ?: "$storageRoot/$relativePath"
                val folderName = if (!dataPath.isNullOrBlank()) {
                    runCatching { File(dataPath).parentFile?.name }.getOrNull() ?: "Music"
                } else {
                    relativePath.substringAfterLast('/').ifBlank { "Music" }
                }

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        contentUri = contentUri,
                        artworkUri = resolveAlbumArtwork(albumId, albumArtworkAvailability),
                        duration = duration,
                        sizeBytes = sizeBytes,
                        folderName = folderName,
                        folderPath = com.xvox.music.features.home.FolderPaths.normalize(folderPath),
                        displayName = displayName.orEmpty()
                    )
                )
            }
        }

        MediaStoreSongScan(
            songs = songs,
            unavailableRowsSkipped = unavailableRowsSkipped,
            duplicateRowsSkipped = duplicateRowsSkipped
        )
    }

    /** Compatibility entry point for consumers which only need the safe playable-song list. */
    suspend fun loadSongs(): List<Song> = scanSongs().songs
}
