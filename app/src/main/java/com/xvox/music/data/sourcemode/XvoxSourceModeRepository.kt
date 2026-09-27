package com.xvox.music.data.sourcemode

import android.content.Context
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.sourcemode.XvoxSourceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Isolated persistence boundary for Experimental source selection. It intentionally has no media
 * extraction or provider-specific implementation: an Online mode can only be completed by a
 * compliant [com.xvox.music.features.sourcemode.XvoxOfficialProviderGateway] integration.
 */
class XvoxSourceModeRepository(context: Context) {
    private val preferences = UserPreferencesRepository(context.applicationContext)

    val mode: Flow<XvoxSourceMode> = preferences.sourceMode.map(XvoxSourceMode::fromStorage)

    suspend fun select(mode: XvoxSourceMode) {
        preferences.setSourceMode(mode.storageValue)
    }
}
