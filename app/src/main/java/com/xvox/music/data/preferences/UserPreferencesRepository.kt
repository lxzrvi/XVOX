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

        val recentSearches =
            stringPreferencesKey(
                "recent_searches"
            )

        // Playback settings 1-12
        val gaplessPlayback = booleanPreferencesKey("gapless_playback")
        val crossfade = booleanPreferencesKey("crossfade")
        val crossfadeDuration = longPreferencesKey("crossfade_duration")
        val fadeIn = booleanPreferencesKey("fade_in")
        val fadeOut = booleanPreferencesKey("fade_out")
        val replayGain = booleanPreferencesKey("replay_gain")
        val loudnessNormalization = booleanPreferencesKey("loudness_normalization")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val pitchControl = booleanPreferencesKey("pitch_control")
        val audioFocus = booleanPreferencesKey("audio_focus")
        val pauseOnHeadphoneDisconnect = booleanPreferencesKey("pause_on_headphone_disconnect")
        val clearQueueAfterPlayback = booleanPreferencesKey("clear_queue_after_playback")
        val rememberQueue = booleanPreferencesKey("remember_queue")

        // Equalizer / Audio 13-26
        val equalizerEnabled = booleanPreferencesKey("equalizer_enabled")
        val eqPreset = stringPreferencesKey("eq_preset")
        val eqBands = stringPreferencesKey("eq_bands")
        val eqPreamp = stringPreferencesKey("eq_preamp")
        val bassBoost = booleanPreferencesKey("bass_boost")
        val virtualizerEnabled = booleanPreferencesKey("virtualizer_enabled")
        val loudnessEnhancer = booleanPreferencesKey("loudness_enhancer")
        val compressorEnabled = booleanPreferencesKey("compressor_enabled")
        val limiterEnabled = booleanPreferencesKey("limiter_enabled")
        val balance = stringPreferencesKey("balance_l_r")
        val monoAudio = booleanPreferencesKey("mono_audio")
        val stereoWidening = booleanPreferencesKey("stereo_widening")
        val volumeNormalization = booleanPreferencesKey("volume_normalization")

        // Headphones / Bluetooth 27-28
        val playOnHeadsetConnect = booleanPreferencesKey("play_on_headset_connect")
        // pauseOnHeadphoneDisconnect already above

        // Notification 29
        val mediaNotification = booleanPreferencesKey("media_notification")

        // Volume 30-32
        val appVolume = stringPreferencesKey("app_volume")
        val rememberVolume = booleanPreferencesKey("remember_volume")
        val volumeLimit = stringPreferencesKey("volume_limit")

        // Output is dynamic via AudioManager, no pref needed but selection persisted
        val selectedOutput = stringPreferencesKey("selected_output")

        // Customizations 37-43
        val theme = stringPreferencesKey("theme")
        val accentColor = stringPreferencesKey("accent_color")
        val miniPlayerLayout = stringPreferencesKey("mini_player_layout")
        val fullPlayerLayout = stringPreferencesKey("full_player_layout")
        val homeLayout = stringPreferencesKey("home_layout")
        val fontSizeScale = stringPreferencesKey("font_size_scale")
        val hapticFeedback = booleanPreferencesKey("haptic_feedback")

        // Widgets 44
        val enabledWidgets = stringPreferencesKey("enabled_widgets")
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

    val recentSearches: Flow<List<String>> =
        context.xvoxDataStore.data.map { prefs -> decodeRecentSearches(prefs[Keys.recentSearches].orEmpty()) }

    // Playback flows
    val gaplessPlayback: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.gaplessPlayback] ?: true }
    val crossfade: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.crossfade] ?: false }
    val fadeIn: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.fadeIn] ?: false }
    val fadeOut: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.fadeOut] ?: false }
    val replayGain: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.replayGain] ?: false }
    val loudnessNormalization: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.loudnessNormalization] ?: false }
    val skipSilence: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.skipSilence] ?: false }
    val pitchControl: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.pitchControl] ?: false }
    val audioFocus: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.audioFocus] ?: true }
    val pauseOnHeadphoneDisconnect: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.pauseOnHeadphoneDisconnect] ?: true }
    val clearQueueAfterPlayback: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.clearQueueAfterPlayback] ?: false }
    val rememberQueue: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.rememberQueue] ?: true }
    val playOnHeadsetConnect: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.playOnHeadsetConnect] ?: false }

    // Audio
    val equalizerEnabled: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.equalizerEnabled] ?: false }
    val bassBoost: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.bassBoost] ?: false }
    val virtualizerEnabled: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.virtualizerEnabled] ?: false }
    val monoAudio: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.monoAudio] ?: false }
    val stereoWidening: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.stereoWidening] ?: false }
    val mediaNotification: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.mediaNotification] ?: true }
    val rememberVolume: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.rememberVolume] ?: true }
    val theme: Flow<String> = context.xvoxDataStore.data.map { it[Keys.theme] ?: "System" }
    val hapticFeedback: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.hapticFeedback] ?: true }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        context.xvoxDataStore.edit { prefs ->
            val current = decodeRecentSearches(prefs[Keys.recentSearches].orEmpty())
            val updated = buildList {
                add(clean)
                addAll(current.filterNot { it.equals(clean, ignoreCase = true) })
            }.take(10)
            prefs[Keys.recentSearches] = updated.joinToString("\n")
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.xvoxDataStore.edit { prefs ->
            val updated = decodeRecentSearches(prefs[Keys.recentSearches].orEmpty()).filterNot { it == query }
            if (updated.isEmpty()) prefs.remove(Keys.recentSearches) else prefs[Keys.recentSearches] = updated.joinToString("\n")
        }
    }

    suspend fun clearRecentSearches() {
        context.xvoxDataStore.edit { it.remove(Keys.recentSearches) }
    }

    suspend fun setGaplessPlayback(v: Boolean) { context.xvoxDataStore.edit { it[Keys.gaplessPlayback] = v } }
    suspend fun setCrossfade(v: Boolean) { context.xvoxDataStore.edit { it[Keys.crossfade] = v } }
    suspend fun setFadeIn(v: Boolean) { context.xvoxDataStore.edit { it[Keys.fadeIn] = v } }
    suspend fun setFadeOut(v: Boolean) { context.xvoxDataStore.edit { it[Keys.fadeOut] = v } }
    suspend fun setReplayGain(v: Boolean) { context.xvoxDataStore.edit { it[Keys.replayGain] = v } }
    suspend fun setLoudnessNormalization(v: Boolean) { context.xvoxDataStore.edit { it[Keys.loudnessNormalization] = v } }
    suspend fun setSkipSilence(v: Boolean) { context.xvoxDataStore.edit { it[Keys.skipSilence] = v } }
    suspend fun setAudioFocus(v: Boolean) { context.xvoxDataStore.edit { it[Keys.audioFocus] = v } }
    suspend fun setPauseOnHeadphoneDisconnect(v: Boolean) { context.xvoxDataStore.edit { it[Keys.pauseOnHeadphoneDisconnect] = v } }
    suspend fun setClearQueueAfterPlayback(v: Boolean) { context.xvoxDataStore.edit { it[Keys.clearQueueAfterPlayback] = v } }
    suspend fun setRememberQueue(v: Boolean) { context.xvoxDataStore.edit { it[Keys.rememberQueue] = v } }
    suspend fun setPlayOnHeadsetConnect(v: Boolean) { context.xvoxDataStore.edit { it[Keys.playOnHeadsetConnect] = v } }
    suspend fun setEqualizerEnabled(v: Boolean) { context.xvoxDataStore.edit { it[Keys.equalizerEnabled] = v } }
    suspend fun setBassBoost(v: Boolean) { context.xvoxDataStore.edit { it[Keys.bassBoost] = v } }
    suspend fun setVirtualizer(v: Boolean) { context.xvoxDataStore.edit { it[Keys.virtualizerEnabled] = v } }
    suspend fun setMonoAudio(v: Boolean) { context.xvoxDataStore.edit { it[Keys.monoAudio] = v } }
    suspend fun setStereoWidening(v: Boolean) { context.xvoxDataStore.edit { it[Keys.stereoWidening] = v } }
    suspend fun setMediaNotification(v: Boolean) { context.xvoxDataStore.edit { it[Keys.mediaNotification] = v } }
    suspend fun setTheme(v: String) { context.xvoxDataStore.edit { it[Keys.theme] = v } }
    suspend fun setHapticFeedback(v: Boolean) { context.xvoxDataStore.edit { it[Keys.hapticFeedback] = v } }

    private fun decodeRecentSearches(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
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
