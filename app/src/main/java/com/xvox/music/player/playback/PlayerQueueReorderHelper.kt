package com.xvox.music.player.playback

import com.xvox.music.core.model.Song

object PlayerQueueReorderHelper {

    fun reorderQueue(
        currentQueue: List<Song>,
        reordered: List<Song>,
        currentSongId: Long?,
        onQueueCommitted: (List<Song>, Int) -> Unit
    ) {
        if (reordered.size != currentQueue.size) return
        if (currentQueue == reordered) return

        val newCurrentIndex = reordered.indexOfFirst { it.id == currentSongId }
        onQueueCommitted(reordered, if (newCurrentIndex >= 0) newCurrentIndex else 0)
    }

    fun shuffleQueue(
        currentSongId: Long?,
        currentQueue: List<Song>
    ): List<Song>? {
        val currentIndex = currentQueue.indexOfFirst { it.id == currentSongId }
        if (currentIndex >= 0 && currentQueue.size > 2) {
            val currentSong = currentQueue[currentIndex]
            val others = currentQueue.filterIndexed { index, _ -> index != currentIndex }.shuffled()
            return listOf(currentSong) + others
        }
        return null
    }

    fun unshuffleQueue(
        currentQueue: List<Song>,
        originalQueue: List<Song>?
    ): Pair<List<Song>, Int>? {
        if (originalQueue != null && originalQueue.size == currentQueue.size) {
            return Pair(originalQueue, 0)
        }
        return null
    }
}
