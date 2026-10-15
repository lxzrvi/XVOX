package com.xvox.music.artwork

import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object XvoxArtworkCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 4).coerceIn(32 * 1024, 128 * 1024) // 25% of heap

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }
    // LruCache is deliberately non-observable. Full-player pager pages subscribe only while an
    // original cover is loading, letting an adjacent prewarm become visible in the very next
    // frame instead of first falling back to a placeholder cover.
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    fun put(key: String, bitmap: Bitmap) {
        if (get(key) == null) {
            cache.put(key, bitmap)
            _revision.value += 1L
        }
    }

    fun get(key: String): Bitmap? {
        return cache.get(key)
    }

    fun contains(key: String): Boolean {
        return cache.get(key) != null
    }

    fun clear() {
        cache.evictAll()
        _revision.value += 1L
    }

    fun keyFor(uri: Any?): String {
        return uri?.toString() ?: "null"
    }
}
