package com.xvox.music.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class XvoxPlaylist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long
)

class XvoxLibraryPreferences(
    private val context: Context
) {
    private object Keys {
        val liked =
            stringPreferencesKey(
                "library_liked_ids"
            )

        val hidden =
            stringPreferencesKey(
                "library_hidden_ids"
            )

        val playlists =
            stringPreferencesKey(
                "library_playlists_json"
            )
    }

    val likedSongIds: Flow<Set<Long>> =
        context.xvoxDataStore.data
            .map {
                decodeIds(
                    it[Keys.liked]
                        .orEmpty()
                )
            }

    val hiddenSongIds: Flow<Set<Long>> =
        context.xvoxDataStore.data
            .map {
                decodeIds(
                    it[Keys.hidden]
                        .orEmpty()
                )
            }

    val playlists: Flow<List<XvoxPlaylist>> =
        context.xvoxDataStore.data
            .map {
                decodePlaylists(
                    it[Keys.playlists]
                        .orEmpty()
                )
            }

    suspend fun setLiked(
        songId: Long,
        liked: Boolean
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            val ids =
                decodeIds(
                    prefs[
                        Keys.liked
                    ].orEmpty()
                ).toMutableSet()

            if (liked) {
                ids.add(songId)
            } else {
                ids.remove(songId)
            }

            prefs[Keys.liked] =
                ids.joinToString(",")
        }
    }

    suspend fun hideSong(
        songId: Long
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            val ids =
                decodeIds(
                    prefs[
                        Keys.hidden
                    ].orEmpty()
                ).toMutableSet()

            ids.add(songId)

            prefs[Keys.hidden] =
                ids.joinToString(",")
        }
    }

    suspend fun createPlaylist(
        name: String,
        songIds: Collection<Long>
    ): XvoxPlaylist? {
        val clean = name.trim()

        if (clean.isEmpty()) {
            return null
        }

        val now =
            System.currentTimeMillis()

        val playlist =
            XvoxPlaylist(
                id = now.toString(),
                name = clean,
                songIds =
                    songIds.distinct(),
                createdAt = now
            )

        context.xvoxDataStore.edit {
            prefs ->

            val current =
                decodePlaylists(
                    prefs[
                        Keys.playlists
                    ].orEmpty()
                )

            prefs[Keys.playlists] =
                encodePlaylists(
                    current + playlist
                )
        }

        return playlist
    }

    suspend fun addSongToPlaylist(
        playlistId: String,
        songId: Long
    ): XvoxPlaylist? =
        updatePlaylist(
            playlistId
        ) {
            it.copy(
                songIds =
                    (
                        it.songIds +
                            songId
                        ).distinct()
            )
        }

    suspend fun removeSongFromPlaylist(
        playlistId: String,
        songId: Long
    ): XvoxPlaylist? =
        updatePlaylist(
            playlistId
        ) {
            it.copy(
                songIds =
                    it.songIds.filterNot {
                        id ->
                        id == songId
                    }
            )
        }

    suspend fun renamePlaylist(
        playlistId: String,
        name: String
    ): XvoxPlaylist? {
        val clean = name.trim()

        if (clean.isEmpty()) {
            return null
        }

        return updatePlaylist(
            playlistId
        ) {
            it.copy(
                name = clean
            )
        }
    }

    suspend fun deletePlaylist(
        playlistId: String
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            val current =
                decodePlaylists(
                    prefs[
                        Keys.playlists
                    ].orEmpty()
                )

            prefs[Keys.playlists] =
                encodePlaylists(
                    current.filterNot {
                        it.id == playlistId
                    }
                )
        }
    }

    private suspend fun updatePlaylist(
        playlistId: String,
        transform: (
            XvoxPlaylist
        ) -> XvoxPlaylist
    ): XvoxPlaylist? {
        var result: XvoxPlaylist? =
            null

        context.xvoxDataStore.edit {
            prefs ->

            val current =
                decodePlaylists(
                    prefs[
                        Keys.playlists
                    ].orEmpty()
                )

            val updated =
                current.map {
                    playlist ->

                    if (
                        playlist.id ==
                        playlistId
                    ) {
                        transform(
                            playlist
                        ).also {
                            result = it
                        }
                    } else {
                        playlist
                    }
                }

            prefs[Keys.playlists] =
                encodePlaylists(updated)
        }

        return result
    }

    private fun decodeIds(
        raw: String
    ): Set<Long> =
        raw.split(",")
            .mapNotNull {
                it.toLongOrNull()
            }
            .toSet()

    private fun encodePlaylists(
        playlists: List<XvoxPlaylist>
    ): String {
        val array = JSONArray()

        playlists.forEach {
            playlist ->

            val songs = JSONArray()

            playlist.songIds.forEach {
                songs.put(it)
            }

            array.put(
                JSONObject()
                    .put(
                        "id",
                        playlist.id
                    )
                    .put(
                        "name",
                        playlist.name
                    )
                    .put(
                        "createdAt",
                        playlist.createdAt
                    )
                    .put(
                        "songs",
                        songs
                    )
            )
        }

        return array.toString()
    }

    private fun decodePlaylists(
        raw: String
    ): List<XvoxPlaylist> {
        if (raw.isBlank()) {
            return emptyList()
        }

        return runCatching {
            val array =
                JSONArray(raw)

            buildList {
                for (
                    index in
                    0 until array.length()
                ) {
                    val value =
                        array.getJSONObject(
                            index
                        )

                    val id =
                        value.getString("id")

                    val songs =
                        value.optJSONArray(
                            "songs"
                        )

                    val songIds =
                        buildList {
                            if (songs != null) {
                                for (
                                    songIndex in
                                    0 until
                                        songs.length()
                                ) {
                                    add(
                                        songs.getLong(
                                            songIndex
                                        )
                                    )
                                }
                            }
                        }

                    val fallbackCreated =
                        id.toLongOrNull()
                            ?: 0L

                    add(
                        XvoxPlaylist(
                            id = id,
                            name =
                                value.getString(
                                    "name"
                                ),
                            songIds =
                                songIds,
                            createdAt =
                                value.optLong(
                                    "createdAt",
                                    fallbackCreated
                                )
                        )
                    )
                }
            }
        }.getOrDefault(
            emptyList()
        )
    }
}
