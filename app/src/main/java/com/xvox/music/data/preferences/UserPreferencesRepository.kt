package com.xvox.music.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.xvoxDataStore by
    preferencesDataStore(
        name = "xvox_preferences"
    )

class UserPreferencesRepository(
    private val context: Context
) {
    private object Keys {
        val setupCompleted =
            booleanPreferencesKey(
                "setup_completed"
            )
        val username =
            stringPreferencesKey(
                "username"
            )
        val selectedPfp =
            stringPreferencesKey(
                "selected_pfp"
            )
        val customPfpUri =
            stringPreferencesKey(
                "custom_pfp_uri"
            )
        val recentSongIds =
            stringPreferencesKey(
                "recent_song_ids"
            )
        val lyricsUris =
            stringPreferencesKey(
                "lyrics_uris"
            )
        val lastPlayedSongId =
            longPreferencesKey(
                "last_played_song_id"
            )
    }

    val preferences: Flow<UserPreferences> =
        context.xvoxDataStore.data.map {
            prefs ->

            UserPreferences(
                setupCompleted =
                    prefs[
                        Keys.setupCompleted
                    ] ?: false,
                username =
                    prefs[
                        Keys.username
                    ].orEmpty(),
                selectedPfp =
                    prefs[
                        Keys.selectedPfp
                    ] ?: "DEFAULT",
                customPfpUri =
                    prefs[
                        Keys.customPfpUri
                    ]
            )
        }

    val recentSongIds: Flow<List<Long>> =
        context.xvoxDataStore.data.map {
            prefs ->

            prefs[
                Keys.recentSongIds
            ]
                .orEmpty()
                .split(",")
                .mapNotNull {
                    it.toLongOrNull()
                }
                .distinct()
                .take(20)
        }

    val lastPlayedSongId:
        Flow<Long?> =
        context.xvoxDataStore.data.map {
            prefs ->

            prefs[
                Keys.lastPlayedSongId
            ]
        }

    suspend fun setLastPlayedSongId(
        songId: Long?
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            if (songId == null) {
                prefs.remove(
                    Keys.lastPlayedSongId
                )
            } else {
                prefs[
                    Keys.lastPlayedSongId
                ] = songId
            }
        }
    }

    suspend fun completeSetup(
        username: String,
        selectedPfp: String,
        customPfpUri: String?
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            prefs[Keys.username] =
                username
            prefs[Keys.selectedPfp] =
                selectedPfp

            if (
                customPfpUri != null
            ) {
                prefs[
                    Keys.customPfpUri
                ] = customPfpUri
            } else {
                prefs.remove(
                    Keys.customPfpUri
                )
            }

            prefs[
                Keys.setupCompleted
            ] = true
        }
    }

    suspend fun recordRecentSong(
        songId: Long
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            val current =
                prefs[
                    Keys.recentSongIds
                ]
                    .orEmpty()
                    .split(",")
                    .mapNotNull {
                        it.toLongOrNull()
                    }

            prefs[
                Keys.recentSongIds
            ] =
                buildList {
                    add(songId)
                    addAll(
                        current.filterNot {
                            it == songId
                        }
                    )
                }
                    .take(20)
                    .joinToString(",")
        }
    }

    fun lyricsUri(
        songId: Long
    ): Flow<String?> =
        context.xvoxDataStore.data.map {
            prefs ->

            decodeLyricsUris(
                prefs[
                    Keys.lyricsUris
                ].orEmpty()
            )[songId]
        }

    suspend fun setLyricsUri(
        songId: Long,
        uri: String?
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            val map =
                decodeLyricsUris(
                    prefs[
                        Keys.lyricsUris
                    ].orEmpty()
                ).toMutableMap()

            if (uri == null) {
                map.remove(songId)
            } else {
                map[songId] = uri
            }

            prefs[
                Keys.lyricsUris
            ] =
                map.entries
                    .joinToString("\n") {
                        "${it.key}\t${it.value}"
                    }
        }
    }

    private fun decodeLyricsUris(
        raw: String
    ): Map<Long, String> {
        if (raw.isBlank()) {
            return emptyMap()
        }

        return buildMap {
            raw.lineSequence()
                .forEach {
                    line ->

                    val separator =
                        line.indexOf('\t')

                    if (
                        separator <= 0 ||
                        separator >=
                        line.lastIndex
                    ) {
                        return@forEach
                    }

                    val id =
                        line.substring(
                            0,
                            separator
                        ).toLongOrNull()
                            ?: return@forEach

                    val uri =
                        line.substring(
                            separator + 1
                        )

                    if (uri.isNotBlank()) {
                        put(id, uri)
                    }
                }
        }
    }
}
