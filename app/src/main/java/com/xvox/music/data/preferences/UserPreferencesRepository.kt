package com.xvox.music.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.xvoxDataStore by preferencesDataStore(
    name = "xvox_preferences"
)

class UserPreferencesRepository(
    private val context: Context
) {

    private object Keys {
        val setupCompleted =
            booleanPreferencesKey("setup_completed")

        val username =
            stringPreferencesKey("username")

        val selectedPfp =
            stringPreferencesKey("selected_pfp")

        val customPfpUri =
            stringPreferencesKey("custom_pfp_uri")
    }

    val preferences: Flow<UserPreferences> =
        context.xvoxDataStore.data.map { prefs ->
            UserPreferences(
                setupCompleted =
                    prefs[Keys.setupCompleted] ?: false,
                username =
                    prefs[Keys.username].orEmpty(),
                selectedPfp =
                    prefs[Keys.selectedPfp] ?: "DEFAULT",
                customPfpUri =
                    prefs[Keys.customPfpUri]
            )
        }

    suspend fun completeSetup(
        username: String,
        selectedPfp: String,
        customPfpUri: String?
    ) {
        context.xvoxDataStore.edit { prefs ->
            prefs[Keys.username] = username
            prefs[Keys.selectedPfp] = selectedPfp

            if (customPfpUri != null) {
                prefs[Keys.customPfpUri] =
                    customPfpUri
            } else {
                prefs.remove(Keys.customPfpUri)
            }

            prefs[Keys.setupCompleted] = true
        }
    }
}
