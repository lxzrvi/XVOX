package com.xvox.music.artwork

import android.graphics.Bitmap
import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap
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
    // Album-art URIs can exist in MediaStore while serving no image. Remember those failures by
    // source identity so later Now Playing / card compositions immediately select the bundled
    // fallback rather than reopening the same dead URI and flashing an empty frame.
    private val failedSources = ConcurrentHashMap.newKeySet<String>()

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
        failedSources.clear()
        _revision.value += 1L
    }

    fun markFailed(source: Any?) {
        if (source == null) return
        if (failedSources.add(keyFor(source))) _revision.value += 1L
    }

    fun markSuccessful(source: Any?) {
        if (source == null) return
        if (failedSources.remove(keyFor(source))) _revision.value += 1L
    }

    fun isKnownFailed(source: Any?): Boolean = source != null && keyFor(source) in failedSources

    fun keyFor(uri: Any?): String {
        return uri?.toString() ?: "null"
    }
}
