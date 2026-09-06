package com.xvox.music.data.preferences

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal val Context.xvoxDataStore by preferencesDataStore(name = "xvox_preferences")

class UserPreferencesRepository(
    private val context: Context
) {
    private object Keys {
        val setupCompleted = booleanPreferencesKey("setup_completed")
        val username = stringPreferencesKey("username")
        val selectedPfp = stringPreferencesKey("selected_pfp")
        val customPfpUri = stringPreferencesKey("custom_pfp_uri")
        val recentSongIds = stringPreferencesKey("recent_song_ids")
        val lyricsUris = stringPreferencesKey("lyrics_uris")
        val lastPlayedSongId = longPreferencesKey("last_played_song_id")
        val recentSearches = stringPreferencesKey("recent_searches")

        val crossfade = booleanPreferencesKey("crossfade")
        val crossfadeDuration = intPreferencesKey("crossfade_duration")
        val pauseOnHeadphoneDisconnect = booleanPreferencesKey("pause_on_headphone_disconnect")
        val playOnHeadsetConnect = booleanPreferencesKey("play_on_headset_connect")

        val equalizerEnabled = booleanPreferencesKey("equalizer_enabled")
        val eqPreset = stringPreferencesKey("eq_preset")
        val eqBands = stringPreferencesKey("eq_bands")
        val balance = floatPreferencesKey("balance_l_r")
        val stereoWidening = booleanPreferencesKey("stereo_widening")
        val surroundPanSpeed = intPreferencesKey("surround_pan_speed")

        val appVolume = floatPreferencesKey("app_volume")
        val volumeLimit = floatPreferencesKey("volume_limit")

        val theme = stringPreferencesKey("theme")
        val accentColor = stringPreferencesKey("accent_color")
        val fontSizeScale = floatPreferencesKey("font_size_scale")
        val fourRowsGrid = booleanPreferencesKey("four_rows_grid")

        val homeLayoutStyle = stringPreferencesKey("home_layout_style")
        val homeScrollDirection = stringPreferencesKey("home_scroll_direction")
        val homeHorizontalRows = intPreferencesKey("home_horizontal_rows")
        val hideRecentlyPlayed = booleanPreferencesKey("hide_recently_played")

        val widgetTransparency = floatPreferencesKey("widget_transparency")
        val widgetTheme = stringPreferencesKey("widget_theme")
        val widgetCustomColor = stringPreferencesKey("widget_custom_color")
        val widgetShowLogo = booleanPreferencesKey("widget_show_logo")
        val widgetCornerRadius = intPreferencesKey("widget_corner_radius")
    }

    val preferences: Flow<UserPreferences> = context.xvoxDataStore.data.map { prefs ->
        UserPreferences(
            setupCompleted = prefs[Keys.setupCompleted] ?: false,
            username = prefs[Keys.username].orEmpty(),
            selectedPfp = prefs[Keys.selectedPfp] ?: "DEFAULT",
            customPfpUri = prefs[Keys.customPfpUri]
        )
    }

    val recentSongIds: Flow<List<Long>> = context.xvoxDataStore.data.map { prefs ->
        decodeRecentIds(prefs[Keys.recentSongIds].orEmpty())
    }

    val lastPlayedSongId: Flow<Long?> = context.xvoxDataStore.data.map { prefs ->
        prefs[Keys.lastPlayedSongId]
    }

    val recentSearches: Flow<List<String>> = context.xvoxDataStore.data.map { prefs ->
        decodeRecentSearches(prefs[Keys.recentSearches].orEmpty())
    }

    val crossfade: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.crossfade] ?: false }
    val crossfadeDuration: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.crossfadeDuration] ?: 3 }
    val pauseOnHeadphoneDisconnect: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.pauseOnHeadphoneDisconnect] ?: true }
    val playOnHeadsetConnect: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.playOnHeadsetConnect] ?: false }

    val equalizerEnabled: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.equalizerEnabled] ?: false }
    val eqPreset: Flow<String> = context.xvoxDataStore.data.map { it[Keys.eqPreset] ?: "Flat" }
    val eqBands: Flow<List<Int>> = context.xvoxDataStore.data.map { decodeBands(it[Keys.eqBands].orEmpty()) }
    val balance: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.balance] ?: 0f }
    val stereoWidening: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.stereoWidening] ?: false }
    val surroundPanSpeed: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.surroundPanSpeed] ?: 6 }

    val appVolume: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.appVolume] ?: 1.0f }
    val volumeLimit: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.volumeLimit] ?: 1.0f }

    val theme: Flow<String> = context.xvoxDataStore.data.map { it[Keys.theme] ?: "System" }
    val accentColor: Flow<String> = context.xvoxDataStore.data.map { it[Keys.accentColor] ?: "Default" }
    val fontSizeScale: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.fontSizeScale] ?: 1.0f }
    val fourRowsGrid: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.fourRowsGrid] ?: true }

    val homeLayoutStyle: Flow<String> = context.xvoxDataStore.data.map { it[Keys.homeLayoutStyle] ?: "mosaic" }
    val homeScrollDirection: Flow<String> = context.xvoxDataStore.data.map { it[Keys.homeScrollDirection] ?: "horizontal" }
    val homeHorizontalRows: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.homeHorizontalRows] ?: 4 }
    val hideRecentlyPlayed: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.hideRecentlyPlayed] ?: false }

    val widgetTransparency: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.widgetTransparency] ?: 0.25f }
    val widgetTheme: Flow<String> = context.xvoxDataStore.data.map { it[Keys.widgetTheme] ?: "Dark" }
    val widgetCustomColor: Flow<String> = context.xvoxDataStore.data.map { it[Keys.widgetCustomColor] ?: "#000000" }
    val widgetShowLogo: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.widgetShowLogo] ?: true }
    val widgetCornerRadius: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.widgetCornerRadius] ?: 16 }

    suspend fun setCrossfade(v: Boolean) { context.xvoxDataStore.edit { it[Keys.crossfade] = v } }
    suspend fun setCrossfadeDuration(v: Int) { context.xvoxDataStore.edit { it[Keys.crossfadeDuration] = v } }
    suspend fun setPauseOnHeadphoneDisconnect(v: Boolean) { context.xvoxDataStore.edit { it[Keys.pauseOnHeadphoneDisconnect] = v } }
    suspend fun setPlayOnHeadsetConnect(v: Boolean) { context.xvoxDataStore.edit { it[Keys.playOnHeadsetConnect] = v } }

    suspend fun setEqualizerEnabled(v: Boolean) { context.xvoxDataStore.edit { it[Keys.equalizerEnabled] = v } }
    suspend fun setEqPreset(preset: String) { context.xvoxDataStore.edit { it[Keys.eqPreset] = preset } }
    suspend fun setEqBands(bands: List<Int>) { context.xvoxDataStore.edit { it[Keys.eqBands] = bands.joinToString(",") } }
    suspend fun setBalance(v: Float) { context.xvoxDataStore.edit { it[Keys.balance] = v } }
    suspend fun setStereoWidening(v: Boolean) { context.xvoxDataStore.edit { it[Keys.stereoWidening] = v } }
    suspend fun setSurroundPanSpeed(v: Int) { context.xvoxDataStore.edit { it[Keys.surroundPanSpeed] = v.coerceIn(2, 10) } }

    suspend fun setAppVolume(v: Float) { context.xvoxDataStore.edit { it[Keys.appVolume] = v } }
    suspend fun setVolumeLimit(v: Float) { context.xvoxDataStore.edit { it[Keys.volumeLimit] = v } }

    suspend fun setTheme(v: String) { context.xvoxDataStore.edit { it[Keys.theme] = v } }
    suspend fun setAccentColor(v: String) { context.xvoxDataStore.edit { it[Keys.accentColor] = v } }
    suspend fun setFontSizeScale(v: Float) { context.xvoxDataStore.edit { it[Keys.fontSizeScale] = v } }
    suspend fun setFourRowsGrid(v: Boolean) { context.xvoxDataStore.edit { it[Keys.fourRowsGrid] = v } }

    suspend fun setHomeLayoutStyle(style: String) { context.xvoxDataStore.edit { it[Keys.homeLayoutStyle] = style } }
    suspend fun setHomeScrollDirection(direction: String) { context.xvoxDataStore.edit { it[Keys.homeScrollDirection] = direction } }
    suspend fun setHomeHorizontalRows(rows: Int) { context.xvoxDataStore.edit { it[Keys.homeHorizontalRows] = rows.coerceIn(3, 8) } }
    suspend fun setHideRecentlyPlayed(hide: Boolean) { context.xvoxDataStore.edit { it[Keys.hideRecentlyPlayed] = hide } }

    suspend fun setWidgetTransparency(v: Float) { context.xvoxDataStore.edit { it[Keys.widgetTransparency] = v } }
    suspend fun setWidgetTheme(v: String) { context.xvoxDataStore.edit { it[Keys.widgetTheme] = v } }
    suspend fun setWidgetCustomColor(v: String) { context.xvoxDataStore.edit { it[Keys.widgetCustomColor] = v } }
    suspend fun setWidgetShowLogo(v: Boolean) { context.xvoxDataStore.edit { it[Keys.widgetShowLogo] = v } }
    suspend fun setWidgetCornerRadius(v: Int) { context.xvoxDataStore.edit { it[Keys.widgetCornerRadius] = v } }

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

    private fun decodeRecentSearches(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    private fun decodeBands(raw: String): List<Int> {
        if (raw.isBlank()) return listOf(0, 0, 0, 0, 0)
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(0, 0, 0, 0, 0) }
    }

    suspend fun setLastPlayedSongId(songId: Long?) {
        context.xvoxDataStore.edit { prefs ->
            if (songId == null) {
                prefs.remove(Keys.lastPlayedSongId)
            } else {
                prefs[Keys.lastPlayedSongId] = songId
            }
        }
    }

    suspend fun completeSetup(username: String, selectedPfp: String, customPfpUri: String?) {
        val persistedPfp = persistProfileImage(customPfpUri)
        context.xvoxDataStore.edit { prefs ->
            prefs[Keys.username] = username.trim()
            prefs[Keys.selectedPfp] = selectedPfp
            if (persistedPfp != null) {
                prefs[Keys.customPfpUri] = persistedPfp
            } else {
                prefs.remove(Keys.customPfpUri)
            }
            prefs[Keys.setupCompleted] = true
        }
        cleanupProfileImages(persistedPfp)
    }

    suspend fun saveProfile(username: String, selectedPfp: String, customPfpUri: String?) {
        val cleanName = username.trim()
        if (cleanName.isEmpty()) return
        val persistedPfp = persistProfileImage(customPfpUri)
        context.xvoxDataStore.edit { prefs ->
            prefs[Keys.username] = cleanName
            prefs[Keys.selectedPfp] = selectedPfp
            if (persistedPfp != null) {
                prefs[Keys.customPfpUri] = persistedPfp
            } else {
                prefs.remove(Keys.customPfpUri)
            }
        }
        cleanupProfileImages(persistedPfp)
    }

    suspend fun recordRecentSong(songId: Long) {
        context.xvoxDataStore.edit { prefs ->
            val current = decodeRecentIds(prefs[Keys.recentSongIds].orEmpty())
            prefs[Keys.recentSongIds] = buildList {
                add(songId)
                addAll(current.filterNot { it == songId })
            }.take(20).joinToString(",")
        }
    }

    suspend fun removeRecentSong(songId: Long) {
        context.xvoxDataStore.edit { prefs ->
            val updated = decodeRecentIds(prefs[Keys.recentSongIds].orEmpty()).filterNot { it == songId }
            if (updated.isEmpty()) {
                prefs.remove(Keys.recentSongIds)
            } else {
                prefs[Keys.recentSongIds] = updated.joinToString(",")
            }
        }
    }

    fun lyricsUri(songId: Long): Flow<String?> = context.xvoxDataStore.data.map { prefs ->
        decodeLyricsUris(prefs[Keys.lyricsUris].orEmpty())[songId]
    }

    suspend fun setLyricsUri(songId: Long, uri: String?) {
        context.xvoxDataStore.edit { prefs ->
            val map = decodeLyricsUris(prefs[Keys.lyricsUris].orEmpty()).toMutableMap()
            if (uri == null) {
                map.remove(songId)
            } else {
                map[songId] = uri
            }
            prefs[Keys.lyricsUris] = map.entries.joinToString("\n") { "${it.key}\t${it.value}" }
        }
    }

    private fun decodeRecentIds(raw: String): List<Long> =
        raw.split(",").mapNotNull { it.trim().toLongOrNull() }.distinct().take(50)

    private suspend fun persistProfileImage(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return@withContext value
            val profileDirectory = File(context.filesDir, "profile")
            val existing = if (uri.scheme == "file") uri.path?.let(::File) else null

            if (existing != null && existing.exists() && runCatching {
                existing.parentFile?.canonicalPath == profileDirectory.canonicalPath
            }.getOrDefault(false)) {
                return@withContext value
            }

            runCatching {
                profileDirectory.mkdirs()
                val target = File(profileDirectory, "pfp_${System.nanoTime()}.img")
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false

                if (!copied) {
                    target.delete()
                    value
                } else {
                    Uri.fromFile(target).toString()
                }
            }.getOrDefault(value)
        }
    }

    private suspend fun cleanupProfileImages(retainedUri: String?) {
        withContext(Dispatchers.IO) {
            val retained = retainedUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            if (retained?.scheme != "file") return@withContext
            val retainedPath = retained.path ?: return@withContext
            File(context.filesDir, "profile").listFiles()?.forEach { file ->
                if (file.path != retainedPath) {
                    file.delete()
                }
            }
        }
    }

    private fun decodeLyricsUris(raw: String): Map<Long, String> {
        if (raw.isBlank()) return emptyMap()
        return buildMap {
            raw.lineSequence().forEach { line ->
                val separator = line.indexOf('\t')
                if (separator <= 0 || separator >= line.lastIndex) return@forEach
                val id = line.substring(0, separator).toLongOrNull() ?: return@forEach
                val uri = line.substring(separator + 1)
                if (uri.isNotBlank()) {
                    put(id, uri)
                }
            }
        }
    }
}
