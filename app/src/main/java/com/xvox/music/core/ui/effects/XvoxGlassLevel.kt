package com.xvox.music.core.ui.effects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The glass blur level picked in Settings -> Appearance: "clear", "small", "regular" or "large".
 * One process-wide value so the mini player, the long-press plates and any other XVOX glass read
 * the same choice without threading it through every call.
 */
object XvoxGlassLevel {
    var key: String by mutableStateOf("clear")

    /** Opacity of a frosted plate: more blur level, more body. */
    val plateAlpha: Float
        get() = when (key) {
            "clear" -> .10f
            "small" -> .20f
            "regular" -> .30f
            else -> .42f
        }
}
