// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/player/playback/PlayerQueueReorderHelper.kt
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

    /**
     * Orders [base] by [method], starting at the audible song. Patterns walk the queue circularly
     * from the current song, so "every other" really means every other song AFTER this one.
     */
    fun arrangeQueue(method: String, base: List<Song>, activeIndex: Int): List<Song>? {
        val n = base.size
        if (n < 3 || activeIndex !in base.indices) return null
        val r = List(n) { base[(activeIndex + it) % n] }
        val order: List<Int> = when (method) {
            "EVERY_OTHER" -> (0 until n step 2) + (1 until n step 2)
            "PAIRS" -> (0 until n).filter { (it / 2) % 2 == 0 } + (0 until n).filter { (it / 2) % 2 == 1 }
            "GROWING_JUMP" -> {
                // a, b, d, g, k ... (gaps 1,2,3,4...), then everything skipped, in order.
                val picked = ArrayList<Int>()
                var i = 0
                var gap = 1
                picked.add(0)
                while (true) {
                    i += gap
                    gap++
                    if (i >= n) break
                    picked.add(i)
                }
                picked + (0 until n).filter { it !in picked }
            }
            "REVERSE" -> listOf(0) + (n - 1 downTo 1).toList()
            else -> listOf(0) + (1 until n).shuffled()
        }
        return order.map { r[it] }
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
