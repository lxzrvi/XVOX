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
        val oldIds = currentQueue.map { it.id }.toSet()
        val newIds = reordered.map { it.id }.toSet()
        if (oldIds != newIds || currentQueue.map { it.id } == reordered.map { it.id }) return

        val newCurrentIndex = reordered.indexOfFirst { it.id == currentSongId }
        onQueueCommitted(reordered, newCurrentIndex)
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
            val currentIds = currentQueue.map { it.id }.toSet()
            val originalIds = originalQueue.map { it.id }.toSet()
            if (currentIds == originalIds) {
                return Pair(originalQueue, 0)
            }
        }
        return null
    }
}
