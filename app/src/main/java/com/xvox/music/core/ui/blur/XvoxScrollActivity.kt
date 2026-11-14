// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/blur/XvoxScrollActivity.kt
package com.xvox.music.core.ui.blur

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide "is the user scrolling" signal. Per-card glass that repaints a wallpaper window on
 * every position change (that was one full redraw per visible card per frame = UI-thread jank)
 * pauses while this is true and realigns once, when scrolling goes idle.
 */
object XvoxScrollActivity {
    @Volatile
    var scrolling: Boolean = false
        private set

    /** Bumped when scrolling settles; draw scopes that read it repaint exactly once. */
    var idleTick by mutableLongStateOf(0L)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val idle = Runnable {
        scrolling = false
        idleTick += 1
    }

    fun mark() {
        scrolling = true
        handler.removeCallbacks(idle)
        handler.postDelayed(idle, 140L)
    }
}
