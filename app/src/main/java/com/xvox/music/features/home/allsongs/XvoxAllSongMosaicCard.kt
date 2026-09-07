package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.features.home.XvoxRecentArtworkSize
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.home.rememberSongCardColor

/** 24 visual treatments, combined with procedurally generated tile proportions. */
@Composable
fun XvoxAllSongMosaicCard(
    song: Song, widthUnits: Float, heightUnits: Float,
    onClick: () -> Unit, onLongClick: () -> Unit,
    modifier: Modifier = Modifier, current: Boolean = false, playing: Boolean = false,
    selected: Boolean = false, styleIndex: Int = 0
) {
    val colors = XvoxTheme.colors
    val cardColor = rememberSongCardColor(song, current, selected)
    val shape = when ((styleIndex / 4) % 6) {
        0 -> RoundedCornerShape(6.dp)
        1 -> RoundedCornerShape(12.dp)
        2 -> RoundedCornerShape(22.dp)
        3 -> RoundedCornerShape(topStart = 26.dp, topEnd = 8.dp, bottomEnd = 26.dp, bottomStart = 8.dp)
        4 -> RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomEnd = 8.dp, bottomStart = 24.dp)
        else -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 6.dp, bottomStart = 6.dp)
    }
    val presentation = styleIndex % 4
    val overlay = presentation == 1

    @Composable
    fun Labels(onArt: Boolean = false, modifier: Modifier = Modifier) {
        Column(modifier) {
            Text(song.title, color = if (onArt) Color.White else colors.primaryText,
                fontSize = if (widthUnits >= 2) 12.sp else 10.sp, lineHeight = 14.sp,
                fontWeight = FontWeight.Bold, maxLines = if (heightUnits >= 2) 2 else 1, overflow = TextOverflow.Ellipsis)
            Text(if (current && playing) "${song.artist} · Playing" else song.artist,
                color = if (onArt) Color.White.copy(alpha = .8f) else colors.secondaryText,
                fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    @Composable
    fun Cover(modifier: Modifier = Modifier, disc: Boolean = false) {
        Box(modifier, contentAlignment = Alignment.Center) {
            XvoxSongArtwork(song.artworkUri, requestSize = XvoxRecentArtworkSize,
                modifier = Modifier.fillMaxSize().clip(if (disc) CircleShape else RoundedCornerShape(8.dp)))
            if (disc) Box(Modifier.size(14.dp).background(cardColor, CircleShape).border(2.dp, colors.cardBorder, CircleShape))
        }
    }

    Box(modifier.xvoxSongPress(onClick, onLongClick).clip(shape).background(cardColor)
        .border(if (selected) 2.dp else 0.7.dp, if (selected) colors.primaryAccent else colors.cardBorder, shape)) {
        when {
            overlay -> {
                Cover(Modifier.fillMaxSize().padding(4.dp))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .78f)))))
                Labels(onArt = true, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                if (current) Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter).background(cardColor))
            }
            presentation == 3 && widthUnits >= heightUnits * 1.5f -> Row(
                Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Cover(Modifier.fillMaxHeight().aspectRatio(1f), disc = true)
                Labels(modifier = Modifier.weight(1f).padding(end = 16.dp))
            }
            else -> Column(Modifier.fillMaxSize().padding(if (presentation == 2) 9.dp else 5.dp)) {
                if (presentation == 2) Labels(modifier = Modifier.padding(bottom = 6.dp, end = 18.dp))
                Cover(Modifier.weight(1f).fillMaxWidth(), disc = presentation == 3)
                if (presentation != 2) Labels(modifier = Modifier.padding(top = 5.dp))
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(30.dp)
            .clip(CircleShape).background(if (selected) colors.primaryAccent else colors.surface.copy(alpha = .88f))
            .clickable(onClick = if (selected) onClick else onLongClick), contentAlignment = Alignment.Center) {
            Icon(painterResource(if (selected) R.drawable.ic_xvox_check else R.drawable.ic_xvox_more),
                if (selected) "Selected" else "Song options", tint = if (selected) colors.background else colors.primaryText,
                modifier = Modifier.size(16.dp))
        }
    }
}
