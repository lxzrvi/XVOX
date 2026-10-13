package com.xvox.music.artwork

import kotlinx.coroutines.Dispatchers

/** Shared bounds stop a grid/style switch spawning dozens of competing artwork decoders. */
object XvoxImageWork {
    val decoderContext = Dispatchers.IO.limitedParallelism(2)
    val fetchContext = Dispatchers.IO.limitedParallelism(3)
}
