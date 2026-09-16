package com.xvox.music.player.playback

import com.xvox.music.core.model.Song
import com.xvox.music.features.player.styles.XvoxPlayerStyle

enum class RepeatMode { OFF, ONE, ALL }

data class XvoxSavedQueue(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val currentIndex: Int = 0,
    val source: String = "Queue"
)

data class MainPlayerUiState(
    val connected: Boolean = false,
    val queue: List<Song> = emptyList(),
    val currentSongId: Long? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val miniPlayerVisible: Boolean = false,
    val miniPlayerRiseKey: Int = 0,
    val nowPlayingVisible: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val sleepTimerMinutes: Int? = null,
    val playerStyle: XvoxPlayerStyle = XvoxPlayerStyle.NORMAL,
    val sleepTimerProgress: Float? = null, // 0..1 while timer active, null when off
    val sleepTimerRemainingMillis: Long? = null,
    val sleepTimerShouldCloseApp: Boolean = false,
    val playingSource: String = "All Songs",
    val activeQueueName: String = "Current Queue",
    val savedQueues: List<XvoxSavedQueue> = emptyList(),
)
