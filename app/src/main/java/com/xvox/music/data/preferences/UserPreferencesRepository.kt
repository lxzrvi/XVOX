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
import kotlinx.coroutines.flow.distinctUntilChanged
import com.xvox.music.features.home.HomePresentation
import com.xvox.music.features.home.HomeSections
import com.xvox.music.features.home.normalizeHomeStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val customPfpUris = stringPreferencesKey("custom_pfp_uris")
        val customCoverUris = stringPreferencesKey("custom_cover_uris")
        val recentSongIds = stringPreferencesKey("recent_song_ids")
        val recentSongSources = stringPreferencesKey("recent_song_sources")
        val lyricsUris = stringPreferencesKey("lyrics_uris")
        val lastPlayedSongId = longPreferencesKey("last_played_song_id")
        val recentSearches = stringPreferencesKey("recent_searches")

        val lyricsSettings = stringPreferencesKey("lyrics_settings_v1")
        val splitShowPill = booleanPreferencesKey("split_show_pill")
        val splitHideCollection = booleanPreferencesKey("split_hide_collection")
        val playlistStyle = stringPreferencesKey("home_playlist_style")
        val playlistLongHeight = intPreferencesKey("home_playlist_long_height")
        val homeMerge = booleanPreferencesKey("home_merge")
        val homeSectionOrder = stringPreferencesKey("home_section_order")
        val homeHiddenSections = stringPreferencesKey("home_hidden_sections")
        val crossfadeSmart = booleanPreferencesKey("crossfade_smart")
        val crossfadeClashControl = floatPreferencesKey("crossfade_clash_control")
        val crossfadeBeatSync = booleanPreferencesKey("crossfade_beat_sync")
        val widgetCustomization = stringPreferencesKey("widget_customization_v1")
        val widgetPaddingX = intPreferencesKey("widget_padding_x")
        val widgetPaddingY = intPreferencesKey("widget_padding_y")
        val crossfade = booleanPreferencesKey("crossfade")
        val crossfadeDuration = intPreferencesKey("crossfade_duration")
        val pauseOnHeadphoneDisconnect = booleanPreferencesKey("pause_on_headphone_disconnect")
        val playOnHeadsetConnect = booleanPreferencesKey("play_on_headset_connect")
        val btDisconnectAction = stringPreferencesKey("bt_disconnect_action")
        val btConnectAction = stringPreferencesKey("bt_connect_action")

        val equalizerEnabled = booleanPreferencesKey("equalizer_enabled")
        val eqPreset = stringPreferencesKey("eq_preset")
        val eqBandCount = intPreferencesKey("eq_band_count")
        val noiseReduction = floatPreferencesKey("noise_reduction")
        val softenHighs = floatPreferencesKey("soften_highs")
        val eqBands = stringPreferencesKey("eq_bands")
        val balance = floatPreferencesKey("balance_l_r")
        val stereoWidening = booleanPreferencesKey("stereo_widening")
        val surroundPanSpeed = intPreferencesKey("surround_pan_speed")

        val appVolume = floatPreferencesKey("app_volume")
        val volumeLimit = floatPreferencesKey("volume_limit")

        val surroundWidth = floatPreferencesKey("surround_width")
        val surroundPosition = floatPreferencesKey("surround_position")
        val roomAmount = floatPreferencesKey("room_amount")
        val reverbAmount = floatPreferencesKey("reverb_amount")
        val hrtf = floatPreferencesKey("hrtf")
        val centerPreservation = floatPreferencesKey("center_preservation")

        val theme = stringPreferencesKey("theme")
        val accentColor = stringPreferencesKey("accent_color")
        val themeBackground = stringPreferencesKey("theme_background")
        val themeBackgroundImage = stringPreferencesKey("theme_background_image")
        val cardTransparency = floatPreferencesKey("card_transparency")
        val fontSizeScale = floatPreferencesKey("font_size_scale")
        val fourRowsGrid = booleanPreferencesKey("four_rows_grid")

        val homeLayoutStyle = stringPreferencesKey("home_layout_style")
        val homeScrollDirection = stringPreferencesKey("home_scroll_direction")
        val homeHorizontalRows = intPreferencesKey("home_horizontal_rows")
        val recentsPlacement = stringPreferencesKey("recents_placement")
        val eqHeadroomDb = floatPreferencesKey("eq_headroom_db")
        val surroundDepth = floatPreferencesKey("surround_depth")
        val hideRecentlyPlayed = booleanPreferencesKey("hide_recently_played")
        val sortOrder = stringPreferencesKey("sort_order")

        val ignoreBelowSec = intPreferencesKey("ignore_below_sec")
        val ignoreBelowKb = intPreferencesKey("ignore_below_kb")
        val ignoredFolders = stringPreferencesKey("ignored_folders")

        val widgetTransparency = floatPreferencesKey("widget_transparency")
        val widgetTheme = stringPreferencesKey("widget_theme")
        val widgetCustomColor = stringPreferencesKey("widget_custom_color")
        val widgetShowLogo = booleanPreferencesKey("widget_show_logo")
        val widgetCornerRadius = intPreferencesKey("widget_corner_radius")

        // Revision 7: per-surface chrome (header / mini player / nav bar / pills / cards / boxes).
        val chromeStyle = stringPreferencesKey("chrome_style_v1")
        val backgroundBrightness = floatPreferencesKey("background_brightness")
        val audioOutputRoute = stringPreferencesKey("audio_output_route")
        val profileLines = stringPreferencesKey("profile_lines")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val remindersLastAt = longPreferencesKey("reminders_last_at")
        val remindersDayCount = intPreferencesKey("reminders_day_count")
    }

    val preferences: Flow<UserPreferences> = context.xvoxDataStore.data.map { prefs ->
        UserPreferences(
            setupCompleted = prefs[Keys.setupCompleted] ?: false,
            username = prefs[Keys.username].orEmpty(),
            selectedPfp = prefs[Keys.selectedPfp] ?: "DEFAULT",
            customPfpUri = prefs[Keys.customPfpUri],
            customPfpUris = decodeUriList(prefs[Keys.customPfpUris].orEmpty()),
            profileLines = prefs[Keys.profileLines].orEmpty().lines()
                .map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(4)
        )
    }.distinctUntilChanged()

    /** Every custom profile picture the user has kept; they stack next to the built-in ones. */
    val customPfpUris: Flow<List<String>> = context.xvoxDataStore.data
        .map { decodeUriList(it[Keys.customPfpUris].orEmpty()) }.distinctUntilChanged()

    /** Every custom playlist cover the user has kept, reusable across playlists. */
    val customCoverUris: Flow<List<String>> = context.xvoxDataStore.data
        .map { decodeUriList(it[Keys.customCoverUris].orEmpty()) }.distinctUntilChanged()

    val recentSongIds: Flow<List<Long>> = context.xvoxDataStore.data.map { prefs ->
        decodeRecentIds(prefs[Keys.recentSongIds].orEmpty())
    }.distinctUntilChanged()

    /** Where each recent song was started from: Liked, a playlist name, All Songs, XvoxSplit… */
    val recentSongSources: Flow<Map<Long, String>> = context.xvoxDataStore.data.map { prefs ->
        decodeRecentSources(prefs[Keys.recentSongSources].orEmpty())
    }.distinctUntilChanged()

    val lastPlayedSongId: Flow<Long?> = context.xvoxDataStore.data.map { prefs ->
        prefs[Keys.lastPlayedSongId]
    }.distinctUntilChanged()

    val recentSearches: Flow<List<String>> = context.xvoxDataStore.data.map { prefs ->
        decodeRecentSearches(prefs[Keys.recentSearches].orEmpty())
    }.distinctUntilChanged()

    val lyricsSettings: Flow<LyricsSettings> = context.xvoxDataStore.data.map { it[Keys.lyricsSettings].orEmpty() }.distinctUntilChanged().map { LyricsSettings.decode(it) }
    val splitShowPill: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.splitShowPill] ?: true }.distinctUntilChanged()
    val splitHideCollection: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.splitHideCollection] ?: false }.distinctUntilChanged()
    val playlistStyle: Flow<String> = context.xvoxDataStore.data.map { if (it[Keys.playlistStyle] == "cards") "cards" else "long" }.distinctUntilChanged()
    /** 0 = original proportional height; otherwise an explicit dp height for long playlist cards. */
    val playlistLongHeight: Flow<Int> = context.xvoxDataStore.data.map { (it[Keys.playlistLongHeight] ?: 0).coerceIn(0, 260) }.distinctUntilChanged()
    val homeMerge: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.homeMerge] ?: false }.distinctUntilChanged()
    val homeSectionOrder: Flow<List<String>> = context.xvoxDataStore.data.map {
        val raw = it[Keys.homeSectionOrder]
        if (raw.isNullOrBlank()) HomeSections.placeRecent(HomeSections.defaultOrder, it[Keys.recentsPlacement] ?: "bottom")
        else HomeSections.normalize(raw.split(","))
    }.distinctUntilChanged()
    val homeHiddenSections: Flow<Set<String>> = context.xvoxDataStore.data.map {
        it[Keys.homeHiddenSections].orEmpty().split(",").filter { id -> id in HomeSections.defaultOrder }.toSet()
    }.distinctUntilChanged()
    val homePresentation: Flow<HomePresentation> = context.xvoxDataStore.data.map {
        val placement = it[Keys.recentsPlacement] ?: "bottom"
        HomePresentation(
            style = normalizeHomeStyle(it[Keys.homeLayoutStyle]), direction = it[Keys.homeScrollDirection] ?: "horizontal",
            rows = (it[Keys.homeHorizontalRows] ?: 4).coerceIn(3, 8), hideRecents = it[Keys.hideRecentlyPlayed] ?: false,
            recentsPlacement = placement, merge = it[Keys.homeMerge] ?: false,
            order = it[Keys.homeSectionOrder]?.let { raw -> HomeSections.normalize(raw.split(",")) }
                ?: HomeSections.placeRecent(HomeSections.defaultOrder, placement),
            playlistStyle = if (it[Keys.playlistStyle] == "cards") "cards" else "long", hideSplit = it[Keys.splitHideCollection] ?: false,
            playlistLongHeight = (it[Keys.playlistLongHeight] ?: 0).coerceIn(0, 260),
            hidden = it[Keys.homeHiddenSections].orEmpty().split(",").filter { id -> id in HomeSections.defaultOrder }.toSet()
        )
    }.distinctUntilChanged()
    val crossfadeSmart: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.crossfadeSmart] ?: true }.distinctUntilChanged()
    val crossfadeClashControl: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.crossfadeClashControl] ?: .7f).coerceIn(0f, 1f) }.distinctUntilChanged()
    val crossfadeBeatSync: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.crossfadeBeatSync] ?: true }.distinctUntilChanged()
    val widgetPaddingX: Flow<Int> = context.xvoxDataStore.data.map { (it[Keys.widgetPaddingX] ?: 10).coerceIn(0, 32) }.distinctUntilChanged()
    val widgetPaddingY: Flow<Int> = context.xvoxDataStore.data.map { (it[Keys.widgetPaddingY] ?: 8).coerceIn(0, 28) }.distinctUntilChanged()
    val crossfade: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.crossfade] ?: false }.distinctUntilChanged()
    val crossfadeDuration: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.crossfadeDuration] ?: 3 }.distinctUntilChanged()
    val pauseOnHeadphoneDisconnect: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.pauseOnHeadphoneDisconnect] ?: true }.distinctUntilChanged()
    val playOnHeadsetConnect: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.playOnHeadsetConnect] ?: false }.distinctUntilChanged()
    val btDisconnectAction: Flow<String> = context.xvoxDataStore.data.map { it[Keys.btDisconnectAction] ?: "pause" }.distinctUntilChanged()
    val btConnectAction: Flow<String> = context.xvoxDataStore.data.map { it[Keys.btConnectAction] ?: "none" }.distinctUntilChanged()

    val equalizerEnabled: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.equalizerEnabled] ?: false }.distinctUntilChanged()
    val eqPreset: Flow<String> = context.xvoxDataStore.data.map { it[Keys.eqPreset] ?: "Flat" }.distinctUntilChanged()
    val eqBandCount: Flow<Int> = context.xvoxDataStore.data.map { com.xvox.music.audio.EqBands.count(it[Keys.eqBandCount] ?: 5) }.distinctUntilChanged()
    val noiseReduction: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.noiseReduction] ?: 0f).coerceIn(0f, 1f) }.distinctUntilChanged()
    val softenHighs: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.softenHighs] ?: 0f).coerceIn(0f, 1f) }.distinctUntilChanged()
    val eqBands: Flow<List<Int>> = context.xvoxDataStore.data.map {
        com.xvox.music.audio.EqBands.convert(decodeBands(it[Keys.eqBands].orEmpty()), com.xvox.music.audio.EqBands.count(it[Keys.eqBandCount] ?: 5))
    }.distinctUntilChanged()
    val balance: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.balance] ?: 0f }.distinctUntilChanged()
    val stereoWidening: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.stereoWidening] ?: false }.distinctUntilChanged()
    val surroundPanSpeed: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.surroundPanSpeed] ?: 6 }.distinctUntilChanged()
    val surroundWidth: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.surroundWidth] ?: .78f).coerceIn(.05f, 1f) }.distinctUntilChanged()
    val surroundPosition: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.surroundPosition] ?: 0f).coerceIn(-1.5f, 1.5f) }.distinctUntilChanged()
    val roomAmount: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.roomAmount] ?: .5f).coerceIn(0f, 1f) }.distinctUntilChanged()
    val reverbAmount: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.reverbAmount] ?: 0f).coerceIn(0f, 1f) }.distinctUntilChanged()
    val hrtf: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.hrtf] ?: .6f).coerceIn(0f, 1f) }.distinctUntilChanged()
    val centerPreservation: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.centerPreservation] ?: 0f).coerceIn(0f, 1f) }.distinctUntilChanged()

    val appVolume: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.appVolume] ?: 1.0f }.distinctUntilChanged()
    val volumeLimit: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.volumeLimit] ?: 1.0f }.distinctUntilChanged()

    val theme: Flow<String> = context.xvoxDataStore.data.map { it[Keys.theme] ?: "System" }.distinctUntilChanged()
    val accentColor: Flow<String> = context.xvoxDataStore.data.map { it[Keys.accentColor] ?: "Red" }.distinctUntilChanged()
    val themeBackground: Flow<String> = context.xvoxDataStore.data.map { it[Keys.themeBackground] ?: "Default" }.distinctUntilChanged()
    val themeBackgroundImage: Flow<String> = context.xvoxDataStore.data.map { it[Keys.themeBackgroundImage].orEmpty() }.distinctUntilChanged()
    val cardTransparency: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.cardTransparency] ?: 0f).coerceIn(0f, 0.6f) }.distinctUntilChanged()
    val fontSizeScale: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.fontSizeScale] ?: 1.0f }.distinctUntilChanged()
    val fourRowsGrid: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.fourRowsGrid] ?: true }.distinctUntilChanged()

    val homeLayoutStyle: Flow<String> = context.xvoxDataStore.data.map { normalizeHomeStyle(it[Keys.homeLayoutStyle]) }.distinctUntilChanged()
    val homeScrollDirection: Flow<String> = context.xvoxDataStore.data.map { it[Keys.homeScrollDirection] ?: "horizontal" }.distinctUntilChanged()
    val homeHorizontalRows: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.homeHorizontalRows] ?: 4 }.distinctUntilChanged()
    val recentsPlacement: Flow<String> = context.xvoxDataStore.data.map { if (it[Keys.recentsPlacement] == "top") "top" else "bottom" }.distinctUntilChanged()
    val eqHeadroomDb: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.eqHeadroomDb] ?: 0f).coerceIn(0f, 18f) }.distinctUntilChanged()
    val surroundDepth: Flow<Float> = context.xvoxDataStore.data.map { (it[Keys.surroundDepth] ?: 0.65f).coerceIn(0f, 1f) }.distinctUntilChanged()

    // Read audio settings atomically, so a preset never passes through a half-updated state.
    val audioDspSettings: Flow<com.xvox.music.audio.AudioDspSettings> = context.xvoxDataStore.data.map {
        val preset = it[Keys.eqPreset] ?: "Flat"
        val count = com.xvox.music.audio.EqBands.count(it[Keys.eqBandCount] ?: 5)
        val bands = com.xvox.music.audio.EqBands.convert(com.xvox.music.audio.AudioEffectsManager.PRESETS[preset] ?: decodeBands(it[Keys.eqBands].orEmpty()), count)
        com.xvox.music.audio.AudioDspSettings(
            equalizerEnabled = it[Keys.equalizerEnabled] ?: false,
            bands = bands.map { it.toFloat() }, bandCount = count,
            noiseReduction = (it[Keys.noiseReduction] ?: 0f).coerceIn(0f, 1f), softenHighs = (it[Keys.softenHighs] ?: 0f).coerceIn(0f, 1f),
            headroomDb = (it[Keys.eqHeadroomDb] ?: 0f).coerceIn(0f, 18f),
            balance = (it[Keys.balance] ?: 0f).coerceIn(-1f, 1f),
            surroundEnabled = it[Keys.stereoWidening] ?: false,
            surroundDepth = (it[Keys.surroundDepth] ?: 0.65f).coerceIn(0f, 1f),
            orbitSeconds = (it[Keys.surroundPanSpeed] ?: 6).toFloat().coerceIn(2f, 10f),
            masterVolume = ((it[Keys.appVolume] ?: 1f) * (it[Keys.volumeLimit] ?: 1f)).coerceIn(0f, 1f),
            surroundWidth = (it[Keys.surroundWidth] ?: .78f).coerceIn(.05f, 1f),
            surroundPosition = (it[Keys.surroundPosition] ?: 0f).coerceIn(-1.5f, 1.5f),
            roomAmount = (it[Keys.roomAmount] ?: .5f).coerceIn(0f, 1f),
            reverbAmount = (it[Keys.reverbAmount] ?: 0f).coerceIn(0f, 1f),
            hrtf = (it[Keys.hrtf] ?: .6f).coerceIn(0f, 1f),
            centerPreservation = (it[Keys.centerPreservation] ?: 0f).coerceIn(0f, 1f)
        )
    }.distinctUntilChanged()

    val hideRecentlyPlayed: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.hideRecentlyPlayed] ?: false }.distinctUntilChanged()
    val sortOrder: Flow<String> = context.xvoxDataStore.data.map { it[Keys.sortOrder] ?: "A-Z" }.distinctUntilChanged()

    val ignoreBelowSec: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.ignoreBelowSec] ?: 0 }.distinctUntilChanged()
    val ignoreBelowKb: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.ignoreBelowKb] ?: 0 }.distinctUntilChanged()
    val ignoredFolders: Flow<Set<String>> = context.xvoxDataStore.data.map {
        it[Keys.ignoredFolders].orEmpty().split("\n").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }.toSet()
    }.distinctUntilChanged()

    val widgetCustomization: Flow<com.xvox.music.widget.WidgetCustomization> = context.xvoxDataStore.data.map { it[Keys.widgetCustomization].orEmpty() }
        .distinctUntilChanged().map { com.xvox.music.widget.WidgetCustomization.decode(it) }
    val widgetStyle: Flow<com.xvox.music.widget.WidgetStyle> = context.xvoxDataStore.data.map {
        com.xvox.music.widget.WidgetStyle(it[Keys.widgetTransparency] ?: .25f, it[Keys.widgetTheme] ?: "Dark",
            it[Keys.widgetCustomColor] ?: "#000000", it[Keys.widgetShowLogo] ?: true, it[Keys.widgetCornerRadius] ?: 16,
            (it[Keys.widgetPaddingX] ?: 10).coerceIn(0, 32), (it[Keys.widgetPaddingY] ?: 8).coerceIn(0, 28),
            com.xvox.music.widget.WidgetCustomization.decode(it[Keys.widgetCustomization].orEmpty()))
    }.distinctUntilChanged()
    val widgetTransparency: Flow<Float> = context.xvoxDataStore.data.map { it[Keys.widgetTransparency] ?: 0.25f }.distinctUntilChanged()
    val widgetTheme: Flow<String> = context.xvoxDataStore.data.map { it[Keys.widgetTheme] ?: "Dark" }.distinctUntilChanged()
    val widgetCustomColor: Flow<String> = context.xvoxDataStore.data.map { it[Keys.widgetCustomColor] ?: "#000000" }.distinctUntilChanged()
    val widgetShowLogo: Flow<Boolean> = context.xvoxDataStore.data.map { it[Keys.widgetShowLogo] ?: true }.distinctUntilChanged()
    val widgetCornerRadius: Flow<Int> = context.xvoxDataStore.data.map { it[Keys.widgetCornerRadius] ?: 16 }.distinctUntilChanged()

    val chromeStyle: Flow<com.xvox.music.core.ui.chrome.XvoxChromeStyle> = context.xvoxDataStore.data
        .map { com.xvox.music.core.ui.chrome.XvoxChromeStyle.decode(it[Keys.chromeStyle].orEmpty()) }
        .distinctUntilChanged()
    val backgroundBrightness: Flow<Float> = context.xvoxDataStore.data
        .map { (it[Keys.backgroundBrightness] ?: 0.8f).coerceIn(0.2f, 1f) }.distinctUntilChanged()
    val audioOutputRoute: Flow<String> = context.xvoxDataStore.data
        .map { when (it[Keys.audioOutputRoute]) { "headset", "phone" -> it[Keys.audioOutputRoute]; else -> "auto" } }
        .distinctUntilChanged()
    val profileLines: Flow<List<String>> = context.xvoxDataStore.data
        .map { it[Keys.profileLines].orEmpty().lines().map { l -> l.trim() }.filter { l -> l.isNotEmpty() }.distinct().take(4) }
        .distinctUntilChanged()
    val remindersEnabled: Flow<Boolean> = context.xvoxDataStore.data
        .map { it[Keys.remindersEnabled] ?: false }.distinctUntilChanged()

    suspend fun setLyricsSettings(settings: LyricsSettings) { context.xvoxDataStore.edit { it[Keys.lyricsSettings] = settings.sanitized().encode() } }
    suspend fun setSplitShowPill(v: Boolean) { context.xvoxDataStore.edit { it[Keys.splitShowPill] = v } }
    suspend fun setSplitHideCollection(v: Boolean) { context.xvoxDataStore.edit { it[Keys.splitHideCollection] = v } }
    suspend fun setPlaylistStyle(value: String) { context.xvoxDataStore.edit { it[Keys.playlistStyle] = if (value == "cards") "cards" else "long" } }
    suspend fun setPlaylistLongHeight(value: Int) { context.xvoxDataStore.edit { it[Keys.playlistLongHeight] = value.coerceIn(0, 260) } }
    suspend fun setHomeMerge(enabled: Boolean) { context.xvoxDataStore.edit { it[Keys.homeMerge] = enabled } }
    suspend fun setHomeSectionOrder(order: List<String>) {
        context.xvoxDataStore.edit {
            val normalized = HomeSections.normalize(order)
            it[Keys.homeSectionOrder] = normalized.joinToString(",")
            it[Keys.recentsPlacement] = if (normalized.indexOf(HomeSections.RECENT) < normalized.indexOf(HomeSections.ALL)) "top" else "bottom"
        }
    }
    suspend fun setHomeSectionVisible(id: String, visible: Boolean) {
        if (id !in HomeSections.defaultOrder) return
        context.xvoxDataStore.edit {
            val hidden = it[Keys.homeHiddenSections].orEmpty().split(",").filter { key -> key in HomeSections.defaultOrder }.toSet()
            it[Keys.homeHiddenSections] = (if (visible) hidden - id else hidden + id).joinToString(",")
            if (id == HomeSections.RECENT) it[Keys.hideRecentlyPlayed] = !visible
        }
    }
    suspend fun setCrossfadeSmart(v: Boolean) { context.xvoxDataStore.edit { it[Keys.crossfadeSmart] = v } }
    suspend fun setCrossfadeClashControl(v: Float) { context.xvoxDataStore.edit { it[Keys.crossfadeClashControl] = v.coerceIn(0f, 1f) } }
    suspend fun setCrossfadeBeatSync(enabled: Boolean) { context.xvoxDataStore.edit { it[Keys.crossfadeBeatSync] = enabled } }
    suspend fun setWidgetCustomization(value: com.xvox.music.widget.WidgetCustomization) { context.xvoxDataStore.edit { it[Keys.widgetCustomization] = value.sanitized().encode() } }
    suspend fun setWidgetPaddingX(value: Int) { context.xvoxDataStore.edit { it[Keys.widgetPaddingX] = value.coerceIn(0, 32) } }
    suspend fun setWidgetPaddingY(value: Int) { context.xvoxDataStore.edit { it[Keys.widgetPaddingY] = value.coerceIn(0, 28) } }
    suspend fun setCrossfade(v: Boolean) { context.xvoxDataStore.edit { it[Keys.crossfade] = v } }
    suspend fun setCrossfadeDuration(v: Int) { context.xvoxDataStore.edit { it[Keys.crossfadeDuration] = v.coerceIn(1, 12) } }
    suspend fun setPauseOnHeadphoneDisconnect(v: Boolean) { context.xvoxDataStore.edit { it[Keys.pauseOnHeadphoneDisconnect] = v } }
    suspend fun setPlayOnHeadsetConnect(v: Boolean) { context.xvoxDataStore.edit { it[Keys.playOnHeadsetConnect] = v } }
    suspend fun setBtDisconnectAction(v: String) { context.xvoxDataStore.edit { it[Keys.btDisconnectAction] = v } }
    suspend fun setBtConnectAction(v: String) { context.xvoxDataStore.edit { it[Keys.btConnectAction] = v } }

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
    suspend fun setThemeBackground(v: String) {
        context.xvoxDataStore.edit { it[Keys.themeBackground] = if (v == "Midnight" || v == "Warm") v else "Default" }
    }
    suspend fun setThemeBackgroundImage(uri: String?) {
        val previous = context.xvoxDataStore.data.map { it[Keys.themeBackgroundImage] }.first()
        val persisted = if (uri.isNullOrBlank()) null else persistGalleryImage(uri, "background")
        context.xvoxDataStore.edit { prefs ->
            if (persisted == null) prefs.remove(Keys.themeBackgroundImage)
            else prefs[Keys.themeBackgroundImage] = persisted
        }
        if (previous != null && previous != persisted) deleteAppFile(previous)
    }
    suspend fun setCardTransparency(v: Float) {
        context.xvoxDataStore.edit { it[Keys.cardTransparency] = v.coerceIn(0f, 0.6f) }
    }
    suspend fun setFontSizeScale(v: Float) { context.xvoxDataStore.edit { it[Keys.fontSizeScale] = v } }
    suspend fun setFourRowsGrid(v: Boolean) { context.xvoxDataStore.edit { it[Keys.fourRowsGrid] = v } }

    suspend fun setHomeLayoutStyle(style: String) { context.xvoxDataStore.edit { it[Keys.homeLayoutStyle] = normalizeHomeStyle(style) } }
    suspend fun setHomeScrollDirection(direction: String) { context.xvoxDataStore.edit { it[Keys.homeScrollDirection] = direction } }
    suspend fun setHomeHorizontalRows(rows: Int) { context.xvoxDataStore.edit { it[Keys.homeHorizontalRows] = rows.coerceIn(3, 8) } }
    suspend fun setRecentsPlacement(value: String) {
        context.xvoxDataStore.edit {
            val placement = if (value == "top") "top" else "bottom"
            it[Keys.recentsPlacement] = placement
            val order = it[Keys.homeSectionOrder]?.split(",") ?: HomeSections.defaultOrder
            it[Keys.homeSectionOrder] = HomeSections.placeRecent(order, placement).joinToString(",")
        }
    }
    suspend fun setEqHeadroomDb(value: Float) { context.xvoxDataStore.edit { it[Keys.eqHeadroomDb] = value.coerceIn(0f, 18f) } }
    suspend fun setSurroundDepth(value: Float) { context.xvoxDataStore.edit { it[Keys.surroundDepth] = value.coerceIn(0f, 1f) } }
    suspend fun setIgnoredFolders(folders: Set<String>) { context.xvoxDataStore.edit { it[Keys.ignoredFolders] = folders.joinToString("\n") } }
    suspend fun setAudioState(state: com.xvox.music.audio.LiveEqState) {
        context.xvoxDataStore.edit {
            it[Keys.equalizerEnabled] = state.enabled
            it[Keys.eqPreset] = state.preset
            it[Keys.eqBands] = state.bands.joinToString(",")
            it[Keys.eqBandCount] = com.xvox.music.audio.EqBands.count(state.bandCount)
            it[Keys.noiseReduction] = state.noiseReduction.coerceIn(0f, 1f)
            it[Keys.softenHighs] = state.softenHighs.coerceIn(0f, 1f)
            it[Keys.eqHeadroomDb] = state.headroomDb.coerceIn(0f, 18f)
            it[Keys.balance] = state.balance.coerceIn(-1f, 1f)
            it[Keys.stereoWidening] = state.surroundEnabled
            it[Keys.surroundDepth] = state.surroundDepth.coerceIn(0f, 1f)
            it[Keys.surroundPanSpeed] = state.orbitSeconds.coerceIn(2, 10)
            it[Keys.surroundWidth] = state.surroundWidth.coerceIn(.05f, 1f)
            it[Keys.surroundPosition] = state.surroundPosition.coerceIn(-1.5f, 1.5f)
            it[Keys.roomAmount] = state.roomAmount.coerceIn(0f, 1f)
            it[Keys.reverbAmount] = state.reverbAmount.coerceIn(0f, 1f)
            it[Keys.hrtf] = state.hrtf.coerceIn(0f, 1f)
            it[Keys.centerPreservation] = state.centerPreservation.coerceIn(0f, 1f)
            it[Keys.appVolume] = state.appVolume.coerceIn(0f, 1f)
            it[Keys.volumeLimit] = state.volumeLimit.coerceIn(0f, 1f)
        }
    }
    suspend fun setEqState(enabled: Boolean, preset: String, bands: List<Int>) {
        context.xvoxDataStore.edit {
            it[Keys.equalizerEnabled] = enabled
            it[Keys.eqPreset] = preset
            it[Keys.eqBands] = List(5) { i -> bands.getOrElse(i) { 0 }.coerceIn(-12, 12) }.joinToString(",")
        }
    }
    suspend fun setEqConfiguration(preset: String, bands: List<Int>) {
        context.xvoxDataStore.edit {
            it[Keys.eqPreset] = preset
            it[Keys.eqBands] = List(5) { i -> bands.getOrElse(i) { 0 }.coerceIn(-12, 12) }.joinToString(",")
        }
    }
    suspend fun setHideRecentlyPlayed(hide: Boolean) { setHomeSectionVisible(HomeSections.RECENT, !hide) }
    suspend fun setSortOrder(order: String) { context.xvoxDataStore.edit { it[Keys.sortOrder] = order } }

    suspend fun setIgnoreBelowSec(sec: Int) { context.xvoxDataStore.edit { it[Keys.ignoreBelowSec] = sec.coerceIn(0, 86400) } }
    suspend fun setIgnoreBelowKb(kb: Int) { context.xvoxDataStore.edit { it[Keys.ignoreBelowKb] = kb.coerceIn(0, 10485760) } }
    suspend fun toggleIgnoredFolder(folder: String) {
        context.xvoxDataStore.edit { prefs ->
            val current = prefs[Keys.ignoredFolders].orEmpty().split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            if (current.contains(folder)) {
                current.remove(folder)
            } else {
                current.add(folder)
            }
            prefs[Keys.ignoredFolders] = current.joinToString("\n")
        }
    }

    suspend fun setWidgetTransparency(v: Float) { context.xvoxDataStore.edit { it[Keys.widgetTransparency] = v } }
    suspend fun setWidgetTheme(v: String) { context.xvoxDataStore.edit { it[Keys.widgetTheme] = v } }
    suspend fun setWidgetCustomColor(v: String) { context.xvoxDataStore.edit { it[Keys.widgetCustomColor] = v } }
    suspend fun setWidgetShowLogo(v: Boolean) { context.xvoxDataStore.edit { it[Keys.widgetShowLogo] = v } }
    suspend fun setWidgetCornerRadius(v: Int) { context.xvoxDataStore.edit { it[Keys.widgetCornerRadius] = v } }

    suspend fun setChromeStyle(v: com.xvox.music.core.ui.chrome.XvoxChromeStyle) {
        context.xvoxDataStore.edit { it[Keys.chromeStyle] = v.encode() }
    }
    suspend fun setBackgroundBrightness(v: Float) {
        context.xvoxDataStore.edit { it[Keys.backgroundBrightness] = v.coerceIn(0.2f, 1f) }
    }
    suspend fun setAudioOutputRoute(v: String) {
        context.xvoxDataStore.edit { it[Keys.audioOutputRoute] = when (v) { "headset", "phone" -> v; else -> "auto" } }
    }
    suspend fun setProfileLines(lines: List<String>) {
        context.xvoxDataStore.edit { it[Keys.profileLines] = lines.map { l -> l.trim() }.filter { l -> l.isNotEmpty() }.distinct().take(4).joinToString("\n") }
    }
    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.xvoxDataStore.edit { it[Keys.remindersEnabled] = enabled }
    }
    suspend fun setReminderFired() {
        context.xvoxDataStore.edit { it[Keys.remindersLastAt] = System.currentTimeMillis() }
    }

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

    suspend fun recordRecentSong(songId: Long, source: String? = null) {
        context.xvoxDataStore.edit { prefs ->
            val current = decodeRecentIds(prefs[Keys.recentSongIds].orEmpty())
            val updated = buildList {
                add(songId)
                addAll(current.filterNot { it == songId })
            }.take(20)
            prefs[Keys.recentSongIds] = updated.joinToString(",")
            // Remember the origin alongside the id, and drop entries that fell off the list.
            val sources = decodeRecentSources(prefs[Keys.recentSongSources].orEmpty()).toMutableMap()
            if (!source.isNullOrBlank()) sources[songId] = source.take(48)
            sources.keys.retainAll(updated.toSet())
            prefs[Keys.recentSongSources] = encodeRecentSources(sources)
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
    }.distinctUntilChanged()

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

    private fun decodeRecentSources(raw: String): Map<Long, String> {
        if (raw.isBlank()) return emptyMap()
        return buildMap {
            raw.lineSequence().forEach { line ->
                val separator = line.indexOf('\t')
                if (separator <= 0 || separator >= line.lastIndex) return@forEach
                val id = line.substring(0, separator).toLongOrNull() ?: return@forEach
                put(id, line.substring(separator + 1))
            }
        }
    }

    private fun encodeRecentSources(sources: Map<Long, String>): String =
        sources.entries.joinToString("\n") { "${it.key}\t${it.value}" }

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

    /**
     * Only orphans are reclaimed.
     *
     * The old behaviour deleted every profile image except the selected one, which is exactly why
     * a second custom picture could never be kept. Files that are still listed in the gallery are
     * now preserved; the user removes them with the delete badge instead.
     */
    private suspend fun cleanupProfileImages(retainedUri: String?) {
        val kept = context.xvoxDataStore.data.map { decodeUriList(it[Keys.customPfpUris].orEmpty()) }.first()
        withContext(Dispatchers.IO) {
            val protectedPaths = (kept + listOfNotNull(retainedUri)).mapNotNull {
                runCatching { Uri.parse(it) }.getOrNull()?.takeIf { uri -> uri.scheme == "file" }?.path
            }.toSet()
            if (protectedPaths.isEmpty()) return@withContext
            File(context.filesDir, "profile").listFiles()?.forEach { file ->
                if (file.path !in protectedPaths) file.delete()
            }
        }
    }

    /** Copies a picked image into app storage and stacks it in the profile gallery. */
    suspend fun addCustomPfp(source: String): String? {
        val persisted = persistProfileImage(source) ?: return null
        context.xvoxDataStore.edit { prefs ->
            val current = decodeUriList(prefs[Keys.customPfpUris].orEmpty())
            prefs[Keys.customPfpUris] = encodeUriList(listOf(persisted) + current.filterNot { it == persisted })
        }
        return persisted
    }

    /** Removes a stacked profile picture and its file; falls back to the default if it was in use. */
    suspend fun removeCustomPfp(uri: String) {
        context.xvoxDataStore.edit { prefs ->
            val remaining = decodeUriList(prefs[Keys.customPfpUris].orEmpty()).filterNot { it == uri }
            if (remaining.isEmpty()) prefs.remove(Keys.customPfpUris) else prefs[Keys.customPfpUris] = encodeUriList(remaining)
            if (prefs[Keys.customPfpUri] == uri) {
                prefs.remove(Keys.customPfpUri)
                if (prefs[Keys.selectedPfp] == "CUSTOM") prefs[Keys.selectedPfp] = "DEFAULT"
            }
        }
        deleteAppFile(uri)
    }

    /** Copies a picked image into app storage and stacks it in the playlist-cover gallery. */
    suspend fun addCustomCover(source: String): String? {
        val persisted = persistGalleryImage(source, "cover_gallery") ?: return null
        context.xvoxDataStore.edit { prefs ->
            val current = decodeUriList(prefs[Keys.customCoverUris].orEmpty())
            prefs[Keys.customCoverUris] = encodeUriList(listOf(persisted) + current.filterNot { it == persisted })
        }
        return persisted
    }

    suspend fun removeCustomCover(uri: String) {
        context.xvoxDataStore.edit { prefs ->
            val remaining = decodeUriList(prefs[Keys.customCoverUris].orEmpty()).filterNot { it == uri }
            if (remaining.isEmpty()) prefs.remove(Keys.customCoverUris) else prefs[Keys.customCoverUris] = encodeUriList(remaining)
        }
        deleteAppFile(uri)
    }

    private suspend fun deleteAppFile(uri: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                val parsed = Uri.parse(uri)
                if (parsed.scheme == "file") parsed.path?.let { File(it).delete() }
            }
        }
    }

    private suspend fun persistGalleryImage(value: String, folder: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(value)
            val directory = File(context.filesDir, folder).apply { mkdirs() }
            val existing = if (uri.scheme == "file") uri.path?.let(::File) else null
            if (existing != null && existing.exists() &&
                runCatching { existing.parentFile?.canonicalPath == directory.canonicalPath }.getOrDefault(false)
            ) return@runCatching value
            val target = File(directory, "img_${System.nanoTime()}.img")
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (!copied) { target.delete(); null } else Uri.fromFile(target).toString()
        }.getOrNull()
    }

    private fun decodeUriList(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private fun encodeUriList(values: List<String>): String = values.distinct().take(24).joinToString("\n")

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
