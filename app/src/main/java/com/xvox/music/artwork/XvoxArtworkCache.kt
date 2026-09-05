package com.xvox.music.artwork

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Temporary in-memory store for artwork - keeps covers fast for visible screen
 * and holds behind covers after scroll preload without realizing
 */
object XvoxArtworkCache {
    private const val MAX_SIZE = 50 * 1024 * 1024 // 50MB

    private val cache = object : LruCache<String, Bitmap>(MAX_SIZE) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
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

    fun clear() {
        cache.evictAll()
    }

    fun keyFor(uri: Any?): String {
        return uri?.toString() ?: "null"
    }
}
