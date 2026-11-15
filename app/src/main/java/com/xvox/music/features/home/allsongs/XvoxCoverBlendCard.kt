// NAYI FILE BANAO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/features/home/allsongs/XvoxCoverBlendCard.kt
package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.components.XvoxSelectionTick
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.XvoxGridArtwork
import com.xvox.music.features.home.XvoxGridArtworkSize

/**
 * Song card where the cover fills the whole card. The lower half melts into a soft, darkened copy of the
 * same cover (the way Now Playing's iOS layout blends its cover into the background) and carries the
 * title / artist. No live backdrop blur is used, so long lists scroll cheaply.
 */
@Composable
fun XvoxCoverBlendCard(
    song: Song,
    shape: Shape,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    current: Boolean = false,
    playing: Boolean = false,
    selected: Boolean = false,
    dense: Boolean = false,
    requestSize: Int = XvoxGridArtworkSize,
    titleSp: Float = if (dense) 8.5f else 10.5f,
    artistSp: Float = if (dense) 7f else 8.5f,
    titleLines: Int = 1,
    pressedScale: Float = .95f
) {
    val colors = XvoxTheme.colors
    val accent = colors.primaryAccent
    val borderBrush = if (selected || current) {
        Brush.verticalGradient(listOf(accent, accent.copy(alpha = .55f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = .62f), Color.White.copy(alpha = .14f)))
    }
    Box(
        modifier
            .xvoxSongPress(onClick = onClick, onLongClick = onLongClick, pressedScale = pressedScale, hapticOnTap = false)
            .clip(shape)
            .border(if (selected || current) 1.6.dp else .9.dp, borderBrush, shape)
    ) {
        XvoxGridArtwork(
            artwork = song.artworkUri,
            fallbackSeed = song.id.hashCode(),
            requestSize = requestSize,
            modifier = Modifier.fillMaxSize()
        )
        // Soft lower half: a tiny decode of the same cover stretched over the card, darkened, and
        // faded in from the middle so the sharp cover melts into it.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            .42f to Color.Transparent,
                            .78f to Color.Black,
                            1f to Color.Black
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            XvoxGridArtwork(
                artwork = song.artworkUri,
                fallbackSeed = song.id.hashCode(),
                requestSize = 24,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .46f)))
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = if (dense) 5.dp else 8.dp, end = if (dense) 5.dp else 8.dp, bottom = if (dense) 4.dp else 6.dp)
        ) {
            Text(
                song.title,
                color = if (current) accent else Color.White,
                fontSize = titleSp.sp,
                lineHeight = (titleSp + 1.5f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = titleLines,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (current && playing) "${song.artist} · Playing" else song.artist,
                color = Color.White.copy(alpha = .78f),
                fontSize = artistSp.sp,
                lineHeight = (artistSp + 1.5f).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        XvoxSelectionTick(selected, shape)
    }
}
