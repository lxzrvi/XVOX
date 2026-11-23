package com.xvox.music.core.ui.effects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chrisbanes.haze.HazeState

/**
 * The Haze source of the live Stage page - the same blur engine BitChord's held menu uses. The
 * lifted long-press menu blurs this through HazeMaterials.ultraThin, so the real, moving page is
 * pushed back. Null while the Stage is not recording the page.
 */
object XvoxPageBackdrop {
    var haze: HazeState? by mutableStateOf(null)
}
