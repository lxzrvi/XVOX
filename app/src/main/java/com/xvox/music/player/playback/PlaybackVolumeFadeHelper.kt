package com.xvox.music.player.playback

import androidx.media3.session.MediaController
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

object PlaybackVolumeFadeHelper {

    suspend fun applyFadeIn(
        mediaController: MediaController,
        prefs: UserPreferencesRepository,
        steps: Int = 8
    ) {
        val masterVol = (prefs.appVolume.first() * prefs.volumeLimit.first()).coerceIn(0.1f, 1f)
        mediaController.volume = masterVol
        mediaController.play()
    }

    suspend fun applyFadeOutAndPause(
        mediaController: MediaController,
        prefs: UserPreferencesRepository,
        steps: Int = 8
    ) {
        mediaController.pause()
    }
}
