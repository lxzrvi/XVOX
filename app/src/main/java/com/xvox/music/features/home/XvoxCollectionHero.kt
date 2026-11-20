// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/XvoxCollectionHero.kt
package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.xvox.music.core.ui.icons.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.xvoxGlassRim
import com.xvox.music.core.ui.effects.xvoxPressScale

/**
 * Playlist / album / artist page header: the cover fills the top of the screen (under the status bar),
 * its lower part melts into a blurred copy, and title + Shuffle / Play / Add sit centred on it.
 */
@Composable
fun XvoxCollectionHero(
    title: String,
    subtitle: String,
    cover: Any?,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAdd: (() -> Unit)? = null,
    onWashChange: (Color?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    // The cover's own bottom-edge colour (BitChord's ArtworkPalette.wash). The whole page is
    // painted in it behind the list (see HomeScreen), so the cover dissolves into the page.
    val wash = rememberXvoxArtworkWash(cover) ?: colors.background
    val onWash = androidx.compose.runtime.rememberUpdatedState(onWashChange)
    androidx.compose.runtime.SideEffect { onWash.value(wash) }
    Box(modifier.fillMaxWidth().height(400.dp).clipToBounds()) {
        XvoxSongArtwork(
            artwork = cover,
            requestSize = 768,
            modifier = Modifier.fillMaxSize()
        )
        // The same cover, blurred and faded in over the lower half, so the picture loses its
        // detail before it loses its colour: a merge rather than a fade to a flat tint.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0.55f to Color.Transparent, 0.85f to Color.Black),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            XvoxSongArtwork(
                artwork = cover,
                requestSize = 96,
                modifier = Modifier.fillMaxSize().blur(48.dp, BlurredEdgeTreatment.Rectangle)
            )
        }
        // Ends on the wash at full strength, eased through several stops so no slope change
        // shows as a line.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.45f to Color.Transparent,
                    0.65f to wash.copy(alpha = 0.30f),
                    0.82f to wash.copy(alpha = 0.72f),
                    0.94f to wash.copy(alpha = 0.95f),
                    1.00f to wash
                )
            )
        )
        // Status-bar legibility over bright covers.
        Box(
            Modifier.fillMaxWidth().height(110.dp).background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = .28f), 1f to Color.Transparent)
            )
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
            Text(subtitle, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                HeroButton(R.drawable.ic_xvox_shuffle, "Shuffle", 46.dp, glass = true, onClick = onShuffle)
                HeroButton(R.drawable.ic_xvox_play, "Play", 62.dp, glass = false, onClick = onPlay)
                if (onAdd != null) HeroButton(R.drawable.ic_xvox_add, "Add songs", 46.dp, glass = true, onClick = onAdd)
                else HeroButton(R.drawable.ic_xvox_more, "More", 46.dp, glass = true, onClick = {})
            }
        }
    }
}

@Composable
private fun HeroButton(icon: Int, label: String, size: androidx.compose.ui.unit.Dp, glass: Boolean, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    Box(
        Modifier
            .size(size)
            .xvoxPressScale(pressedScale = .9f, onClick = onClick)
            .clip(CircleShape)
            .background(if (glass) colors.cardElevated.copy(alpha = .62f) else colors.primaryAccent, CircleShape)
            .xvoxGlassRim(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(icon), label,
            tint = if (glass) colors.primaryText else colors.background,
            modifier = Modifier.size(if (glass) 21.dp else 28.dp)
        )
    }
}
