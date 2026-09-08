package com.xvox.music.data.preferences

import kotlinx.coroutines.*

/** Small UI-only preferences persist after a drag, even if the settings screen is dismissed. */
object PreferenceWriteQueue {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()
    @Synchronized
    fun submit(key: String, write: suspend () -> Unit) {
        jobs.remove(key)?.cancel()
        jobs[key] = scope.launch {
            delay(120)
            try { write() } catch (cancelled: CancellationException) { throw cancelled } catch (_: java.io.IOException) { /* Preserve the in-memory UI; a subsequent edit can retry. */ }
        }
    }
}
