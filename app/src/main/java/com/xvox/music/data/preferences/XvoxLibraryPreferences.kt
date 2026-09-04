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
    val songIds: List<Long>
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
            preferences ->

            val ids =
                decodeIds(
                    preferences[
                        Keys.liked
                    ].orEmpty()
                ).toMutableSet()

            if (liked) {
                ids.add(songId)
            } else {
                ids.remove(songId)
            }

            preferences[Keys.liked] =
                ids.joinToString(",")
        }
    }

    suspend fun hideSong(
        songId: Long
    ) {
        context.xvoxDataStore.edit {
            preferences ->

            val ids =
                decodeIds(
                    preferences[
                        Keys.hidden
                    ].orEmpty()
                ).toMutableSet()

            ids.add(songId)

            preferences[Keys.hidden] =
                ids.joinToString(",")
        }
    }

    suspend fun createPlaylist(
        name: String,
        songIds: Collection<Long>
    ): XvoxPlaylist? {
        val clean =
            name.trim()

        if (clean.isEmpty()) {
            return null
        }

        val playlist =
            XvoxPlaylist(
                id =
                    System.currentTimeMillis()
                        .toString(),
                name = clean,
                songIds =
                    songIds.distinct()
            )

        context.xvoxDataStore.edit {
            preferences ->

            val current =
                decodePlaylists(
                    preferences[
                        Keys.playlists
                    ].orEmpty()
                )

            preferences[
                Keys.playlists
            ] =
                encodePlaylists(
                    current + playlist
                )
        }

        return playlist
    }

    suspend fun addSongToPlaylist(
        playlistId: String,
        songId: Long
    ): XvoxPlaylist? {
        var updatedPlaylist:
            XvoxPlaylist? = null

        context.xvoxDataStore.edit {
            preferences ->

            val current =
                decodePlaylists(
                    preferences[
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
                        playlist.copy(
                            songIds =
                                (
                                    playlist.songIds +
                                        songId
                                    )
                                    .distinct()
                        ).also {
                            updatedPlaylist = it
                        }
                    } else {
                        playlist
                    }
                }

            preferences[
                Keys.playlists
            ] =
                encodePlaylists(updated)
        }

        return updatedPlaylist
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
                    val objectValue =
                        array.getJSONObject(
                            index
                        )

                    val songs =
                        objectValue
                            .optJSONArray(
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

                    add(
                        XvoxPlaylist(
                            id =
                                objectValue
                                    .getString(
                                        "id"
                                    ),
                            name =
                                objectValue
                                    .getString(
                                        "name"
                                    ),
                            songIds =
                                songIds
                        )
                    )
                }
            }
        }.getOrDefault(
            emptyList()
        )
    }
}
