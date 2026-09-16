package com.xvox.music.player.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerSleepTimerManager(
    private val scope: CoroutineScope,
    private val stateFlow: MutableStateFlow<MainPlayerUiState>,
    private val onStopPlayback: () -> Unit
) {
    private var sleepTimerJob: Job? = null
    private var sleepTimerProgressJob: Job? = null

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerProgressJob?.cancel()

        if (minutes == null || minutes <= 0) {
            stateFlow.update {
                it.copy(
                    sleepTimerMinutes = null,
                    sleepTimerProgress = null,
                    sleepTimerRemainingMillis = null,
                    sleepTimerShouldCloseApp = false
                )
            }
            return
        }

        val totalMillis = minutes * 60 * 1000L
        stateFlow.update {
            it.copy(
                sleepTimerMinutes = minutes,
                sleepTimerProgress = 1f,
                sleepTimerRemainingMillis = totalMillis,
                sleepTimerShouldCloseApp = false
            )
        }

        sleepTimerJob = scope.launch {
            delay(totalMillis)
            sleepTimerProgressJob?.cancel()
            onStopPlayback()
            stateFlow.update {
                it.copy(
                    isPlaying = false,
                    sleepTimerMinutes = null,
                    sleepTimerProgress = null,
                    sleepTimerRemainingMillis = null,
                    miniPlayerVisible = false,
                    nowPlayingVisible = false
                )
            }
        }

        sleepTimerProgressJob = scope.launch {
            val start = System.currentTimeMillis()
            while (true) {
                delay(200L)
                val elapsed = System.currentTimeMillis() - start
                val remaining = (totalMillis - elapsed).coerceAtLeast(0L)
                if (remaining <= 0L) break
                val progress = (remaining.toFloat() / totalMillis).coerceIn(0f, 1f)
                stateFlow.update {
                    it.copy(
                        sleepTimerProgress = progress,
                        sleepTimerRemainingMillis = remaining
                    )
                }
            }
        }
    }

    fun setCustomSleepTimer(
        minutes: Int,
        seconds: Int,
        pauseMusic: Boolean,
        closeApp: Boolean
    ) {
        sleepTimerJob?.cancel()
        sleepTimerProgressJob?.cancel()

        val totalMillis = (minutes * 60L + seconds) * 1000L
        if (totalMillis <= 0L) {
            stateFlow.update {
                it.copy(
                    sleepTimerMinutes = null,
                    sleepTimerProgress = null,
                    sleepTimerRemainingMillis = null,
                    sleepTimerShouldCloseApp = false
                )
            }
            return
        }

        val totalMinutes = (totalMillis / 60000L).toInt().coerceAtLeast(1)
        val doPause = if (pauseMusic && closeApp) true else pauseMusic
        val doClose = if (pauseMusic && closeApp) false else closeApp

        stateFlow.update {
            it.copy(
                sleepTimerMinutes = totalMinutes,
                sleepTimerProgress = 1f,
                sleepTimerRemainingMillis = totalMillis,
                sleepTimerShouldCloseApp = false
            )
        }

        sleepTimerJob = scope.launch {
            delay(totalMillis)
            sleepTimerProgressJob?.cancel()

            if (doPause) {
                onStopPlayback()
                stateFlow.update {
                    it.copy(
                        isPlaying = false,
                        sleepTimerMinutes = null,
                        sleepTimerProgress = null,
                        sleepTimerRemainingMillis = null,
                        miniPlayerVisible = false,
                        nowPlayingVisible = false
                    )
                }
            }

            if (doClose) {
                onStopPlayback()
                stateFlow.update {
                    it.copy(
                        isPlaying = false,
                        sleepTimerMinutes = null,
                        sleepTimerProgress = null,
                        sleepTimerRemainingMillis = null,
                        miniPlayerVisible = false,
                        nowPlayingVisible = false,
                        sleepTimerShouldCloseApp = true
                    )
                }
            }

            if (!doPause && !doClose) {
                stateFlow.update {
                    it.copy(
                        sleepTimerMinutes = null,
                        sleepTimerProgress = null,
                        sleepTimerRemainingMillis = null
                    )
                }
            }
        }

        sleepTimerProgressJob = scope.launch {
            val start = System.currentTimeMillis()
            while (true) {
                delay(200L)
                val elapsed = System.currentTimeMillis() - start
                val remaining = (totalMillis - elapsed).coerceAtLeast(0L)
                if (remaining <= 0L) break
                val progress = (remaining.toFloat() / totalMillis).coerceIn(0f, 1f)
                stateFlow.update {
                    it.copy(
                        sleepTimerProgress = progress,
                        sleepTimerRemainingMillis = remaining
                    )
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerProgressJob?.cancel()
        stateFlow.update {
            it.copy(
                sleepTimerMinutes = null,
                sleepTimerProgress = null,
                sleepTimerRemainingMillis = null,
                sleepTimerShouldCloseApp = false
            )
        }
    }

    fun release() {
        sleepTimerJob?.cancel()
        sleepTimerProgressJob?.cancel()
    }
}
