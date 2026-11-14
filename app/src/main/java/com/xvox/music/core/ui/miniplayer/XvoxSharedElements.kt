// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/core/ui/miniplayer/XvoxSharedElements.kt
package com.xvox.music.core.ui.miniplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.model.Song
import com.xvox.music.features.home.XvoxSongArtwork

/**
 * Registry of on-screen element rectangles (root coordinates) so one element can be drawn
 * travelling from where it was (Mini Player card, previous layout) to where it is now.
 */
object XvoxShared {
    val rects = HashMap<String, Rect>()

    /** Keys whose real element is invisible while a ghost copy travels. */
    var hidden by mutableStateOf(emptySet<String>())

    /** True while Now Playing grows out of the Mini Player (hides both real title/artist). */
    var growHide by mutableStateOf(false)
}

/** Reports [key]'s bounds and hides the real element while its ghost is travelling. */
fun Modifier.xvoxShared(key: String): Modifier = this
    .onGloballyPositioned {
        val r = it.boundsInRoot()
        XvoxShared.rects[key] = r
        if (key == "cover") XvoxMiniBounds.coverRect = r
    }
    .graphicsLayer {
        alpha = if (key in XvoxShared.hidden || (XvoxShared.growHide && key != "cover")) 0f else 1f
    }

/** Mini Player side: reports bounds; hidden while its text has grown into the player. */
fun Modifier.xvoxSharedMini(key: String): Modifier = this
    .onGloballyPositioned { XvoxShared.rects[key] = it.boundsInRoot() }
    .graphicsLayer { alpha = if (XvoxShared.growHide) 0f else 1f }

class XvoxGhost(
    val fromKey: String,
    val toKey: String,
    /** null = artwork ghost. */
    val text: String?,
    val color: Color,
    val fontSizeSp: Float,
    val weight: FontWeight,
    val song: Song? = null,
    val radiusFromDp: Float = 0f,
    val radiusToDp: Float = 0f
)

/** Draws the ghosts at lerp(from, to, progress). [from] null = use the live registry. */
@Composable
fun XvoxSharedGhosts(
    ghosts: List<XvoxGhost>,
    from: Map<String, Rect>?,
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Box(modifier.fillMaxSize()) {
        for (g in ghosts) {
            val fr = (from ?: XvoxShared.rects)[g.fromKey] ?: continue
            val to = XvoxShared.rects[g.toKey] ?: continue
            if (g.text != null) {
                Text(
                    text = g.text,
                    color = g.color,
                    fontSize = g.fontSizeSp.sp,
                    fontWeight = g.weight,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .offset {
                            val f = progress().coerceIn(0f, 1f)
                            IntOffset((fr.left + (to.left - fr.left) * f).toInt(), (fr.top + (to.top - fr.top) * f).toInt())
                        }
                        .graphicsLayer {
                            val f = progress().coerceIn(0f, 1f)
                            val s = (fr.height / to.height.coerceAtLeast(1f)).let { it + (1f - it) * f }
                            scaleX = s; scaleY = s
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                )
            } else if (g.song != null) {
                // The rect heights are used for both axes only through width/height lerp below.
                val f = progress().coerceIn(0f, 1f)
                val w = fr.width + (to.width - fr.width) * f
                val h = fr.height + (to.height - fr.height) * f
                val r = g.radiusFromDp + (g.radiusToDp - g.radiusFromDp) * f
                XvoxSongArtwork(
                    artwork = g.song.artworkUri,
                    requestSize = 256,
                    modifier = Modifier
                        .offset { IntOffset((fr.left + (to.left - fr.left) * f).toInt(), (fr.top + (to.top - fr.top) * f).toInt()) }
                        .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                        .clip(RoundedCornerShape(r.coerceAtLeast(0f).dp))
                )
            }
        }
    }
}
