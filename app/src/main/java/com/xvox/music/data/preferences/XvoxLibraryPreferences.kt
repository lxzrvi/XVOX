package com.xvox.music.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class XvoxPlaylist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long,
    val coverSongIds: List<Long> = emptyList(),
    val customCoverUri: String? = null
)

class XvoxLibraryPreferences(
    private val context: Context
) {
    private object Keys {
        val liked = stringPreferencesKey("library_liked_ids")
        val hidden = stringPreferencesKey("library_hidden_ids")
        val playlists = stringPreferencesKey("library_playlists_json")
        /** Media URI -> app-owned preview of artwork that was also embedded into the edited file. */
        val songArtworkOverrides = stringPreferencesKey("library_song_artwork_overrides_json")
    }

    val likedSongIds: Flow<Set<Long>> = context.xvoxDataStore.data.map { it[Keys.liked].orEmpty() }
        .distinctUntilChanged().map(::decodeIds)
    val hiddenSongIds: Flow<Set<Long>> = context.xvoxDataStore.data.map { it[Keys.hidden].orEmpty() }
        .distinctUntilChanged().map(::decodeIds)
    val playlists: Flow<List<XvoxPlaylist>> = context.xvoxDataStore.data.map { it[Keys.playlists].orEmpty() }
        .distinctUntilChanged().map(::decodePlaylists)
    /**
     * Immediate per-track artwork source after a successful file-tag write. MediaStore artwork is
     * album-scoped and can take time to re-index, so this preserves the exact chosen image in the
     * app while the embedded artwork remains the durable file-level source of truth.
     */
    val songArtworkOverrides: Flow<Map<String, String>> = context.xvoxDataStore.data
        .map { it[Keys.songArtworkOverrides].orEmpty() }
        .distinctUntilChanged()
        .map(::decodeArtworkOverrides)

    suspend fun setSongArtworkOverride(mediaUri: String, artworkUri: String?) {
        if (mediaUri.isBlank()) return
        context.xvoxDataStore.edit { prefs ->
            val current = decodeArtworkOverrides(prefs[Keys.songArtworkOverrides].orEmpty()).toMutableMap()
            if (artworkUri.isNullOrBlank()) current.remove(mediaUri) else current[mediaUri] = artworkUri
            prefs[Keys.songArtworkOverrides] = encodeArtworkOverrides(current)
        }
    }

    suspend fun setLiked(songId: Long, liked: Boolean) {
        context.xvoxDataStore.edit { prefs ->
            val ids = decodeIds(prefs[Keys.liked].orEmpty()).toMutableSet()
            if (liked) ids.add(songId) else ids.remove(songId)
            prefs[Keys.liked] = ids.joinToString(",")
        }
    }

    suspend fun setAllLiked(ids: Set<Long>) {
        context.xvoxDataStore.edit { prefs ->
            prefs[Keys.liked] = ids.joinToString(",")
        }
    }

    suspend fun savePlaylist(playlist: XvoxPlaylist): XvoxPlaylist? {
        return updatePlaylist(playlist.id) { playlist }
    }

    suspend fun restoreSong(songId: Long) {
        context.xvoxDataStore.edit { prefs -> prefs[Keys.hidden] = (decodeIds(prefs[Keys.hidden].orEmpty()) - songId).joinToString(",") }
    }

    /**
     * Forget a soft-delete/bin record after its backing MediaStore item has been permanently
     * removed. This intentionally does not describe the action as a restore: there is no file to
     * make visible again.
     */
    suspend fun discardHiddenSong(songId: Long) {
        context.xvoxDataStore.edit { prefs ->
            prefs[Keys.hidden] = (decodeIds(prefs[Keys.hidden].orEmpty()) - songId).joinToString(",")
        }
    }

    suspend fun restoreAllSongs() { context.xvoxDataStore.edit { it.remove(Keys.hidden) } }
    suspend fun hideSong(songId: Long) {
        context.xvoxDataStore.edit { prefs ->
            val ids = decodeIds(prefs[Keys.hidden].orEmpty()).toMutableSet()
            ids.add(songId)
            prefs[Keys.hidden] = ids.joinToString(",")
        }
    }

    suspend fun createPlaylist(name: String, songIds: Collection<Long>): XvoxPlaylist? {
        val clean = name.trim()
        if (clean.isEmpty()) return null

        val now = System.currentTimeMillis()
        val cleanSongIds = songIds.distinct()
        val playlist = XvoxPlaylist(
            id = now.toString(),
            name = clean,
            songIds = cleanSongIds,
            createdAt = now,
            coverSongIds = cleanSongIds.take(4)
        )

        context.xvoxDataStore.edit { prefs ->
            val current = decodePlaylists(prefs[Keys.playlists].orEmpty())
            prefs[Keys.playlists] = encodePlaylists(current + playlist)
        }
        return playlist
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: Long): XvoxPlaylist? =
        updatePlaylist(playlistId) { playlist ->
            val songs = (playlist.songIds + songId).distinct()
            playlist.copy(
                songIds = songs,
                coverSongIds = if (playlist.customCoverUri == null && playlist.coverSongIds.size < 4) {
                    (playlist.coverSongIds + songId).distinct().take(4)
                } else {
                    playlist.coverSongIds
                }
            )
        }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long): XvoxPlaylist? =
        updatePlaylist(playlistId) { playlist ->
            val songs = playlist.songIds.filterNot { it == songId }
            val retainedCover = playlist.coverSongIds.filter { it != songId && it in songs }
            playlist.copy(
                songIds = songs,
                coverSongIds = (retainedCover + songs.filterNot { it in retainedCover }).distinct().take(4)
            )
        }

    suspend fun renamePlaylist(playlistId: String, name: String): XvoxPlaylist? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        return updatePlaylist(playlistId) { it.copy(name = clean) }
    }

    suspend fun setPlaylistCover(
        playlistId: String,
        coverSongIds: List<Long>,
        customCoverUri: String?
    ): XvoxPlaylist? = updatePlaylist(playlistId) { playlist ->
        val validCover = coverSongIds.filter { it in playlist.songIds }.distinct().take(4)
        playlist.copy(coverSongIds = validCover, customCoverUri = customCoverUri)
    }

    suspend fun deletePlaylist(playlistId: String) {
        context.xvoxDataStore.edit { prefs ->
            val current = decodePlaylists(prefs[Keys.playlists].orEmpty())
            prefs[Keys.playlists] = encodePlaylists(current.filterNot { it.id == playlistId })
        }
    }

    private suspend fun updatePlaylist(
        playlistId: String,
        transform: (XvoxPlaylist) -> XvoxPlaylist
    ): XvoxPlaylist? {
        var result: XvoxPlaylist? = null
        context.xvoxDataStore.edit { prefs ->
            val current = decodePlaylists(prefs[Keys.playlists].orEmpty())
            val updated = current.map { playlist ->
                if (playlist.id == playlistId) {
                    transform(playlist).also { result = it }
                } else {
                    playlist
                }
            }
            prefs[Keys.playlists] = encodePlaylists(updated)
        }
        return result
    }

    private fun decodeIds(raw: String): Set<Long> =
        raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()

    private fun encodeArtworkOverrides(overrides: Map<String, String>): String {
        val objectValue = JSONObject()
        overrides.forEach { (mediaUri, artworkUri) ->
            if (mediaUri.isNotBlank() && artworkUri.isNotBlank()) objectValue.put(mediaUri, artworkUri)
        }
        return objectValue.toString()
    }

    private fun decodeArtworkOverrides(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val objectValue = JSONObject(raw)
            buildMap {
                val keys = objectValue.keys()
                while (keys.hasNext()) {
                    val mediaUri = keys.next()
                    val artworkUri = objectValue.optString(mediaUri).trim()
                    if (mediaUri.isNotBlank() && artworkUri.isNotBlank()) put(mediaUri, artworkUri)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodePlaylists(playlists: List<XvoxPlaylist>): String {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val songs = JSONArray()
            playlist.songIds.forEach { songs.put(it) }

            val coverSongs = JSONArray()
            playlist.coverSongIds.forEach { coverSongs.put(it) }

            val value = JSONObject()
                .put("id", playlist.id)
                .put("name", playlist.name)
                .put("createdAt", playlist.createdAt)
                .put("songs", songs)
                .put("coverSongs", coverSongs)

            playlist.customCoverUri?.let { value.put("customCoverUri", it) }
            array.put(value)
        }
        return array.toString()
    }

    private fun decodePlaylists(raw: String): List<XvoxPlaylist> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.getJSONObject(index)
                    val id = value.getString("id")
                    val songIds = decodeLongArray(value.optJSONArray("songs"))
                    val storedCover = decodeLongArray(value.optJSONArray("coverSongs"))
                    val coverSongs = if (storedCover.isEmpty()) {
                        songIds.take(4)
                    } else {
                        storedCover.filter { it in songIds }.distinct().take(4)
                    }
                    val customCover = value.optString("customCoverUri", "").takeIf { it.isNotBlank() }
                    val fallbackCreated = id.toLongOrNull() ?: 0L

                    add(
                        XvoxPlaylist(
                            id = id,
                            name = value.getString("name"),
                            songIds = songIds,
                            createdAt = value.optLong("createdAt", fallbackCreated),
                            coverSongIds = coverSongs,
                            customCoverUri = customCover
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun decodeLongArray(array: JSONArray?): List<Long> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getLong(index))
            }
        }
    }
}
