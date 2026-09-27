package com.xvox.music.player.playback

import com.xvox.music.core.model.Song

object PlayerQueueReorderHelper {

    fun reorderQueue(
        currentQueue: List<Song>,
        reordered: List<Song>,
        currentSongId: Long?,
        currentIndex: Int = -1,
        onQueueCommitted: (List<Song>, Int) -> Unit
    ) {
        if (reordered.size != currentQueue.size) return
        if (reordered.indices.all { index -> reordered[index] === currentQueue[index] }) return

        val occurrence = if (currentIndex in currentQueue.indices && currentQueue[currentIndex].id == currentSongId) {
            currentQueue.take(currentIndex + 1).count { it.id == currentSongId } - 1
        } else {
            0
        }
        val activeReference = currentQueue.getOrNull(currentIndex)
        val matching = reordered.indices.filter { reordered[it].id == currentSongId }
        val newCurrentIndex = reordered.indexOfFirst { it === activeReference }
            .takeIf { it >= 0 }
            ?: matching.getOrNull(occurrence)
            ?: matching.firstOrNull()
            ?: -1
        onQueueCommitted(reordered, if (newCurrentIndex >= 0) newCurrentIndex else 0)
    }

    fun shuffleQueue(
        currentSongId: Long?,
        currentQueue: List<Song>,
        currentIndexHint: Int = -1
    ): List<Song>? {
        val currentIndex = currentIndexHint.takeIf { it in currentQueue.indices && currentQueue[it].id == currentSongId }
            ?: currentQueue.indexOfFirst { it.id == currentSongId }
        if (currentIndex >= 0 && currentQueue.size > 2) {
            // Preserve the audible occurrence at its actual queue position. Moving it to zero
            // makes the cover deck remap unnecessarily and breaks the user's sense of "around
            // this song" shuffle. Every other occurrence is independently reordered around it.
            val currentSong = currentQueue[currentIndex]
            val originalOthers = currentQueue.filterIndexed { index, _ -> index != currentIndex }
            var shuffledOthers = originalOthers.shuffled()
            // A random shuffle can occasionally return the identical ordering. Guarantee that a
            // fresh shuffle visibly changes the queue whenever there is a meaningful alternative.
            if (shuffledOthers.size > 1 && shuffledOthers.indices.all { shuffledOthers[it] === originalOthers[it] }) {
                shuffledOthers = shuffledOthers.drop(1) + shuffledOthers.first()
            }
            var cursor = 0
            return currentQueue.indices.map { index ->
                if (index == currentIndex) currentSong else shuffledOthers[cursor++]
            }
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
