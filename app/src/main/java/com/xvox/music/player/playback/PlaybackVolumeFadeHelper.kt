package com.xvox.music.player.playback

import androidx.media3.session.MediaController
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

object PlaybackVolumeFadeHelper {

    suspend fun applyFadeIn(
        mediaController: MediaController,
        prefs: UserPreferencesRepository,
        steps: Int = 8
    ) {
        val masterVol = (prefs.appVolume.first() * prefs.volumeLimit.first()).coerceIn(0.1f, 1f)
        val isFadeIn = prefs.fadeIn.first()
        if (isFadeIn) {
            mediaController.volume = 0f
            mediaController.play()
            for (i in 1..steps) {
                delay(60L)
                mediaController.volume = (masterVol * (i.toFloat() / steps.toFloat())).coerceIn(0f, 1f)
            }
            mediaController.volume = masterVol
        } else {
            mediaController.volume = masterVol
            mediaController.play()
        }
    }

    suspend fun applyFadeOutAndPause(
        mediaController: MediaController,
        prefs: UserPreferencesRepository,
        steps: Int = 8
    ) {
        val masterVol = (prefs.appVolume.first() * prefs.volumeLimit.first()).coerceIn(0.1f, 1f)
        val isFadeOut = prefs.fadeOut.first()
        if (isFadeOut) {
            for (i in (steps - 1) downTo 0) {
                delay(40L)
                mediaController.volume = (masterVol * (i.toFloat() / steps.toFloat())).coerceIn(0f, 1f)
            }
            mediaController.pause()
            mediaController.volume = masterVol
        } else {
            mediaController.pause()
        }
    }
}
