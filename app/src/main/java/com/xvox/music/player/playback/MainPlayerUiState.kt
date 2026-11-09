// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/player/playback/MainPlayerUiState.kt
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
    /** Mirrors PlaybackController occurrence tokens for duplicate-safe queue UI reorders. */
    val queueOccurrenceIds: List<String> = emptyList(),
    val currentSongId: Long? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val miniPlayerVisible: Boolean = false,
    val miniPlayerRiseKey: Int = 0,
    /** Precomposes the full player below the bottom edge while Mini Player completes its exit. */
    val nowPlayingPrepared: Boolean = false,
    val nowPlayingVisible: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    /** RANDOM, EVERY_OTHER, PAIRS, GROWING_JUMP, REVERSE. */
    val shuffleMethod: String = "RANDOM",
    /** Song repeats still to play (Repeat One with a count). 0 = no counted repeat; -1 = forever. */
    val repeatTimesLeft: Int = 0,
    /** A-B loop inside the current song (ms); -1 = unset. */
    val abLoopStartMs: Long = -1L,
    val abLoopEndMs: Long = -1L,
    val sleepTimerMinutes: Int? = null,
    val playerStyle: XvoxPlayerStyle = XvoxPlayerStyle.NORMAL,
    val sleepTimerProgress: Float? = null, // 0..1 while timer active, null when off
    val sleepTimerRemainingMillis: Long? = null,
    val sleepTimerShouldCloseApp: Boolean = false,
    val playingSource: String = "All Songs",
    val activeQueueId: String = "queue_1",
    val activeQueueName: String = "Queue 1",
    val savedQueues: List<XvoxSavedQueue> = emptyList(),
)
