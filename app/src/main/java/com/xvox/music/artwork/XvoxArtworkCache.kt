package com.xvox.music.artwork

import android.graphics.Bitmap
import android.util.LruCache

object XvoxArtworkCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 4).coerceIn(32 * 1024, 128 * 1024) // 25% of heap

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    fun put(key: String, bitmap: Bitmap) {
        if (get(key) == null) {
            cache.put(key, bitmap)
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
    }

    fun keyFor(uri: Any?): String {
        return uri?.toString() ?: "null"
    }
}
