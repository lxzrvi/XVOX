package com.xvox.music.features.home.allsongs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.blur.xvoxGlassRim
import com.xvox.music.core.ui.effects.xvoxSongPress
import com.xvox.music.core.ui.icons.painterResource
import com.xvox.music.features.home.XvoxSongArtwork

/** All songs as big vertical cards, three per row: cover, title below it, then the three-dot button. */
fun LazyListScope.allSongsGridItems(
    songs: List<Song>,
    currentSongId: Long?,
    selectedSongIds: Set<Long>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        item(key = "grid_empty") {
            Text(
                "No songs match your library filters",
                color = XvoxTheme.colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }
    val rows = songs.chunked(3)
    items(
        count = rows.size,
        key = { "grid_row_${rows[it].first().id}" },
        contentType = { "grid_row" }
    ) { index ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows[index].forEach { song ->
                GridSongCard(
                    song = song,
                    current = song.id == currentSongId,
                    selected = song.id in selectedSongIds,
                    onClick = { onSongClick(song) },
                    onMore = { onSongLongClick(song) },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(3 - rows[index].size) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun GridSongCard(
    song: Song,
    current: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.xvoxSongPress(onClick = onClick, onLongClick = onMore, pressedScale = .95f)) {
        XvoxSongArtwork(
            artwork = song.artworkUri,
            requestSize = 384,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(if (selected) colors.accentSoft else colors.card)
                .xvoxGlassRim(shape)
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = if (current) colors.primaryAccent else colors.primaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist.ifBlank { "Unknown artist" },
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier.size(32.dp).xvoxSongPress(onClick = onMore, pressedScale = .8f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_more),
                    contentDescription = "More",
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
