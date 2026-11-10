// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/artwork/XvoxImageWork.kt
package com.xvox.music.artwork

import kotlinx.coroutines.Dispatchers

/** Shared bounds stop a grid/style switch spawning dozens of competing artwork decoders. */
object XvoxImageWork {
    // Covers appear as fast as the device allows while a fling is still in progress: more parallel
    // decoders on big-core phones, but never so many that they starve the render thread.
    private val cores = Runtime.getRuntime().availableProcessors()
    val decoderContext = Dispatchers.IO.limitedParallelism((cores / 2).coerceIn(3, 4))
    val fetchContext = Dispatchers.IO.limitedParallelism((cores / 2).coerceIn(3, 5))
}
