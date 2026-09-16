package com.xvox.music.features.playlist

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XvoxPlaylistCoverStorage(
    context: Context
) {
    private val appContext =
        context.applicationContext

    /**
     * Copies [source] into app storage and returns the new file URI.
     *
     * Every save writes a brand-new file name instead of reusing `playlist_<id>.img`; the image
     * loaders cache by URI, so overwriting one fixed file made a second photo for the same playlist
     * keep showing the old image. The new URI is unique per write, so any cache is skipped and the
     * fresh file is decoded. Older copies of this playlist's cover are removed once the new one is
     * safely in place.
     */
    suspend fun persist(
        playlistId: String,
        source: Uri
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(appContext.filesDir, "playlist_covers")
            directory.mkdirs()

            val familyPrefix = "playlist_${playlistId}_"
            val target = File(directory, "${familyPrefix}${System.currentTimeMillis()}.img")
            val temporary = File(directory, "${target.name}.tmp")

            appContext.contentResolver.openInputStream(source)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null

            if (target.exists()) target.delete()
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }

            // Cleanup is best-effort; it must never fail a successful write.
            runCatching { cleanupFamily(directory, playlistId, keep = target.name) }
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    suspend fun delete(
        playlistId: String
    ) {
        withContext(Dispatchers.IO) {
            cleanupFamily(File(appContext.filesDir, "playlist_covers"), playlistId, keep = null)
        }
    }

    /** Removes every stored copy of one playlist's cover (legacy fixed name, temp, and timestamps). */
    private fun cleanupFamily(directory: File, playlistId: String, keep: String?) {
        val files = directory.listFiles() ?: return
        val legacyName = "playlist_$playlistId.img"
        val tempName = "playlist_$playlistId.tmp"
        val familyPrefix = "playlist_${playlistId}_"
        files.forEach { file ->
            val stale = file.name == legacyName || file.name == tempName ||
                (file.name.startsWith(familyPrefix) && file.name != keep)
            if (stale) file.delete()
        }
    }
}
