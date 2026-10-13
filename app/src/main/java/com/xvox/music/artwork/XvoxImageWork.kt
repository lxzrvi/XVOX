package com.xvox.music.artwork

import kotlinx.coroutines.Dispatchers

/** Shared bounds stop a grid/style switch spawning dozens of competing artwork decoders. */
object XvoxImageWork {
    val decoderContext = Dispatchers.IO.limitedParallelism(2)
    val fetchContext = Dispatchers.IO.limitedParallelism(3)
    /** Seeded fallback-wave raster jobs are CPU work; cap them so a dense coverless grid cannot
     * contend with Compose's frame work or real-cover decoding during a fast All Songs fling. */
    val waveContext = Dispatchers.Default.limitedParallelism(2)
}
