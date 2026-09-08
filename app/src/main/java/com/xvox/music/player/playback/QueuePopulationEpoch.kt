package com.xvox.music.player.playback

import java.util.concurrent.atomic.AtomicLong

/** A new explicit source selection invalidates an old background/widget queue expansion. */
object QueuePopulationEpoch {
    private val value = AtomicLong()
    fun snapshot(): Long = value.get()
    fun begin(): Long = value.incrementAndGet()
    fun current(token: Long): Boolean = value.get() == token
    fun invalidate() { value.incrementAndGet() }
}
