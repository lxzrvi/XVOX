package com.xvox.music.player.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.player.styles.XvoxPlayerStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainPlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val controller =
        PlaybackController(application)

    private val preferences =
        UserPreferencesRepository(application)

    private val _state =
        MutableStateFlow(
            MainPlayerUiState()
        )

    val state:
        StateFlow<MainPlayerUiState> =
        _state.asStateFlow()

    private var restoreResolved = false
    private var savedSongId: Long? = null
    private var libraryQueueSignature = 0L
    private var libraryQueueSize = -1

    private var sleepTimerJob: Job? = null
    private var sleepTimerProgressJob: Job? = null
    private var originalQueueBeforeShuffle: List<Song>? = null

    init {
        viewModelScope.launch {
            savedSongId =
                preferences
                    .lastPlayedSongId
                    .first()

            restoreFromQueueIfPossible()
        }

        viewModelScope.launch {
            controller.state.collect { playback ->
                _state.update {
                    it.copy(
                        connected =
                            playback.connected,
                        currentSongId =
                            playback.currentSongId,
                        currentIndex =
                            playback.currentIndex,
                        isPlaying =
                            playback.isPlaying,
                        position =
                            playback.position,
                        duration =
                            playback.duration
                    )
                }
            }
        }
    }

    fun setQueue(
        songs: List<Song>
    ) {
        val signature =
            queueSignature(songs)

        if (
            songs.size ==
            libraryQueueSize &&
            signature ==
            libraryQueueSignature
        ) {
            return
        }

        libraryQueueSize =
            songs.size

        libraryQueueSignature =
            signature

        val current =
            _state.value

        if (
            current.playingSource !=
            "All Songs" &&
            current.queue.isNotEmpty() &&
            current.currentSongId != null
        ) {
            return
        }

        if (
            current.currentSongId ==
            null ||
            current.queue.isEmpty()
        ) {
            controller.setQueue(
                songs
            )

            _state.update {
                it.copy(
                    queue = songs
                )
            }

            restoreFromQueueIfPossible()
            return
        }

        val available =
            HashSet<Long>(
                songs.size * 4 / 3 + 1
            )

        songs.forEach {
            available.add(it.id)
        }

        val retained =
            ArrayList<Song>(
                current.queue.size
            )

        val existing =
            HashSet<Long>(
                songs.size * 4 / 3 + 1
            )

        current.queue.forEach {
                song ->

            if (
                song.id in available
            ) {
                retained.add(song)
                existing.add(song.id)
            }
        }

        val merged =
            ArrayList<Song>(
                songs.size
            )

        merged.addAll(retained)

        songs.forEach {
                song ->

            if (
                existing.add(song.id)
            ) {
                merged.add(song)
            }
        }

        controller.setQueue(
            merged
        )

        _state.update {
            it.copy(
                queue = merged
            )
        }

        restoreFromQueueIfPossible()
    }

    fun setQueueExact(
        songs: List<Song>
    ) {
        controller.setQueue(
            songs
        )

        libraryQueueSize =
            songs.size

        libraryQueueSignature =
            queueSignature(
                songs
            )

        _state.update {
            it.copy(
                queue = songs
            )
        }

        restoreFromQueueIfPossible()
    }

    /*
     * Used by queue drag/drop.
     *
     * Unlike setQueue(), this does not run
     * library/source merging logic.
     * It keeps current song selected by ID.
     */
    fun reorderQueue(
        reordered: List<Song>
    ) {
        val oldQueue =
            _state.value.queue

        if (
            reordered.size !=
            oldQueue.size
        ) {
            return
        }

        val oldIds =
            oldQueue
                .map { it.id }
                .toSet()

        val newIds =
            reordered
                .map { it.id }
                .toSet()

        if (oldIds != newIds) {
            return
        }

        if (
            oldQueue.map { it.id } ==
            reordered.map { it.id }
        ) {
            return
        }

        val currentId =
            _state.value
                .currentSongId

        controller.setQueue(
            reordered
        )

        val newCurrentIndex =
            reordered.indexOfFirst {
                it.id == currentId
            }

        libraryQueueSize =
            reordered.size

        libraryQueueSignature =
            queueSignature(
                reordered
            )

        _state.update {
            it.copy(
                queue = reordered,
                currentIndex =
                    newCurrentIndex
            )
        }
    }

    fun playFromSource(
        song: Song,
        sourceQueue: List<Song>,
        source: String
    ) {
        val queue =
            if (
                sourceQueue.isEmpty()
            ) {
                listOf(song)
            } else {
                sourceQueue
            }

        controller.setQueue(
            queue
        )

        libraryQueueSize =
            queue.size

        libraryQueueSignature =
            queueSignature(
                queue
            )

        _state.update {
            it.copy(
                queue = queue,
                playingSource = source
            )
        }

        play(
            song,
            source
        )
    }

    fun setPlayingSource(
        source: String
    ) {
        _state.update {
            it.copy(
                playingSource =
                    source
            )
        }
    }

    fun playNextInQueue(
        song: Song
    ) {
        val queue =
            controller.playNext(
                song
            )

        _state.update {
            it.copy(
                queue = queue
            )
        }
    }

    fun addToQueue(
        song: Song
    ) {
        val queue =
            controller.addToQueue(
                song
            )

        _state.update {
            it.copy(
                queue = queue
            )
        }
    }

    fun removeFromQueue(
        songId: Long
    ) {
        val wasCurrent =
            _state.value
                .currentSongId ==
                songId

        val queue =
            controller
                .removeFromQueue(
                    songId
                )

        if (wasCurrent) {
            controller.stop()
        }

        _state.update {
            it.copy(
                queue = queue,
                miniPlayerVisible =
                    if (wasCurrent) {
                        false
                    } else {
                        it.miniPlayerVisible
                    },
                nowPlayingVisible =
                    if (wasCurrent) {
                        false
                    } else {
                        it.nowPlayingVisible
                    }
            )
        }
    }

    private fun restoreFromQueueIfPossible() {
        if (restoreResolved) {
            return
        }

        val songs =
            _state.value.queue

        val id =
            savedSongId
                ?: return

        val index =
            songs.indexOfFirst {
                it.id == id
            }

        if (index < 0) {
            return
        }

        val song =
            songs[index]

        restoreResolved =
            true

        controller.restoreSong(
            song.id
        )

        _state.update {
                current ->

            current.copy(
                currentSongId =
                    song.id,
                currentIndex =
                    index,
                duration =
                    song.duration,
                position = 0L,
                isPlaying = false,
                miniPlayerVisible =
                    true,
                miniPlayerRiseKey =
                    current
                        .miniPlayerRiseKey +
                        1
            )
        }
    }

    private fun persistSong(
        songId: Long
    ) {
        savedSongId =
            songId

        restoreResolved =
            true

        viewModelScope.launch {
            preferences
                .setLastPlayedSongId(
                    songId
                )
        }
    }

    fun play(
        song: Song,
        source: String? = null
    ) {
        val needsEntrance =
            !_state.value
                .miniPlayerVisible &&
                !_state.value
                    .nowPlayingVisible

        controller.play(
            song
        )

        persistSong(
            song.id
        )

        _state.update {
                current ->

            current.copy(
                queue =
                    controller
                        .currentQueue(),
                miniPlayerVisible =
                    true,
                miniPlayerRiseKey =
                    if (
                        needsEntrance
                    ) {
                        current
                            .miniPlayerRiseKey +
                            1
                    } else {
                        current
                            .miniPlayerRiseKey
                    },
                playingSource =
                    source
                        ?: current
                            .playingSource
            )
        }
    }

    fun playQueueIndex(
        index: Int
    ) {
        val song =
            _state.value
                .queue
                .getOrNull(
                    index
                )
                ?: return

        persistSong(
            song.id
        )

        controller.playQueueIndex(
            index = index,
            keepPlayingState = true
        )
    }

    fun playPrevious() {
        val queue =
            _state.value.queue

        val index =
            _state.value.currentIndex

        if (
            queue.isEmpty() ||
            index < 0
        ) {
            return
        }

        val atFirst =
            index <= 0

        if (
            atFirst &&
            _state.value.repeatMode ==
            RepeatMode.OFF
        ) {
            return
        }

        val target =
            if (
                atFirst &&
                _state.value.repeatMode ==
                RepeatMode.ALL
            ) {
                queue.lastIndex
            } else {
                index - 1
            }

        playQueueIndex(
            target
        )
    }

    fun playNext() {
        val queue =
            _state.value.queue

        val index =
            _state.value.currentIndex

        if (
            queue.isEmpty() ||
            index < 0
        ) {
            return
        }

        val atLast =
            index >= queue.lastIndex

        if (
            atLast &&
            _state.value.repeatMode ==
            RepeatMode.OFF
        ) {
            return
        }

        val target =
            if (
                atLast &&
                _state.value.repeatMode ==
                RepeatMode.ALL
            ) {
                0
            } else {
                index + 1
            }

        playQueueIndex(
            target
        )
    }

    fun seekTo(
        positionMs: Long
    ) {
        controller.seekTo(
            positionMs
        )
    }

    fun togglePlay() {
        controller.togglePlay()
    }

    fun openNowPlaying() {
        if (
            _state.value
                .currentSongId ==
            null
        ) {
            return
        }

        _state.update {
            it.copy(
                nowPlayingVisible =
                    true,
                miniPlayerVisible =
                    false
            )
        }
    }

    fun closeNowPlaying() {
        _state.update {
                current ->

            if (
                current.currentSongId ==
                null
            ) {
                current.copy(
                    nowPlayingVisible =
                        false,
                    miniPlayerVisible =
                        false
                )
            } else {
                current.copy(
                    nowPlayingVisible =
                        false,
                    miniPlayerVisible =
                        true,
                    miniPlayerRiseKey =
                        current
                            .miniPlayerRiseKey +
                            1
                )
            }
        }
    }

    fun hideMiniPlayer() {
        _state.update {
            it.copy(
                miniPlayerVisible =
                    false
            )
        }
    }

    fun stopPlayback() {
        controller.stop()

        _state.update {
            it.copy(
                miniPlayerVisible =
                    false,
                nowPlayingVisible =
                    false
            )
        }
    }

    fun toggleShuffle() {
        val newShuffle =
            !_state.value
                .isShuffleEnabled

        _state.update {
            it.copy(
                isShuffleEnabled =
                    newShuffle
            )
        }

        if (newShuffle) {
            val currentId =
                _state.value
                    .currentSongId

            val currentQueue =
                _state.value.queue

            originalQueueBeforeShuffle =
                currentQueue.toList()

            val currentIndex =
                currentQueue.indexOfFirst {
                    it.id == currentId
                }

            if (
                currentIndex >= 0 &&
                currentQueue.size > 2
            ) {
                val currentSong =
                    currentQueue[
                        currentIndex
                    ]

                val others =
                    currentQueue
                        .filterIndexed {
                                index,
                                _ ->

                            index !=
                                currentIndex
                        }
                        .shuffled()

                val shuffled =
                    listOf(
                        currentSong
                    ) + others

                controller.setQueue(
                    shuffled
                )

                _state.update {
                    it.copy(
                        queue = shuffled,
                        currentIndex = 0
                    )
                }
            }
        } else {
            val original =
                originalQueueBeforeShuffle

            if (
                original != null &&
                original.size ==
                _state.value
                    .queue.size
            ) {
                val currentId =
                    _state.value
                        .currentSongId

                val currentIds =
                    _state.value
                        .queue
                        .map {
                            it.id
                        }
                        .toSet()

                val originalIds =
                    original
                        .map {
                            it.id
                        }
                        .toSet()

                if (
                    currentIds ==
                    originalIds
                ) {
                    controller.setQueue(
                        original
                    )

                    val newIndex =
                        original
                            .indexOfFirst {
                                it.id ==
                                    currentId
                            }
                            .coerceAtLeast(
                                0
                            )

                    _state.update {
                        it.copy(
                            queue =
                                original,
                            currentIndex =
                                newIndex
                        )
                    }
                }

                originalQueueBeforeShuffle =
                    null
            } else {
                originalQueueBeforeShuffle =
                    null
            }
        }
    }

    fun toggleRepeat() {
        val next =
            when (
                _state.value.repeatMode
            ) {
                RepeatMode.OFF ->
                    RepeatMode.ALL

                RepeatMode.ALL ->
                    RepeatMode.ONE

                RepeatMode.ONE ->
                    RepeatMode.OFF
            }

        _state.update {
            it.copy(
                repeatMode = next
            )
        }

        controller.setRepeatMode(
            next
        )
    }

    fun moveQueueItem(
        from: Int,
        to: Int
    ) {
        val queue =
            controller.moveQueueItem(
                from,
                to
            )

        val currentId =
            _state.value.currentSongId

        val currentIndex =
            queue.indexOfFirst {
                it.id == currentId
            }

        libraryQueueSize =
            queue.size

        libraryQueueSignature =
            queueSignature(queue)

        _state.update {
            it.copy(
                queue = queue,
                currentIndex = currentIndex
            )
        }
    }

    fun setSleepTimer(
        minutes: Int?
    ) {
        sleepTimerJob?.cancel()
        sleepTimerProgressJob?.cancel()

        if (
            minutes == null ||
            minutes <= 0
        ) {
            _state.update {
                it.copy(
                    sleepTimerMinutes =
                        null,
                    sleepTimerProgress =
                        null,
                    sleepTimerRemainingMillis =
                        null,
                    sleepTimerShouldCloseApp =
                        false
                )
            }

            return
        }

        val totalMillis =
            minutes *
                60 *
                1000L

        _state.update {
            it.copy(
                sleepTimerMinutes =
                    minutes,
                sleepTimerProgress =
                    1f,
                sleepTimerRemainingMillis =
                    totalMillis,
                sleepTimerShouldCloseApp =
                    false
            )
        }

        sleepTimerJob =
            viewModelScope.launch {
                delay(
                    totalMillis
                )

                sleepTimerProgressJob
                    ?.cancel()

                controller.stop()

                _state.update {
                    it.copy(
                        isPlaying = false,
                        sleepTimerMinutes =
                            null,
                        sleepTimerProgress =
                            null,
                        sleepTimerRemainingMillis =
                            null,
                        miniPlayerVisible =
                            false,
                        nowPlayingVisible =
                            false
                    )
                }
            }

        sleepTimerProgressJob =
            viewModelScope.launch {
                val start =
                    System.currentTimeMillis()

                while (true) {
                    delay(200L)

                    val elapsed =
                        System.currentTimeMillis() -
                            start

                    val remaining =
                        (
                            totalMillis -
                                elapsed
                            ).coerceAtLeast(
                            0L
                        )

                    if (
                        remaining <= 0L
                    ) {
                        break
                    }

                    val progress =
                        (
                            remaining.toFloat() /
                                totalMillis
                            ).coerceIn(
                            0f,
                            1f
                        )

                    _state.update {
                        it.copy(
                            sleepTimerProgress =
                                progress,
                            sleepTimerRemainingMillis =
                                remaining
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

        val totalMillis =
            (
                minutes * 60L +
                    seconds
                ) * 1000L

        if (
            totalMillis <= 0L
        ) {
            _state.update {
                it.copy(
                    sleepTimerMinutes =
                        null,
                    sleepTimerProgress =
                        null,
                    sleepTimerRemainingMillis =
                        null,
                    sleepTimerShouldCloseApp =
                        false
                )
            }

            return
        }

        val totalMinutes =
            (
                totalMillis /
                    60000L
                )
                .toInt()
                .coerceAtLeast(
                    1
                )

        val doPause =
            if (
                pauseMusic &&
                closeApp
            ) {
                true
            } else {
                pauseMusic
            }

        val doClose =
            if (
                pauseMusic &&
                closeApp
            ) {
                false
            } else {
                closeApp
            }

        _state.update {
            it.copy(
                sleepTimerMinutes =
                    totalMinutes,
                sleepTimerProgress =
                    1f,
                sleepTimerRemainingMillis =
                    totalMillis,
                sleepTimerShouldCloseApp =
                    false
            )
        }

        sleepTimerJob =
            viewModelScope.launch {
                delay(
                    totalMillis
                )

                sleepTimerProgressJob
                    ?.cancel()

                if (doPause) {
                    controller.stop()

                    _state.update {
                        it.copy(
                            isPlaying =
                                false,
                            sleepTimerMinutes =
                                null,
                            sleepTimerProgress =
                                null,
                            sleepTimerRemainingMillis =
                                null,
                            miniPlayerVisible =
                                false,
                            nowPlayingVisible =
                                false
                        )
                    }
                }

                if (doClose) {
                    controller.stop()

                    _state.update {
                        it.copy(
                            isPlaying =
                                false,
                            sleepTimerMinutes =
                                null,
                            sleepTimerProgress =
                                null,
                            sleepTimerRemainingMillis =
                                null,
                            miniPlayerVisible =
                                false,
                            nowPlayingVisible =
                                false,
                            sleepTimerShouldCloseApp =
                                true
                        )
                    }
                }

                if (
                    !doPause &&
                    !doClose
                ) {
                    _state.update {
                        it.copy(
                            sleepTimerMinutes =
                                null,
                            sleepTimerProgress =
                                null,
                            sleepTimerRemainingMillis =
                                null
                        )
                    }
                }
            }

        sleepTimerProgressJob =
            viewModelScope.launch {
                val start =
                    System.currentTimeMillis()

                while (true) {
                    delay(200L)

                    val elapsed =
                        System.currentTimeMillis() -
                            start

                    val remaining =
                        (
                            totalMillis -
                                elapsed
                            ).coerceAtLeast(
                            0L
                        )

                    if (
                        remaining <= 0L
                    ) {
                        break
                    }

                    val progress =
                        (
                            remaining.toFloat() /
                                totalMillis
                            ).coerceIn(
                            0f,
                            1f
                        )

                    _state.update {
                        it.copy(
                            sleepTimerProgress =
                                progress,
                            sleepTimerRemainingMillis =
                                remaining
                        )
                    }
                }
            }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerProgressJob?.cancel()

        _state.update {
            it.copy(
                sleepTimerMinutes =
                    null,
                sleepTimerProgress =
                    null,
                sleepTimerRemainingMillis =
                    null,
                sleepTimerShouldCloseApp =
                    false
            )
        }
    }

    fun consumeCloseApp() {
        _state.update {
            it.copy(
                sleepTimerShouldCloseApp =
                    false
            )
        }
    }

    fun setPlayerStyle(
        style: XvoxPlayerStyle
    ) {
        _state.update {
            it.copy(
                playerStyle = style
            )
        }
    }

    private fun queueSignature(
        songs: List<Song>
    ): Long {
        var result =
            1125899906842597L

        songs.forEach {
            result =
                result * 31L +
                    it.id
        }

        return result
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        sleepTimerProgressJob?.cancel()
        controller.release()
        super.onCleared()
    }
}
