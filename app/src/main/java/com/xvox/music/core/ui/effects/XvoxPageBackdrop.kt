package com.xvox.music.core.ui.effects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.neoruaa.glasmio.glass.render.Backdrop

/**
 * The recorded page layer of the live Stage. Overlays that sit above the whole app (the lifted
 * long-press menu) blur this layer with Glasmio's backdrop pipeline, so they push the real, moving
 * page back instead of a still copy. Null when the Stage is not recording the page.
 */
object XvoxPageBackdrop {
    var backdrop: Backdrop? by mutableStateOf(null)
}
