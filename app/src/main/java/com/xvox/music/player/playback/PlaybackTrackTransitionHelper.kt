package com.xvox.music.player.playback

import androidx.media3.session.MediaController
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackTrackTransitionHelper(
    private val scope: CoroutineScope,
    private val prefs: UserPreferencesRepository,
    private val getController: () -> MediaController?,
    private val getQueue: () -> List<Song>,
    private val getRepeatMode: () -> RepeatMode,
    private val playQueueIndex: (Int, Boolean) -> Unit,
    private val onExpectedPlaying: (Boolean, Long) -> Unit,
    private val stop: () -> Unit
) {
    private var fadeJob: Job? = null
    var crossfadeTriggeredForSongId: Long? = null

    fun reset() {
        fadeJob?.cancel()
        crossfadeTriggeredForSongId = null
    }

    fun handleTrackEnded(currentIndex: Int) {
        val queue = getQueue()
        val repeatMode = getRepeatMode()
        when (repeatMode) {
            RepeatMode.ONE -> {
                getController()?.let {
                    it.seekTo(0)
                    it.play()
                    onExpectedPlaying(true, 1500L)
                }
            }
            RepeatMode.ALL -> {
                if (queue.isNotEmpty()) {
                    val nextIdx = if (currentIndex < 0) 0 else (currentIndex + 1) % queue.size
                    playQueueIndex(nextIdx, true)
                }
            }
            RepeatMode.OFF -> {
                if (queue.isNotEmpty() && currentIndex >= 0 && currentIndex < queue.lastIndex) {
                    playQueueIndex(currentIndex + 1, true)
                } else {
                    scope.launch {
                        val shouldClear = prefs.clearQueueAfterPlayback.first()
                        if (shouldClear) {
                            stop()
                        } else {
                            onExpectedPlaying(false, 800L)
                        }
                    }
                }
            }
        }
    }

    fun checkCrossfadeAndAdvance(currentSongId: Long?, currentIndex: Int) {
        val mediaController = getController() ?: return
        if (!mediaController.isPlaying) return

        val duration = mediaController.duration
        val position = mediaController.currentPosition
        if (duration <= 0L || position <= 0L) return

        val remainingMs = duration - position
        val currentId = currentSongId ?: return

        scope.launch {
            val isCrossfade = prefs.crossfade.first()
            val crossfadeDurationSec = prefs.crossfadeDuration.first()
            val crossfadeThreshold = (crossfadeDurationSec * 1000L).coerceIn(1500L, 12000L)

            if (isCrossfade && remainingMs <= crossfadeThreshold && crossfadeTriggeredForSongId != currentId) {
                crossfadeTriggeredForSongId = currentId
                val queue = getQueue()
                val repeatMode = getRepeatMode()
                val hasNext = when (repeatMode) {
                    RepeatMode.ALL, RepeatMode.ONE -> queue.isNotEmpty()
                    RepeatMode.OFF -> currentIndex >= 0 && currentIndex < queue.lastIndex
                }
                if (hasNext) {
                    val nextIdx = when (repeatMode) {
                        RepeatMode.ALL -> (currentIndex + 1) % queue.size
                        RepeatMode.ONE -> currentIndex
                        RepeatMode.OFF -> currentIndex + 1
                    }
                    performCrossfadeTransition(nextIdx, crossfadeThreshold)
                }
            }
        }
    }

    private fun performCrossfadeTransition(targetIndex: Int, durationMs: Long) {
        val mediaController = getController() ?: return
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 12
            val stepDelay = durationMs / (steps * 2)
            val masterVol = (prefs.appVolume.first() * prefs.volumeLimit.first()).coerceIn(0.1f, 1f)

            for (i in (steps - 1) downTo 2) {
                val fraction = i.toFloat() / steps.toFloat()
                mediaController.volume = (masterVol * fraction).coerceIn(0f, 1f)
                delay(stepDelay)
            }

            playQueueIndex(targetIndex, true)

            for (i in 2..steps) {
                val fraction = i.toFloat() / steps.toFloat()
                mediaController.volume = (masterVol * fraction).coerceIn(0f, 1f)
                delay(stepDelay)
            }
            mediaController.volume = masterVol
        }
    }
}
