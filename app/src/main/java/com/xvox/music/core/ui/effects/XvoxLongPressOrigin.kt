package com.xvox.music.core.ui.effects

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap

/**
 * One held card: where it sat on screen and what it looked like. The picture is taken from the
 * card's own drawing, so the menu it lifts into shows exactly the thing that was held. It
 * arrives a frame after the hold, which is why it is state. (Ported from BitChord.)
 */
@Stable
class XvoxHeldItem internal constructor(val bounds: Rect) {
    var image: ImageBitmap? by mutableStateOf(null)
        internal set
}

/**
 * The last hold, for a menu that wants to lift the held item out of its list rather than slide
 * a sheet up. Only a *hold* records anything: the three-dot button opens the same menu without
 * recording, so [consume] returns null for it and the menu hangs in the centre instead.
 */
object XvoxLongPressOrigin {
    private var held: XvoxHeldItem? = null
    private var recordedAt = 0L

    /** True while a held card's picture is being taken, so its own ⋮ can step out of the shot. */
    internal var capturing: Boolean by mutableStateOf(false)

    /** Bounds of the card currently lifted into a menu; its original steps aside meanwhile. */
    var lifted: Rect? by mutableStateOf(null)

    internal fun record(item: XvoxHeldItem) {
        held = item
        recordedAt = System.nanoTime()
    }

    fun consume(): XvoxHeldItem? {
        val item = held?.takeIf { System.nanoTime() - recordedAt < FRESH_NANOS }
        held = null
        return item
    }

    private const val FRESH_NANOS = 700_000_000L
}
