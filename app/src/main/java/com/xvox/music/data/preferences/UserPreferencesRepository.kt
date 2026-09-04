package com.xvox.music.data.preferences

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

    val preferences:
        Flow<UserPreferences> =
        context.xvoxDataStore.data
            .map { prefs ->
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

    val recentSongIds:
        Flow<List<Long>> =
        context.xvoxDataStore.data
            .map { prefs ->
                decodeRecentIds(
                    prefs[
                        Keys.recentSongIds
                    ].orEmpty()
                )
            }

    val lastPlayedSongId:
        Flow<Long?> =
        context.xvoxDataStore.data
            .map { prefs ->
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
        val persistedPfp =
            persistProfileImage(
                customPfpUri
            )

        context.xvoxDataStore.edit {
            prefs ->

            prefs[Keys.username] =
                username.trim()

            prefs[Keys.selectedPfp] =
                selectedPfp

            if (persistedPfp != null) {
                prefs[
                    Keys.customPfpUri
                ] = persistedPfp
            } else {
                prefs.remove(
                    Keys.customPfpUri
                )
            }

            prefs[
                Keys.setupCompleted
            ] = true
        }

        cleanupProfileImages(
            persistedPfp
        )
    }

    suspend fun saveProfile(
        username: String,
        selectedPfp: String,
        customPfpUri: String?
    ) {
        val cleanName =
            username.trim()

        if (cleanName.isEmpty()) {
            return
        }

        val persistedPfp =
            persistProfileImage(
                customPfpUri
            )

        context.xvoxDataStore.edit {
            prefs ->

            prefs[Keys.username] =
                cleanName

            prefs[Keys.selectedPfp] =
                selectedPfp

            if (persistedPfp != null) {
                prefs[
                    Keys.customPfpUri
                ] = persistedPfp
            } else {
                prefs.remove(
                    Keys.customPfpUri
                )
            }
        }

        cleanupProfileImages(
            persistedPfp
        )
    }

    suspend fun recordRecentSong(
        songId: Long
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            val current =
                decodeRecentIds(
                    prefs[
                        Keys.recentSongIds
                    ].orEmpty()
                )

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

    suspend fun removeRecentSong(
        songId: Long
    ) {
        context.xvoxDataStore.edit {
            prefs ->

            val updated =
                decodeRecentIds(
                    prefs[
                        Keys.recentSongIds
                    ].orEmpty()
                )
                    .filterNot {
                        it == songId
                    }

            if (updated.isEmpty()) {
                prefs.remove(
                    Keys.recentSongIds
                )
            } else {
                prefs[
                    Keys.recentSongIds
                ] =
                    updated.joinToString(
                        ","
                    )
            }
        }
    }

    fun lyricsUri(
        songId: Long
    ): Flow<String?> =
        context.xvoxDataStore.data
            .map { prefs ->
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
                )
                    .toMutableMap()

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

    private fun decodeRecentIds(
        raw: String
    ): List<Long> =
        raw.split(",")
            .mapNotNull {
                it.toLongOrNull()
            }
            .distinct()
            .take(20)

    private suspend fun persistProfileImage(
        value: String?
    ): String? {
        if (value.isNullOrBlank()) {
            return null
        }

        return withContext(
            Dispatchers.IO
        ) {
            val uri =
                runCatching {
                    Uri.parse(value)
                }.getOrNull()
                    ?: return@withContext value

            val profileDirectory =
                File(
                    context.filesDir,
                    "profile"
                )

            val existing =
                if (
                    uri.scheme == "file"
                ) {
                    uri.path?.let(::File)
                } else {
                    null
                }

            if (
                existing != null &&
                existing.exists() &&
                runCatching {
                    existing.parentFile
                        ?.canonicalPath ==
                        profileDirectory
                            .canonicalPath
                }.getOrDefault(false)
            ) {
                return@withContext value
            }

            runCatching {
                profileDirectory.mkdirs()

                val target =
                    File(
                        profileDirectory,
                        "pfp_${System.nanoTime()}.img"
                    )

                val copied =
                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { input ->
                            target.outputStream()
                                .use { output ->
                                    input.copyTo(
                                        output
                                    )
                                }

                            true
                        } ?: false

                if (!copied) {
                    target.delete()
                    value
                } else {
                    Uri.fromFile(target)
                        .toString()
                }
            }.getOrDefault(value)
        }
    }

    private suspend fun cleanupProfileImages(
        retainedUri: String?
    ) {
        withContext(
            Dispatchers.IO
        ) {
            val retained =
                retainedUri
                    ?.let {
                        runCatching {
                            Uri.parse(it)
                        }.getOrNull()
                    }

            if (
                retained?.scheme !=
                "file"
            ) {
                return@withContext
            }

            val retainedPath =
                retained.path
                    ?: return@withContext

            File(
                context.filesDir,
                "profile"
            )
                .listFiles()
                ?.forEach { file ->
                    if (
                        file.path !=
                        retainedPath
                    ) {
                        file.delete()
                    }
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
                .forEach { line ->
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
                        )
                            .toLongOrNull()
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
