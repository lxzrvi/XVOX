package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.XvoxRecentArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor

private data class CardCorners(val tl: Dp, val tr: Dp, val br: Dp, val bl: Dp) {
    fun inset(by: Dp) = RoundedCornerShape(
        topStart = (tl - by).coerceAtLeast(0.dp), topEnd = (tr - by).coerceAtLeast(0.dp),
        bottomEnd = (br - by).coerceAtLeast(0.dp), bottomStart = (bl - by).coerceAtLeast(0.dp))
}

/** Mosaic 2 keeps varied cards, but never turns a cover into a disc or punches a hole in it. */
@Composable
fun XvoxAllSongMosaicCard(
    song: Song, widthUnits: Float, heightUnits: Float,
    onClick: () -> Unit, onLongClick: () -> Unit,
    modifier: Modifier = Modifier, current: Boolean = false, playing: Boolean = false,
    selected: Boolean = false, styleIndex: Int = 0, classic: Boolean = false
) {
    val colors = XvoxTheme.colors
    val background = rememberSongCardColor(song, current, selected)
    val corners = if (classic) CardCorners(12.dp, 12.dp, 12.dp, 12.dp) else when ((styleIndex / 4) % 6) {
        0 -> CardCorners(8.dp, 8.dp, 8.dp, 8.dp)
        1 -> CardCorners(14.dp, 14.dp, 14.dp, 14.dp)
        2 -> CardCorners(22.dp, 22.dp, 22.dp, 22.dp)
        3 -> CardCorners(24.dp, 8.dp, 24.dp, 8.dp)
        4 -> CardCorners(8.dp, 24.dp, 8.dp, 24.dp)
        else -> CardCorners(22.dp, 22.dp, 8.dp, 8.dp)
    }
    val shape = corners.inset(0.dp)
    val inset = if (classic) 6.dp else 5.dp
    val artShape = corners.inset(inset)
    val presentation = if (classic) 0 else styleIndex % 4

    @Composable
    fun Labels(onArt: Boolean = false, modifier: Modifier = Modifier) {
        Column(modifier) {
            Text(song.title,
                // Currently playing song keeps the accent colour, on art or on card.
                color = when {
                    current -> colors.primaryAccent
                    onArt -> Color.White
                    else -> colors.primaryText
                },
                fontSize = if (widthUnits >= 2) 12.sp else 10.sp,
                lineHeight = if (widthUnits >= 2) 14.sp else 12.sp,
                fontWeight = FontWeight.Bold, maxLines = if (heightUnits >= 2) 2 else 1,
                overflow = TextOverflow.Ellipsis)
            Text(if (current && playing) "${song.artist} · Playing" else song.artist,
                color = if (onArt) Color.White.copy(alpha = .85f) else colors.secondaryText,
                fontSize = if (widthUnits >= 2) 9.sp else 8.sp, lineHeight = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    @Composable
    fun Cover(modifier: Modifier = Modifier) {
        XvoxSongArtwork(song.artworkUri, requestSize = if (widthUnits == 1f && heightUnits == 1f) 160 else XvoxRecentArtworkSize,
            modifier = modifier.clip(artShape))
    }

    Box(modifier.xvoxSongPress(onClick, onLongClick, pressedScale = 0.96f, hapticOnTap = false).clip(shape).background(background)
        .border(if (selected) 2.dp else .7.dp, if (selected) colors.primaryAccent else colors.cardBorder, shape)) {
        when {
            presentation == 1 -> {
                // Inner clipping also clips the title gradient to the same corners as the cover.
                Box(Modifier.fillMaxSize().padding(inset).clip(artShape)) {
                    Cover(Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = .82f)))))
                    Labels(true, Modifier.align(Alignment.BottomStart).padding(8.dp))
                }
            }
            presentation == 3 && widthUnits >= heightUnits * 1.5f -> {
                Row(Modifier.fillMaxSize().padding(inset), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cover(Modifier.fillMaxHeight().aspectRatio(1f))
                    Labels(modifier = Modifier.weight(1f).padding(end = 3.dp))
                }
            }
            else -> Column(Modifier.fillMaxSize().padding(inset)) {
                if (presentation == 2 && heightUnits >= 2) Labels(modifier = Modifier.padding(bottom = 6.dp))
                Cover(Modifier.weight(1f).fillMaxWidth())
                if (!(presentation == 2 && heightUnits >= 2)) Labels(modifier = Modifier.padding(top = 5.dp))
            }
        }
        if (selected) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(22.dp)
            .background(colors.primaryAccent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_xvox_check), "Selected", tint = colors.background, modifier = Modifier.size(13.dp))
        }
    }
}
